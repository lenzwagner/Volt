package com.lenz.tennisapp.data.scraper

import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.dao.PlayerDao
import com.lenz.tennisapp.data.db.dao.RankingDao
import com.lenz.tennisapp.data.db.entities.PlayerEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Matches players across three data sources with different name formats:
 *
 *  API-Tennis    → "A. Zverev"          (abbreviated first name, Last)
 *  TennisAbstract→ "Alexander Zverev"   (Full First Last)
 *  live-tennis.eu→ "Zverev Alexander"   (Last First — reversed!)
 *
 * Strategy:
 *  1. Normalize every name to (lastName, firstInitial)
 *  2. Match on lastName first (fast)
 *  3. If multiple candidates share a last name, use firstInitial to pick the right one
 *  4. Store matched keys back into PlayerEntity so future lookups are O(1)
 */
@Singleton
class PlayerMatcher @Inject constructor(
    private val playerDao: PlayerDao,
    private val eloDao: EloDao,
    private val rankingDao: RankingDao
) {
    companion object {
        // Particles that are never a "last name" on their own
        private val NAME_PARTICLES = setOf("van", "de", "du", "del", "di", "von", "der", "den", "dos", "da", "le", "la")
    }

    /**
     * Parse a raw display name into (lastName, firstInitial).
     * Handles all three formats + compound last names (van der Berg etc.)
     */
    fun normalize(raw: String): Pair<String, String> {
        val cleaned = raw
            .replace(".", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()

        val tokens = cleaned.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return Pair("", "")

        // Detect "Last First" format (live-tennis.eu):
        // Heuristic: if first token is long (>3) and second token looks like an initial or short given name,
        // and first token is not a known particle → treat as "Last First"
        // More reliable: if there are exactly 2 tokens and the second looks abbreviated (1-2 chars) → "Last Initial"
        // But live-tennis gives full names reversed, so we use a different signal:
        // live-tennis names are stored like "Zverev Alexander" with no dot — we can't know for sure.
        // Solution: we try BOTH interpretations and pick the one that matches.

        // For normalization we extract:
        // - lastName: last N tokens where leading tokens are particles
        // - firstInitial: first char of the first non-particle, non-lastName token

        return extractLastAndInitial(tokens)
    }

    private fun extractLastAndInitial(tokens: List<String>): Pair<String, String> {
        if (tokens.size == 1) return Pair(tokens[0], "")

        // Build lastName by taking tokens from the end, absorbing preceding particles
        val lastNameTokens = mutableListOf(tokens.last())
        var i = tokens.size - 2
        while (i >= 1 && tokens[i] in NAME_PARTICLES) {
            lastNameTokens.add(0, tokens[i])
            i--
        }
        val lastName = lastNameTokens.joinToString(" ")
        val firstInitial = tokens[0].firstOrNull()?.toString() ?: ""
        return Pair(lastName, firstInitial)
    }

    /**
     * Run after Elo + Rankings are synced.
     * For every unmatched player: try to find their eloKey and rankingKey.
     */
    suspend fun matchAll() {
        matchElo()
        matchRankings()
    }

    private suspend fun matchElo() {
        val unmatched = playerDao.getUnmatchedElo()
        if (unmatched.isEmpty()) return

        // Load all elo entries into memory (typically ~1500 players, ~100KB)
        val allElo = eloDao.getAllElo()
        // Build index: lastName → list of (key, firstInitial)
        val eloIndex = buildIndex(allElo.map { it.playerKey to it.playerName })

        var matched = 0
        for (player in unmatched) {
            val eloKey = findBestMatch(player.lastName, player.firstInitial, eloIndex)
                // Also try reversed interpretation (live-tennis reverses names)
                ?: findBestMatchReversed(player.displayName, eloIndex)
            if (eloKey != null) {
                playerDao.updateEloKey(player.apiKey, eloKey)
                matched++
            }
        }
        Timber.d("PlayerMatcher: matched $matched/${unmatched.size} players to Elo entries")
    }

    private suspend fun matchRankings() {
        val unmatched = playerDao.getUnmatchedRanking()
        if (unmatched.isEmpty()) return

        val allRankings = rankingDao.getAllRankingsOnce()
        val rankIndex = buildIndex(allRankings.map { it.playerKey to it.playerName })

        var matched = 0
        for (player in unmatched) {
            val tour = player.tour
            val rankingKey = findBestMatch(player.lastName, player.firstInitial, rankIndex)
                ?: findBestMatchReversed(player.displayName, rankIndex)
            if (rankingKey != null && tour != null) {
                playerDao.updateRankingKey(player.apiKey, rankingKey, tour)
                matched++
            }
        }
        Timber.d("PlayerMatcher: matched $matched/${unmatched.size} players to Ranking entries")
    }

    // index: lastName → list of Pair(storageKey, firstInitial)
    private fun buildIndex(entries: List<Pair<String, String>>): Map<String, List<Pair<String, String>>> {
        val index = mutableMapOf<String, MutableList<Pair<String, String>>>()
        for ((key, name) in entries) {
            val (lastName, initial) = normalize(name)
            if (lastName.isNotBlank()) {
                index.getOrPut(lastName) { mutableListOf() }.add(key to initial)
            }
        }
        return index
    }

    private fun findBestMatch(
        lastName: String,
        firstInitial: String,
        index: Map<String, List<Pair<String, String>>>
    ): String? {
        val candidates = index[lastName] ?: return null
        if (candidates.size == 1) return candidates[0].first
        // Disambiguate by first initial
        val byInitial = candidates.filter { it.second == firstInitial }
        return when (byInitial.size) {
            1 -> byInitial[0].first
            0 -> candidates[0].first  // best guess: first candidate
            else -> byInitial[0].first
        }
    }

    /**
     * Tries the reversed interpretation: treats first token as lastName.
     * Handles live-tennis.eu "Zverev Alexander" stored in API as "A. Zverev".
     */
    private fun findBestMatchReversed(
        displayName: String,
        index: Map<String, List<Pair<String, String>>>
    ): String? {
        val tokens = displayName
            .replace(".", " ").replace("-", " ")
            .trim().lowercase().split(" ").filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        // Try first token as last name
        val reversedLastName = tokens[0]
        val reversedInitial = tokens[1].firstOrNull()?.toString() ?: ""
        return findBestMatch(reversedLastName, reversedInitial, index)
    }

    /**
     * Build a PlayerEntity from a match row (called when a new player appears in matches).
     */
    fun buildPlayerEntity(apiKey: String, displayName: String, tour: String?): PlayerEntity {
        val (lastName, firstInitial) = normalize(displayName)
        return PlayerEntity(
            apiKey       = apiKey,
            displayName  = displayName,
            lastName     = lastName,
            firstInitial = firstInitial,
            tour         = tour
        )
    }
}
