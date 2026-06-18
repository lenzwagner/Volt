package com.lenz.tennisapp.ui.screens.home

import com.lenz.tennisapp.domain.model.Tournament
import com.lenz.tennisapp.domain.model.TournamentCategory

enum class TourType(val label: String) {
    ATP("ATP"),
    WTA("WTA"),
    JUNIORS("Juniors")
}

/**
 * Grouped category filter – each entry spans both ATP and WTA variants.
 * "Master" in user's language = Grand Slam.
 */
enum class FilterCategory(
    val label: String,
    val categories: Set<TournamentCategory>
) {
    GRAND_SLAM("Grand Slam", setOf(TournamentCategory.GRAND_SLAM)),
    MASTERS   ("Masters",    setOf(TournamentCategory.ATP_MASTERS_1000, TournamentCategory.WTA_1000)),
    FIVE00    ("500er",      setOf(TournamentCategory.ATP_500,          TournamentCategory.WTA_500)),
    TWO50     ("250er",      setOf(TournamentCategory.ATP_250,          TournamentCategory.WTA_250)),
    CHALLENGER("Challenger", setOf(TournamentCategory.CHALLENGER)),
    ITF       ("ITF",        setOf(TournamentCategory.ITF));
}

enum class MatchType(val label: String) {
    SINGLES("Singles"),
    DOUBLES("Doubles")
}

data class LiveFilterState(
    val tours:      Set<TourType>       = emptySet(),
    val categories: Set<FilterCategory> = emptySet(),
    val matchTypes: Set<MatchType>      = emptySet()  // NEW: Singles/Doubles filter
) {
    val isActive get() = tours.isNotEmpty() || categories.isNotEmpty() || matchTypes.isNotEmpty()

    fun matches(tournament: Tournament): Boolean {
        // ── Always exclude Boys/Girls Juniors ──────────────────────────────
        val juniorKeywords = listOf(
            "Boys", "Girls", "Junior", "Juniors", "Youth",
            "U18", "U-18", "U21", "U-21", "U16", "U-16", "U14", "U-14",
            "Jeunesse", "Kinder"
        )

        val isJuniorTournament = juniorKeywords.any { tournament.name.contains(it, ignoreCase = true) }
        val isJuniorMatch = tournament.matches.any { m ->
            juniorKeywords.any { k -> m.homePlayer.name.contains(k, ignoreCase = true) || m.awayPlayer.name.contains(k, ignoreCase = true) }
        }

        if (isJuniorTournament || isJuniorMatch) return false

        // ── Tour Filter ───────────────────────────────────────────────────
        if (tours.isNotEmpty() && tours.size < 2) { // Change to 2 because we only care about ATP vs WTA in selection
            val id = tournament.id.lowercase()
            val name = tournament.name.lowercase()
            
            val isWta = id.contains("wta") || name.contains("wta") || 
                        tournament.category == TournamentCategory.WTA_1000 ||
                        tournament.category == TournamentCategory.WTA_500 ||
                        tournament.category == TournamentCategory.WTA_250
            
            val isAtp = id.contains("atp") || name.contains("atp") || 
                        id.contains("challenger_men") ||
                        tournament.category == TournamentCategory.ATP_MASTERS_1000 ||
                        tournament.category == TournamentCategory.ATP_500 ||
                        tournament.category == TournamentCategory.ATP_250 ||
                        (tournament.category == TournamentCategory.CHALLENGER && !isWta)
            
            // For Grand Slams/ITF where both tours might be mixed, check match IDs
            val matchesWta = if (!isWta && !isAtp) tournament.matches.any { it.leagueId.contains("wta", true) } else isWta
            val matchesAtp = if (!isWta && !isAtp) tournament.matches.any { it.leagueId.contains("atp", true) || it.leagueId.contains("challenger", true) } else isAtp

            val wantAtp = TourType.ATP in tours
            val wantWta = TourType.WTA in tours

            if (wantAtp && !isAtp && !matchesAtp) return false
            if (wantWta && !isWta && !matchesWta) return false
        }

        // ── Category Filter ───────────────────────────────────────────────
        if (categories.isNotEmpty()) {
            val inCategory = categories.any { fc -> tournament.category in fc.categories }
            if (!inCategory) return false
        }

        // ── Match Type Filter (already applied match-by-match in HomeScreen, but keeping for consistency) ──
        if (matchTypes.isNotEmpty()) {
            val wantSingles = MatchType.SINGLES in matchTypes
            val wantDoubles = MatchType.DOUBLES in matchTypes
            
            val hasSingles = tournament.matches.any { !it.homePlayer.name.contains("/") }
            val hasDoubles = tournament.matches.any { it.homePlayer.name.contains("/") }

            val matchTypeOk = (wantSingles && hasSingles) || (wantDoubles && hasDoubles)
            if (!matchTypeOk) return false
        }

        return true
    }
}
