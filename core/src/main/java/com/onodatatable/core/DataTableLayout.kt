@file:OptIn(ExperimentalFoundationApi::class)

package com.onodatatable.core

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

typealias OnoMeasurePolicy = LazyLayoutMeasureScope.(Constraints) -> MeasureResult

@Composable
fun <T, K, S> DataTableLayout(
    modifier: Modifier = Modifier,
    content: DataTableScope<T, K, S>.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val itemProvider = rememberDataTableItemProvider(content)

    // --- Saver for Animatable<Float, AnimationVector1D> ---
    val animatableSaver = remember {
        Saver<Animatable<Float, AnimationVector1D>, Float>(
            save = { it.value },
            restore = { Animatable(it) }
        )
    }

    // --- Use rememberSaveable for the main scroll states ---
    val horizontalScrollState = rememberSaveable(saver = animatableSaver) { Animatable(0f) }
    val verticalScrollState = rememberSaveable(saver = animatableSaver) { Animatable(0f) }

    // Overscroll states are transient and don't need to be saved across configuration changes
    val horizontalOverscrollState = remember { Animatable(0f) }
    val verticalOverscrollState = remember { Animatable(0f) }
    // ---

    val overscrollPullMultiplier = 0.3f

    var maxHorizontalOffset by remember { mutableFloatStateOf(0f) }
    var maxVerticalOffset by remember { mutableFloatStateOf(0f) }

    val flingDecay = rememberSplineBasedDecay<Float>()

    val measuredWidthsPx = rememberMeasuredColumnWidths(itemProvider)
    val cumulativeWidths = remember(measuredWidthsPx) {
        measuredWidthsPx.runningFold(0f) { acc, width -> acc + width.toFloat() }
    }

    if (measuredWidthsPx.isEmpty()) {
        return
    }

    val layoutInfo = remember {
        object {
            var topContentHeight = 0
            var totalTableHeight = 0
        }
    }

    val gestureModifier = modifier
        .clipToBounds()
        .pointerInput(Unit) {
            val velocityTracker = VelocityTracker()
            var scrollDirection: ScrollDirection? = null
            var allowHorizontalScroll = true

            detectDragGestures(
                onDragStart = { startOffset ->
                    scrollDirection = null
                    val startY = startOffset.y + verticalScrollState.value
                    allowHorizontalScroll = startY >= layoutInfo.topContentHeight &&
                            startY <= (layoutInfo.topContentHeight + layoutInfo.totalTableHeight)

                    coroutineScope.launch {
                        // Stop all animations
                        horizontalScrollState.stop()
                        horizontalOverscrollState.stop()
                        verticalScrollState.stop()
                        verticalOverscrollState.stop()
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()

                    if (scrollDirection == null) {
                        scrollDirection = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            ScrollDirection.Horizontal
                        } else {
                            ScrollDirection.Vertical
                        }
                    }

                    if (scrollDirection == ScrollDirection.Horizontal && !allowHorizontalScroll) {
                        return@detectDragGestures
                    }
                    velocityTracker.addPosition(change.uptimeMillis, change.position)


                    coroutineScope.launch {
                        // --- START OF FIX: Add a hard limit for overscroll ---
                        val maxOverscrollPx =
                            150f // A reasonable hard limit for the overscroll distance

                        if (scrollDirection == ScrollDirection.Horizontal) {
                            val currentScroll = horizontalScrollState.value
                            val newScroll = currentScroll - dragAmount.x

                            if (newScroll in 0f..maxHorizontalOffset) {
                                // If we were overscrolling, ensure it's reset before normal scrolling
                                if (horizontalOverscrollState.value != 0f) {
                                    horizontalOverscrollState.snapTo(0f)
                                }
                                horizontalScrollState.snapTo(newScroll)
                            } else {
                                val newOverscroll =
                                    horizontalOverscrollState.value - dragAmount.x * overscrollPullMultiplier
                                // Clamp the overscroll state to the hard limit
                                horizontalOverscrollState.snapTo(
                                    newOverscroll.coerceIn(
                                        -maxOverscrollPx,
                                        maxOverscrollPx
                                    )
                                )
                            }
                        } else { // Vertical
                            val currentScroll = verticalScrollState.value
                            val newScroll = currentScroll - dragAmount.y

                            if (newScroll in 0f..maxVerticalOffset) {
                                // If we were overscrolling, ensure it's reset before normal scrolling
                                if (verticalOverscrollState.value != 0f) {
                                    verticalOverscrollState.snapTo(0f)
                                }
                                verticalScrollState.snapTo(newScroll)
                            } else {
                                val newOverscroll =
                                    verticalOverscrollState.value - dragAmount.y * overscrollPullMultiplier
                                // Clamp the overscroll state to the hard limit
                                verticalOverscrollState.snapTo(
                                    newOverscroll.coerceIn(
                                        -maxOverscrollPx,
                                        maxOverscrollPx
                                    )
                                )
                            }
                        }
                        // --- END OF FIX ---
                    }
                },
                onDragEnd = {
                    val velocity = velocityTracker.calculateVelocity()
                    coroutineScope.launch {
                        if (scrollDirection == ScrollDirection.Horizontal) {
                            if (allowHorizontalScroll) {
                                horizontalScrollState.animateDecay(-velocity.x, flingDecay)
                            }
                        } else {
                            verticalScrollState.animateDecay(-velocity.y, flingDecay)
                        }

                        // ALWAYS animate the overscroll states back to 0.
                        horizontalOverscrollState.animateTo(
                            0f,
                            spring(stiffness = Spring.StiffnessMediumLow)
                        )
                        verticalOverscrollState.animateTo(
                            0f,
                            spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            )
        }



    val measurePolicy = remember<OnoMeasurePolicy>(itemProvider, cumulativeWidths) {
        { constraints ->
            val sumOfWidths = cumulativeWidths.lastOrNull() ?: 0f
            val cellHeight = 150

            // 1. Measure Top/Bottom Content if provided
            val topPlaceable = itemProvider.topContent?.let {
                this.compose(itemProvider.topContentIndex)
                    .map { it.measure(constraints.copy(minHeight = 0)) }
                    .firstOrNull()
            }
            val bottomPlaceable = itemProvider.bottomContent?.let {
                this.compose(itemProvider.bottomContentIndex)
                    .map { it.measure(constraints.copy(minHeight = 0)) }
                    .firstOrNull()
            }

            val topHeight = topPlaceable?.height ?: 0
            val bottomHeight = bottomPlaceable?.height ?: 0
            val totalTableHeight = (itemProvider.rowCount + 1) * cellHeight // +1 for header row

            layoutInfo.topContentHeight = topHeight
            layoutInfo.totalTableHeight = totalTableHeight

            // 2. Calculate Max Offsets (Table height + Top + Bottom)
            maxHorizontalOffset = (sumOfWidths - constraints.maxWidth).coerceAtLeast(0f)
            maxVerticalOffset = (topHeight + totalTableHeight + bottomHeight - constraints.maxHeight).toFloat()
                .coerceAtLeast(0f)

            horizontalScrollState.updateBounds(0f, maxHorizontalOffset)
            verticalScrollState.updateBounds(0f, maxVerticalOffset)

            val horizontalScroll = horizontalScrollState.value + horizontalOverscrollState.value
            val verticalScroll = verticalScrollState.value + verticalOverscrollState.value

            // 3. LAZY LOGIC ADJUSTMENT:
            // The table itself starts at 'topHeight'.
            // We calculate which rows are visible relative to the table's start position.
            val tableScrollY = (verticalScroll - topHeight).coerceAtLeast(0f)

            var firstVisibleRowIndex = (tableScrollY / cellHeight).toInt()
            var lastVisibleRowIndex = ((tableScrollY + constraints.maxHeight) / cellHeight).toInt()

            firstVisibleRowIndex = firstVisibleRowIndex.coerceIn(0, itemProvider.rowCount)
            lastVisibleRowIndex = lastVisibleRowIndex.coerceIn(0, itemProvider.rowCount)

            // (Horizontal logic remains exactly the same as your current code)
            val viewPortEndX = horizontalScroll + constraints.maxWidth
            var firstVisibleColumnIndex = cumulativeWidths.binarySearch { it.compareTo(horizontalScroll) }
                .let { if (it < 0) (-it - 1) else it }.let { if (it > 0) it - 1 else 0 }
            var lastVisibleColumnIndex = firstVisibleColumnIndex
            for (i in firstVisibleColumnIndex until itemProvider.columnCount) {
                if (cumulativeWidths[i] < viewPortEndX) lastVisibleColumnIndex = i else break
            }
            firstVisibleColumnIndex =
                firstVisibleColumnIndex.coerceIn(0, itemProvider.columnCount - 1)
            lastVisibleColumnIndex =
                lastVisibleColumnIndex.coerceIn(0, itemProvider.columnCount - 1)

            val placeables = mutableListOf<Pair<Int, Placeable>>()

            // 4. Measure Table Cells (Lazy)
            if (lastVisibleRowIndex >= firstVisibleRowIndex && lastVisibleColumnIndex >= firstVisibleColumnIndex) {
                for (rowIndex in firstVisibleRowIndex..lastVisibleRowIndex) {
                    for (columnIndex in firstVisibleColumnIndex..lastVisibleColumnIndex) {
                        val itemIndex = rowIndex * itemProvider.columnCount + columnIndex
                        if (itemIndex < itemProvider.itemCount) {
                            val measured = compose(itemIndex).map {
                                it.measure(
                                    Constraints.fixed(
                                        measuredWidthsPx[columnIndex],
                                        cellHeight
                                    )
                                )
                            }
                            placeables.addAll(measured.map { itemIndex to it })
                        }
                    }
                }
            }

            layout(constraints.maxWidth, constraints.maxHeight) {
                // 5. Place Top Content
                topPlaceable?.placeRelative(0, (-verticalScroll).roundToInt())

                // 6. Place Table Cells (Offset by topHeight)
                placeables.forEach { (itemIndex, placeable) ->
                    val rowIndex = itemIndex / itemProvider.columnCount
                    val columnIndex = itemIndex % itemProvider.columnCount
                    val x = cumulativeWidths[columnIndex] - horizontalScroll
                    // The key: add topHeight to the row calculation
                    val y = (topHeight + (rowIndex * cellHeight)) - verticalScroll
                    placeable.placeRelative(x.roundToInt(), y.roundToInt())
                }

                // 7. Place Bottom Content
                bottomPlaceable?.placeRelative(
                    0,
                    (topHeight + totalTableHeight - verticalScroll).roundToInt()
                )
            }
        }
    }

    LazyLayout(
        modifier = gestureModifier,
        itemProvider = { itemProvider },
        measurePolicy = measurePolicy
    )
}

private enum class ScrollDirection {
    Horizontal, Vertical
}

@Composable
private fun <T, K, S> rememberMeasuredColumnWidths(
    itemProvider: DataTableItemProvider<T, K, S>
): List<Int> {
    val measuredWidthsPx: MutableState<List<Int>> = remember { mutableStateOf(emptyList()) }
    SubcomposeLayout { _ ->
        if (measuredWidthsPx.value.isEmpty()) {
            val longestContentPlaceables =
                itemProvider.getLongestContentByColumn().mapIndexed { index, longestContentComposable ->
                    subcompose("longestContent_$index") {
                        longestContentComposable()
                    }.first().measure(Constraints())
                }
            measuredWidthsPx.value = longestContentPlaceables.map { it.width }
        }
        layout(0, 0) {}
    }
    return measuredWidthsPx.value
}
