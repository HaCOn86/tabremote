package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AdaptiveWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

fun adaptiveWidthClassFor(maxWidth: Dp): AdaptiveWidthClass = when {
    maxWidth < 600.dp -> AdaptiveWidthClass.COMPACT
    maxWidth < 840.dp -> AdaptiveWidthClass.MEDIUM
    else -> AdaptiveWidthClass.EXPANDED
}

fun AdaptiveWidthClass.isTablet(): Boolean = this != AdaptiveWidthClass.COMPACT
