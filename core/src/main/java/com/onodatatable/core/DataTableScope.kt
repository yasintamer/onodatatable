package com.onodatatable.core

import androidx.compose.runtime.Composable

interface DataTableScope<T, K, S> {
    val columnHeadersComposable: DataItemComposable<T> // Inferred T
    val rowHeadersComposable: DataItemComposable<K> // Inferred K
    val cellComposable: DataItemComposable<S> // Inferred S

    var topContent: @Composable (() -> Unit)?

    var bottomContent: @Composable (() -> Unit)?

    fun columnHeaders(list: List<T>, columnHeadersComposable: DataItemComposable<T>)
    fun rowHeaders(list: List<K>, rowHeadersComposable: DataItemComposable<K>)
    fun cells(list: List<List<S>>, cellComposable: DataItemComposable<S>)

    fun topContent(content: @Composable () -> Unit)

    fun bottomContent(content: @Composable () -> Unit)
}

class DataTableScopeImpl<T, K, S> : DataTableScope<T, K, S> {
    lateinit var columnHeaders: List<T>
        private set
    lateinit var rowHeaders: List<K>
        private set
    lateinit var cells: List<List<S>>
        private set

    override var topContent: @Composable (() -> Unit)? = null

    override var bottomContent: @Composable (() -> Unit)? = null

    override lateinit var columnHeadersComposable: DataItemComposable<T>
        private set
    override lateinit var rowHeadersComposable: DataItemComposable<K>
        private set
    override lateinit var cellComposable: DataItemComposable<S>
        private set

    override fun columnHeaders(
        list: List<T>,
        columnHeadersComposable: DataItemComposable<T>
    ) {
        this.columnHeaders = list
        this.columnHeadersComposable = columnHeadersComposable
    }

    override fun rowHeaders(
        list: List<K>,
        rowHeadersComposable: DataItemComposable<K>
    ) {
        this.rowHeaders = list
        this.rowHeadersComposable = rowHeadersComposable
    }

    override fun cells(
        list: List<List<S>>,
        cellComposable: DataItemComposable<S>
    ) {
        this.cells = list
        this.cellComposable = cellComposable
    }

    override fun topContent(content: @Composable () -> Unit) { this.topContent = content }

    override fun bottomContent(content: @Composable () -> Unit) { this.bottomContent = content }

}