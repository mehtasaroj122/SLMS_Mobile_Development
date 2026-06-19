package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StudentMyBooksRepository
import com.saroj.lmsmobile.ui.student.model.FineStatus
import com.saroj.lmsmobile.ui.student.model.MyBookStatus
import com.saroj.lmsmobile.ui.student.model.MyBookUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksFineFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSortOption
import com.saroj.lmsmobile.ui.student.model.MyBooksStatusFilter
import com.saroj.lmsmobile.ui.student.model.MyBooksSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyBooksTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class StudentMyBooksViewModel(
    private val repository: StudentMyBooksRepository
) : ViewModel() {

    private val _summaryState = MutableLiveData<NetworkResult<MyBooksSummaryUiModel>>()
    val summaryState: LiveData<NetworkResult<MyBooksSummaryUiModel>> = _summaryState

    private val _booksState = MutableLiveData<NetworkResult<List<MyBookUiModel>>>()
    val booksState: LiveData<NetworkResult<List<MyBookUiModel>>> = _booksState

    private val tabCache = mutableMapOf<MyBooksTab, List<MyBookUiModel>>()
    private var selectedTab = MyBooksTab.CURRENT
    private var query = ""
    private var statusFilter = MyBooksStatusFilter.ALL
    private var fineFilter = MyBooksFineFilter.ALL
    private var sortOption = MyBooksSortOption.DUE_DATE_ASC
    private var booksJob: Job? = null
    private var serverSummary: MyBooksSummaryUiModel? = null

    fun loadInitial() {
        loadSummary()
        switchTab(MyBooksTab.CURRENT, forceRefresh = true)
        preloadStatsTabs(excludeTab = MyBooksTab.CURRENT)
    }

    fun refresh() {
        loadSummary()
        switchTab(selectedTab, forceRefresh = true)
        preloadStatsTabs(excludeTab = selectedTab)
    }

    fun switchTab(tab: MyBooksTab, forceRefresh: Boolean = false) {
        selectedTab = tab
        val cachedBooks = tabCache[tab]
        if (!forceRefresh && cachedBooks != null) {
            publishFilteredBooks(cachedBooks)
            return
        }

        booksJob?.cancel()
        booksJob = viewModelScope.launch {
            val flow = when (tab) {
                MyBooksTab.CURRENT -> repository.getCurrentBooks()
                MyBooksTab.HISTORY -> repository.getHistoryBooks()
                MyBooksTab.DUE_SOON -> repository.getDueSoonBooks()
            }

            flow.collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _booksState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        tabCache[tab] = result.data
                        publishSummaryFromCacheIfNeeded()
                        publishFilteredBooks(result.data)
                    }
                    is NetworkResult.Error -> _booksState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _booksState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun setSearchQuery(value: String) {
        query = value.trim()
        publishFilteredBooks()
    }

    fun setStatusFilter(filter: MyBooksStatusFilter) {
        statusFilter = filter
        publishFilteredBooks()
    }

    fun setFineFilter(filter: MyBooksFineFilter) {
        fineFilter = filter
        publishFilteredBooks()
    }

    fun setSortOption(option: MyBooksSortOption) {
        sortOption = option
        publishFilteredBooks()
    }

    fun resetFilters() {
        query = ""
        statusFilter = MyBooksStatusFilter.ALL
        fineFilter = MyBooksFineFilter.ALL
        sortOption = MyBooksSortOption.DUE_DATE_ASC
        publishFilteredBooks()
    }

    fun currentFilterState(): FilterState {
        return FilterState(statusFilter, fineFilter, sortOption, selectedTab, query)
    }

    private fun loadSummary() {
        viewModelScope.launch {
            repository.getSummary().collect { result ->
                if (result is NetworkResult.Success) {
                    serverSummary = result.data
                }
                _summaryState.value = result
                if (result is NetworkResult.Success && result.data.looksEmpty()) {
                    publishSummaryFromCacheIfNeeded()
                }
            }
        }
    }

    private fun preloadStatsTabs(excludeTab: MyBooksTab) {
        MyBooksTab.entries
            .filterNot { it == excludeTab }
            .forEach { tab ->
                viewModelScope.launch {
                    val flow = when (tab) {
                        MyBooksTab.CURRENT -> repository.getCurrentBooks()
                        MyBooksTab.HISTORY -> repository.getHistoryBooks()
                        MyBooksTab.DUE_SOON -> repository.getDueSoonBooks()
                    }

                    flow.collect { result ->
                        if (result is NetworkResult.Success) {
                            tabCache[tab] = result.data
                            publishSummaryFromCacheIfNeeded()
                        }
                    }
                }
            }
    }

    private fun publishSummaryFromCacheIfNeeded() {
        val summary = serverSummary
        val cachedBooks = tabCache.values.flatten()
        if (cachedBooks.isEmpty()) return
        if (summary != null && !summary.looksEmpty()) return

        _summaryState.value = NetworkResult.Success(buildSummaryFromCache())
    }

    private fun buildSummaryFromCache(): MyBooksSummaryUiModel {
        val currentBooks = tabCache[MyBooksTab.CURRENT].orEmpty()
        val historyBooks = tabCache[MyBooksTab.HISTORY].orEmpty()
        val dueSoonBooks = tabCache[MyBooksTab.DUE_SOON].orEmpty()
        val allBooks = (currentBooks + historyBooks + dueSoonBooks).distinctBy { it.issueId }
        val pendingFine = allBooks
            .filter { it.fineStatus == FineStatus.UNPAID }
            .sumOf { it.fineAmountValue }

        return MyBooksSummaryUiModel(
            totalIssued = allBooks.size,
            currentlyBorrowed = currentBooks.count {
                it.status == MyBookStatus.ISSUED || it.status == MyBookStatus.DUE_SOON || it.status == MyBookStatus.OVERDUE
            },
            overdueBooks = allBooks.count { it.status == MyBookStatus.OVERDUE },
            pendingFine = formatCurrency(pendingFine),
            pendingFineValue = pendingFine
        )
    }

    private fun publishFilteredBooks(source: List<MyBookUiModel>? = tabCache[selectedTab]) {
        val books = source ?: return
        val filtered = books
            .filter { book -> matchesSearch(book) }
            .filter { book -> matchesStatus(book.status) }
            .filter { book -> matchesFineStatus(book.fineStatus) }
            .sortedWith(sortOption.comparator())

        _booksState.value = NetworkResult.Success(filtered)
    }

    private fun matchesSearch(book: MyBookUiModel): Boolean {
        if (query.isBlank()) return true
        return book.title.contains(query, ignoreCase = true) ||
            book.author.contains(query, ignoreCase = true) ||
            book.isbn.contains(query, ignoreCase = true)
    }

    private fun matchesStatus(status: MyBookStatus): Boolean {
        return statusFilter.status == null || statusFilter.status == status
    }

    private fun matchesFineStatus(status: FineStatus): Boolean {
        return fineFilter.status == null || fineFilter.status == status
    }

    private fun MyBooksSummaryUiModel.looksEmpty(): Boolean {
        return totalIssued == 0 && currentlyBorrowed == 0 && overdueBooks == 0 && pendingFineValue == 0.0
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.US, "₹%.2f", value)
    }

    private fun MyBooksSortOption.comparator(): Comparator<MyBookUiModel> {
        return when (this) {
            MyBooksSortOption.DUE_DATE_ASC -> compareBy<MyBookUiModel> { it.dueDateSort }
                .thenBy { it.title.lowercase() }
            MyBooksSortOption.DUE_DATE_DESC -> compareByDescending<MyBookUiModel> { it.dueDateSort }
                .thenBy { it.title.lowercase() }
            MyBooksSortOption.ISSUE_DATE -> compareByDescending<MyBookUiModel> { it.issueDateSort }
                .thenBy { it.title.lowercase() }
            MyBooksSortOption.FINE_AMOUNT -> compareByDescending<MyBookUiModel> { it.fineAmountValue }
                .thenBy { it.title.lowercase() }
        }
    }

    data class FilterState(
        val statusFilter: MyBooksStatusFilter,
        val fineFilter: MyBooksFineFilter,
        val sortOption: MyBooksSortOption,
        val selectedTab: MyBooksTab,
        val query: String
    )
}
