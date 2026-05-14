package com.myhealth.app.ui.common

import androidx.compose.runtime.Stable

/**
 * Performance optimization guidelines for Compose UI.
 *
 * 1. Mark data classes passed to composables with @Stable or @Immutable
 *    to help the compiler skip recomposition when references haven't changed.
 *
 * 2. LazyColumn best practices:
 *    - Always provide a stable `key` in items() to avoid unnecessary recompositions.
 *    - Avoid putting complex composables inline; extract them to named functions.
 *    - Use `contentType` parameter to improve recycling when mixing item types.
 *
 * 3. Use `remember {}` to avoid re-allocating objects on every recomposition.
 *    Prefer `derivedStateOf` for computed values that depend on other state.
 *
 * 4. Avoid allocations in draw scopes (Canvas, drawBehind, drawWithContent).
 *    Pre-compute colors, paints, and paths in remember blocks.
 */
@Stable
data class PerformanceConfig(
    val prefetchDistance: Int = 5,
    val maxCachedItems: Int = 20,
)

object PerformanceConstants {
    const val LAZY_COLUMN_PREFETCH_DISTANCE = 5
    const val MAX_COMPOSITION_LOCALS = 10
    const val DEBOUNCE_MILLIS = 300L
}
