package com.lenz.tennisapp.domain.prediction

import com.lenz.tennisapp.data.db.dao.EloDao
import com.lenz.tennisapp.data.db.entities.EloRatingEntity
import com.lenz.tennisapp.domain.model.BookmakerOdds
import com.lenz.tennisapp.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp

@Singleton
class MatchPredictor @Inject constructor(
    private val eloDao: EloDao
) {
    companion object {
        // Logistic ELO formula: P(win) = 1 / (1 + 10^(-ΔElo/400))
        fun eloWinProbability(elo1: Double, elo2: Double): Float {
            return (1.0 / (1.0 + exp(-(elo1 - elo2) / 400.0 * Math.log(10.0)))).toFloat()
        }

        fun updateElo(winnerElo: Double, loserElo: Double, kFactor: Double = 32.0): Pair<Double, Double> {
            val expectedWinner = 1.0 / (1.0 + Math.pow(10.0, (loserElo - winnerElo) / 400.0))
            return Pair(
                winnerElo + kFactor * (1.0 - expectedWinner),
                loserElo + kFactor * (0.0 - (1.0 - expectedWinner))
            )
        }

        fun kFactor(category: TournamentCategory): Double = when (category) {
            TournamentCategory.GRAND_SLAM -> 48.0
            TournamentCategory.ATP_MASTERS_1000, TournamentCategory.WTA_1000 -> 40.0
            TournamentCategory.ATP_500, TournamentCategory.WTA_500 -> 32.0
            TournamentCategory.ATP_250, TournamentCategory.WTA_250 -> 24.0
            TournamentCategory.WTA_125 -> 20.0
            TournamentCategory.CHALLENGER,
            TournamentCategory.CHALLENGER_175,
            TournamentCategory.CHALLENGER_125,
            TournamentCategory.CHALLENGER_100,
            TournamentCategory.CHALLENGER_75,
            TournamentCategory.CHALLENGER_50 -> 20.0
            else -> 16.0
        }
    }

    suspend fun predict(
        match: TennisMatch,
        h2h: H2HResult?,
        odds: List<BookmakerOdds> = emptyList()
    ): MatchPrediction {
        val elo1 = eloDao.getElo(match.homePlayer.key)
            ?: eloDao.getEloByLastName(match.homePlayer.name.split(" ").last())
            ?: EloRatingEntity(playerKey = match.homePlayer.key, playerName = match.homePlayer.name)
        val elo2 = eloDao.getElo(match.awayPlayer.key)
            ?: eloDao.getEloByLastName(match.awayPlayer.name.split(" ").last())
            ?: EloRatingEntity(playerKey = match.awayPlayer.key, playerName = match.awayPlayer.name)

        // Surface-specific ELO
        val surfaceElo1 = surfaceElo(elo1, match.surface)
        val surfaceElo2 = surfaceElo(elo2, match.surface)

        // Weighted combination: 40% surface ELO, 40% overall ELO, 20% rank
        val eloProb = eloWinProbability(elo1.eloOverall, elo2.eloOverall)
        val surfaceProb = eloWinProbability(surfaceElo1, surfaceElo2)

        val rankProb = rankProbability(match.homePlayer.ranking, match.awayPlayer.ranking)
        val h2hProb = h2hProbability(h2h, match.homePlayer.name)

        // Weighted blend
        val hasRanking = match.homePlayer.ranking != null && match.awayPlayer.ranking != null
        val hasH2H = h2h != null && (h2h.player1Wins + h2h.player2Wins) >= 3

        val weights = when {
            hasRanking && hasH2H -> floatArrayOf(0.35f, 0.30f, 0.20f, 0.15f)
            hasRanking -> floatArrayOf(0.40f, 0.35f, 0.25f, 0.0f)
            hasH2H -> floatArrayOf(0.45f, 0.40f, 0.0f, 0.15f)
            else -> floatArrayOf(0.50f, 0.50f, 0.0f, 0.0f)
        }

        val probs = floatArrayOf(surfaceProb, eloProb, rankProb, h2hProb)
        var p1WinProb = 0f
        probs.forEachIndexed { i, p -> p1WinProb += weights[i] * p }

        // ── Blend in market odds to fix 50:50 when ELO data is missing ──────
        if (odds.isNotEmpty()) {
            val mP1 = odds.map { 1.0 / it.homeOdds }.average().toFloat()
            val mP2 = odds.map { 1.0 / it.awayOdds }.average().toFloat()
            val marketProb = (mP1 / (mP1 + mP2)).coerceIn(0.05f, 0.95f)
            // Weight market more when we have little internal data
            val internalWeight = when {
                elo1.matchesPlayed > 50 && elo2.matchesPlayed > 50 -> 0.80f
                elo1.matchesPlayed > 20 && elo2.matchesPlayed > 20 -> 0.65f
                else -> 0.40f
            }
            p1WinProb = internalWeight * p1WinProb + (1 - internalWeight) * marketProb
        }

        // Clamp to [0.05, 0.95] — never show > 95% confidence
        p1WinProb = p1WinProb.coerceIn(0.05f, 0.95f)

        val factors = buildFactors(elo1, elo2, surfaceElo1, surfaceElo2, match, h2h)
        val confidence = when {
            elo1.matchesPlayed > 50 && elo2.matchesPlayed > 50 -> PredictionConfidence.HIGH
            elo1.matchesPlayed > 20 && elo2.matchesPlayed > 20 -> PredictionConfidence.MEDIUM
            else -> PredictionConfidence.LOW
        }

        return MatchPrediction(
            player1WinProbability = p1WinProb,
            player2WinProbability = 1f - p1WinProb,
            confidence = confidence,
            factors = factors
        )
    }

    private fun surfaceElo(elo: EloRatingEntity, surface: Surface): Double = when (surface) {
        Surface.CLAY -> elo.eloClay
        Surface.GRASS -> elo.eloGrass
        Surface.HARD -> elo.eloHard
        Surface.INDOOR_HARD -> elo.eloIndoor
        else -> elo.eloOverall
    }

    private fun rankProbability(rank1: Int?, rank2: Int?): Float {
        if (rank1 == null || rank2 == null) return 0.5f
        // Higher rank (lower number) = better player
        // Use logistic on rank difference
        val diff = (rank2 - rank1).toDouble()
        return (1.0 / (1.0 + exp(-diff / 50.0))).toFloat()
    }

    private fun h2hProbability(h2h: H2HResult?, player1Name: String): Float {
        if (h2h == null) return 0.5f
        val total = h2h.player1Wins + h2h.player2Wins
        if (total == 0) return 0.5f
        return h2h.player1Wins.toFloat() / total
    }

    private fun buildFactors(
        elo1: EloRatingEntity,
        elo2: EloRatingEntity,
        surfaceElo1: Double,
        surfaceElo2: Double,
        match: TennisMatch,
        h2h: H2HResult?
    ): List<PredictionFactor> {
        val factors = mutableListOf<PredictionFactor>()

        val eloGap = abs(elo1.eloOverall - elo2.eloOverall)
        if (eloGap > 20) {
            factors.add(PredictionFactor(
                label = "ELO Rating",
                favoredPlayer = if (elo1.eloOverall > elo2.eloOverall) 1 else 2,
                strength = (eloGap / 400.0).coerceIn(0.0, 1.0).toFloat()
            ))
        }

        val surfaceGap = abs(surfaceElo1 - surfaceElo2)
        if (surfaceGap > 20) {
            factors.add(PredictionFactor(
                label = "${match.surface.displayName} ELO",
                favoredPlayer = if (surfaceElo1 > surfaceElo2) 1 else 2,
                strength = (surfaceGap / 400.0).coerceIn(0.0, 1.0).toFloat()
            ))
        }

        val r1 = match.homePlayer.ranking
        val r2 = match.awayPlayer.ranking
        if (r1 != null && r2 != null && abs(r1 - r2) > 10) {
            factors.add(PredictionFactor(
                label = "ATP/WTA Ranking",
                favoredPlayer = if (r1 < r2) 1 else 2,
                strength = (abs(r1 - r2).toFloat() / 200f).coerceIn(0f, 1f)
            ))
        }

        if (h2h != null && (h2h.player1Wins + h2h.player2Wins) >= 3) {
            val total = (h2h.player1Wins + h2h.player2Wins).toFloat()
            val gap = abs(h2h.player1Wins - h2h.player2Wins) / total
            if (gap > 0.2f) {
                factors.add(PredictionFactor(
                    label = "Head to Head",
                    favoredPlayer = if (h2h.player1Wins > h2h.player2Wins) 1 else 2,
                    strength = gap
                ))
            }
        }

        return factors.sortedByDescending { it.strength }.take(3)
    }

    suspend fun updateEloFromResult(
        winnerKey: String, winnerName: String,
        loserKey: String, loserName: String,
        surface: Surface, category: TournamentCategory
    ) {
        val winnerElo = eloDao.getElo(winnerKey) ?: EloRatingEntity(winnerKey, winnerName)
        val loserElo = eloDao.getElo(loserKey) ?: EloRatingEntity(loserKey, loserName)
        val k = kFactor(category)

        val (newWinnerOverall, newLoserOverall) = updateElo(winnerElo.eloOverall, loserElo.eloOverall, k)
        val winnerSurface = surfaceElo(winnerElo, surface)
        val loserSurface = surfaceElo(loserElo, surface)
        val (newWinnerSurface, newLoserSurface) = updateElo(winnerSurface, loserSurface, k)

        fun applyNewSurface(elo: EloRatingEntity, newVal: Double, surface: Surface) = when (surface) {
            Surface.CLAY -> elo.copy(eloClay = newVal)
            Surface.GRASS -> elo.copy(eloGrass = newVal)
            Surface.HARD -> elo.copy(eloHard = newVal)
            Surface.INDOOR_HARD -> elo.copy(eloIndoor = newVal)
            Surface.CLAY -> elo.copy(eloClay = newVal)
            else -> elo
        }

        eloDao.upsertElo(
            applyNewSurface(winnerElo.copy(
                eloOverall = newWinnerOverall,
                matchesPlayed = winnerElo.matchesPlayed + 1
            ), newWinnerSurface, surface)
        )
        eloDao.upsertElo(
            applyNewSurface(loserElo.copy(
                eloOverall = newLoserOverall,
                matchesPlayed = loserElo.matchesPlayed + 1
            ), newLoserSurface, surface)
        )
    }
}
