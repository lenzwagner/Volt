package com.lenz.tennisapp.ui.components

import com.lenz.tennisapp.domain.model.TennisMatch

data class TipItem(
    val match: TennisMatch,
    val aiProb: Float,  // 0.5 to 1.0
    val winnerName: String  // Name of predicted winner
)
