package com.onodatatable.core

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun <T, K, S> rememberDataTableItemProvider(
    dataTableScope: DataTableScope<T, K, S>.() -> Unit
): DataTableItemProvider<T, K, S> {
    val layoutScope = remember { DataTableScopeImpl<T, K, S>() }
    layoutScope.dataTableScope()

    return remember(layoutScope) {
        DataTableItemProviderImpl(
            columnHeadersState = derivedStateOf { layoutScope.columnHeaders },
            rowHeadersState = derivedStateOf { layoutScope.rowHeaders },
            cellState = derivedStateOf { layoutScope.cells },
            columnHeadersComposable = layoutScope.columnHeadersComposable,
            rowHeadersComposable = layoutScope.rowHeadersComposable,
            cellComposable = layoutScope.cellComposable,
            topContentState = derivedStateOf { layoutScope.topContent },
            bottomContentState = derivedStateOf { layoutScope.bottomContent }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
interface DataTableItemProvider<T, K, S>: LazyLayoutItemProvider {
    val rowCount: Int
    val columnCount: Int
    fun getLongestContentByColumn(): List<@Composable () -> Unit>

    val columnHeadersComposable: DataItemComposable<T>
    val rowHeadersComposable: DataItemComposable<K>
    val cellComposable: DataItemComposable<S>

    val topContent: (@Composable () -> Unit)?
    val bottomContent: (@Composable () -> Unit)?
    
    val topContentIndex: Int
    val bottomContentIndex: Int
}

class DataTableItemProviderImpl<T, K, S>(
    columnHeadersState: State<List<T>>,
    rowHeadersState: State<List<K>>,
    cellState: State<List<List<S>>>,
    override val columnHeadersComposable: DataItemComposable<T>,
    override val rowHeadersComposable: DataItemComposable<K>,
    override val cellComposable: DataItemComposable<S>,
    topContentState: State<(@Composable () -> Unit)?>,
    bottomContentState: State<(@Composable () -> Unit)?>
) : DataTableItemProvider<T,K,S> {

    private val columnHeaders by columnHeadersState
    private val rowHeaders by rowHeadersState
    private val cells by cellState
    
    override val topContent by topContentState
    override val bottomContent by bottomContentState

    private val tableItemCount: Int
        get() = (rowCount + 1) * columnCount

    override val itemCount: Int
        get() = tableItemCount + 2

    override val topContentIndex: Int get() = tableItemCount
    override val bottomContentIndex: Int get() = tableItemCount + 1

    override val columnCount: Int by derivedStateOf { columnHeaders.size }

    override fun getLongestContentByColumn() = columnHeaders.mapIndexed { index, columnHeader ->
        val rowResult: Pair<String, @Composable () -> Unit> = if(index == 0) {
            val maxRowHeaderOpt = rowHeaders.stream().max { o1, o2 -> o1.toString().length.compareTo(o2.toString().length) }
            if(maxRowHeaderOpt.isPresent) {
               val maxRowHeader = maxRowHeaderOpt.get()
                maxRowHeader.toString() to {
                    rowHeadersComposable(maxRowHeader)
                }
            } else {
                "" to {}
            }

        } else {
            var maxCellContent: S? = null
            cells.forEach { cellsInARow ->
                if(maxCellContent == null) {
                    maxCellContent = cellsInARow[index-1]
                } else {
                    if(cellsInARow[index-1].toString().length > maxCellContent.toString().length) {
                        maxCellContent = cellsInARow[index-1]
                    }
                }
            }
            val calculatedMaxCellContent = maxCellContent
            if(calculatedMaxCellContent != null) {
                calculatedMaxCellContent.toString() to {
                    cellComposable(calculatedMaxCellContent)
                }
            } else {
                "" to {}
            }
        }
        val result: @Composable () -> Unit = if(columnHeader.toString().length >= rowResult.first.length) {
            {
                columnHeadersComposable(columnHeader)
            }
        } else {
            rowResult.second
        }
        result
    }

    override val rowCount: Int by derivedStateOf { rowHeaders.size }

    @Composable
    override fun Item(index: Int, key: Any) {
        when (index) {
            topContentIndex -> topContent?.invoke()
            bottomContentIndex -> bottomContent?.invoke()
            else -> {
                if (index in 0 until columnCount) {
                    columnHeaders.getOrNull(index)?.let {
                        columnHeadersComposable(it)
                    }
                } else if (index % columnCount == 0) {
                    val rowIndex = (index / columnCount) - 1
                    rowHeaders.getOrNull(rowIndex)?.let {
                        rowHeadersComposable(it)
                    }
                } else {
                    val columnIndex = (index % columnCount) - 1
                    val rowIndex = (index / columnCount) - 1
                    cells.getOrNull(rowIndex)?.getOrNull(columnIndex)?.let {
                        cellComposable(it)
                    }
                }
            }
        }
    }
}
