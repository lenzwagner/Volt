package com.lenz.tennisapp.ui.theme

import androidx.compose.animation.core.*

/** Bouncy spring — used on all animated bars and cards (Material Expressive) */
val ExpressiveSpring: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

val ExpressiveTween: AnimationSpec<Float> = tween(
    durationMillis = 500,
    easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
)
