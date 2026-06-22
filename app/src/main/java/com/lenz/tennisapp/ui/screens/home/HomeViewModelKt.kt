package com.lenz.tennisapp.ui.screens.home

import com.lenz.tennisapp.domain.model.TennisMatch
import com.lenz.tennisapp.domain.model.Tournament
import com.lenz.tennisapp.domain.model.TournamentCategory
import java.util.Locale

fun filterTournaments(list: List<Tournament>, tour: TourFilter, format: FormatFilter, category: CategoryFilter): List<Tournament> {
    return list.mapNotNull { t ->
        val filteredMatches = t.matches.filter { match ->
            matchTour(match, tour) && matchFormat(match, format)
        }
        
        if (filteredMatches.isNotEmpty()) {
            t.copy(matches = filteredMatches)
        } else {
            null
        }
    }
}

fun matchTour(match: TennisMatch, tour: TourFilter): Boolean {
    val t = match.eventType.lowercase(Locale.ROOT)
    return when (tour) {
        TourFilter.ALL -> true
        TourFilter.ATP -> t.contains("atp") || (t.contains("challenger") && !t.contains("women"))
        TourFilter.WTA -> t.contains("wta") || t.contains("women")
    }
}

fun matchFormat(match: TennisMatch, format: FormatFilter): Boolean {
    val t = match.eventType.lowercase(Locale.ROOT)
    return when (format) {
        FormatFilter.ALL -> true
        FormatFilter.SINGLES -> t.contains("singles")
        FormatFilter.DOUBLES -> t.contains("doubles") || t.contains("mixed")
    }
}

fun matchCategory(cat: TournamentCategory, filter: CategoryFilter): Boolean {
    return when (filter) {
        CategoryFilter.ALL -> true
        CategoryFilter.GRAND_SLAM -> cat == TournamentCategory.GRAND_SLAM
        CategoryFilter.MASTERS_1000 -> cat == TournamentCategory.ATP_MASTERS_1000 || cat == TournamentCategory.WTA_1000
        CategoryFilter.F500 -> cat == TournamentCategory.ATP_500 || cat == TournamentCategory.WTA_500
        CategoryFilter.F250 -> cat == TournamentCategory.ATP_250 || cat == TournamentCategory.WTA_250 || cat == TournamentCategory.WTA_125
        CategoryFilter.CHALLENGER -> cat == TournamentCategory.CHALLENGER || 
                                     cat == TournamentCategory.CHALLENGER_175 ||
                                     cat == TournamentCategory.CHALLENGER_125 ||
                                     cat == TournamentCategory.CHALLENGER_100 ||
                                     cat == TournamentCategory.CHALLENGER_75 ||
                                     cat == TournamentCategory.CHALLENGER_50
    }
}
