package com.lenz.tennisapp.domain.model

data class Tournament(
    val id: String,
    val name: String,
    val location: String? = null,
    val category: TournamentCategory,
    val surface: Surface,
    val matches: List<TennisMatch>,
    val type: String? = null
)

data class TennisMatch(
    val id: String,
    val date: String,
    val time: String,
    val homePlayer: Player,
    val awayPlayer: Player,
    val status: MatchStatus,
    val score: String?,             // "6-4,7-5" set scores
    val gameScore: String? = null,  // current game: "40-15", "AD-40"
    val isHomeServing: Boolean? = null,
    val round: String?,
    val tournament: String,
    val leagueId: String,  // Add leagueId for tournament lookup
    val tournamentCategory: TournamentCategory,
    val surface: Surface,
    val eventType: String,
    val isQualifying: Boolean = false,
    val winnerKey: String? = null,
    val setScores: String? = null,
    val finalResult: String? = null
) {
    fun isQualifyingMatch(): Boolean {
        val r = round?.lowercase() ?: ""
        return r.contains("qualifying") || r.contains("pre-q") || isQualifying
    }
}

data class Player(
    val key: String,
    val name: String,
    val ranking: Int? = null,
    val nationality: String? = null,
    val logoUrl: String? = null,
    val atpPoints: Int? = null,
    val wtaPoints: Int? = null,
    val careerHighRanking: Int? = null,
    val birthDate: String? = null,
    val prizeMoneyYtd: Int? = null
)

data class MatchDetail(
    val match: TennisMatch,
    val stats: List<StatLine>,
    val h2h: H2HResult,
    val odds: List<BookmakerOdds>,
    val prediction: MatchPrediction,
    val player1Elo: PlayerEloProfile? = null,
    val player2Elo: PlayerEloProfile? = null,
    val homeRecentMatches: List<TennisMatch> = emptyList(),
    val awayRecentMatches: List<TennisMatch> = emptyList(),
    // year → (slamCode → result), e.g. "2024" → {"AO":"QF","USO":"F"}
    val player1GrandSlam: Map<String, Map<String, String>> = emptyMap(),
    val player2GrandSlam: Map<String, Map<String, String>> = emptyMap()
)

data class PlayerEloProfile(
    val eloOverall: Int,
    val eloClay: Int,
    val eloGrass: Int,
    val eloHard: Int,
    val eloIndoor: Int,
    val matchesPlayed: Int
)

data class StatLine(
    val label: String,
    val homeValue: String,
    val awayValue: String,
    val homeIsWinning: Boolean? = null
)

data class H2HResult(
    val player1Name: String,
    val player2Name: String,
    val player1Wins: Int,
    val player2Wins: Int,
    val recentMatches: List<H2HMatch>
)

data class H2HMatch(
    val date: String,
    val winner: String,
    val score: String,
    val tournament: String,
    val surface: Surface,
    val round: String = "",
    val p1Sets: Int = 0,
    val p2Sets: Int = 0,
    val p1Scores: List<String> = emptyList(),
    val p2Scores: List<String> = emptyList()
)

data class BookmakerOdds(
    val bookmakerName: String,
    val homeOdds: Double,
    val awayOdds: Double
)

data class MatchPrediction(
    val player1WinProbability: Float,
    val player2WinProbability: Float,
    val confidence: PredictionConfidence,
    val factors: List<PredictionFactor>
) {
    val player1WinPercent: Int
        get() {
            val sum = player1WinProbability + player2WinProbability
            val normalized = if (sum > 0) player1WinProbability / sum else 0.5f
            return (normalized * 100).toInt()
        }

    val player2WinPercent: Int
        get() = 100 - player1WinPercent
}

data class PredictionFactor(
    val label: String,
    val favoredPlayer: Int,  // 1 or 2
    val strength: Float      // 0..1
)

enum class PredictionConfidence { HIGH, MEDIUM, LOW }

enum class TournamentCategory(val displayName: String, val sortOrder: Int, val points: Int) {
    GRAND_SLAM("Grand Slam", 0, 2000),
    ATP_MASTERS_1000("Masters 1000", 1, 1000),
    WTA_1000("WTA 1000", 1, 1000),
    ATP_500("ATP 500", 2, 500),
    WTA_500("WTA 500", 2, 500),
    ATP_250("ATP 250", 3, 250),
    WTA_250("WTA 250", 3, 250),
    WTA_125("WTA 125", 4, 125),
    CHALLENGER_175("Challenger 175", 4, 175),
    CHALLENGER_125("Challenger 125", 4, 125),
    CHALLENGER_100("Challenger 100", 4, 100),
    CHALLENGER_75("Challenger 75", 4, 75),
    CHALLENGER_50("Challenger 50", 4, 50),
    CHALLENGER("Challenger", 4, 0),
    ITF("ITF", 5, 0),
    OTHER("Other", 6, 0);
}

enum class Surface(val displayName: String) {
    HARD("Hard"),
    CLAY("Clay"),
    GRASS("Grass"),
    INDOOR_HARD("Indoor Hard"),
    UNKNOWN("Unknown");
}

enum class MatchStatus {
    NOT_STARTED, TBD, LIVE, FINISHED, POSTPONED, CANCELLED;
}

// ─── User Prediction models ──────────────────────────────────────────────────

data class UserPrediction(
    val matchId: String,
    val predictedWinnerKey: String,
    val predictedWinnerName: String,
    val homePlayerKey: String,
    val homePlayerName: String,
    val awayPlayerKey: String,
    val awayPlayerName: String,
    val matchDate: String,
    val tournamentName: String,
    val predictedAt: Long,
    val isCorrect: Boolean?,           // null = pending
    val actualWinnerKey: String? = null,
    val actualWinnerName: String? = null
) {
    val isPending get() = isCorrect == null
    val isHomePicked get() = predictedWinnerKey == homePlayerKey
}

data class PredictionStats(
    val totalResolved: Int,
    val correct: Int,
    val pending: Int,
    val weeklyResolved: Int,
    val weeklyCorrect: Int,
    val monthlyResolved: Int,
    val monthlyCorrect: Int
) {
    val overallPct  get() = if (totalResolved > 0) correct * 100 / totalResolved else 0
    val weeklyPct   get() = if (weeklyResolved > 0) weeklyCorrect * 100 / weeklyResolved else 0
    val monthlyPct  get() = if (monthlyResolved > 0) monthlyCorrect * 100 / monthlyResolved else 0
}

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val isKeyExpired: Boolean = false) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
