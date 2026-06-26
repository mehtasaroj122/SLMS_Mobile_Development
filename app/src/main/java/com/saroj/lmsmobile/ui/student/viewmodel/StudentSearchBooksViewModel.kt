package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import com.saroj.lmsmobile.ui.student.model.BookRequestState
import com.saroj.lmsmobile.ui.student.model.StudentSearchBookUiModel
import com.saroj.lmsmobile.utils.Constants
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudentSearchBooksViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _booksState = MutableLiveData<NetworkResult<List<StudentSearchBookUiModel>>>()
    val booksState: LiveData<NetworkResult<List<StudentSearchBookUiModel>>> = _booksState

    private val _requestResult = MutableLiveData<NetworkResult<String>>()
    val requestResult: LiveData<NetworkResult<String>> = _requestResult

    private val _totalBooks = MutableLiveData(0)
    val totalBooks: LiveData<Int> = _totalBooks

    private val _bottomLoading = MutableLiveData(false)
    val bottomLoading: LiveData<Boolean> = _bottomLoading

    private val loadedBooks = mutableListOf<StudentSearchBookUiModel>()
    private val studentRequestStates = mutableMapOf<Int, BookRequestState>()
    private var selectedCategory: String? = null
    private var availabilityFilter: AvailabilityFilter = AvailabilityFilter.ALL
    private var selectedCondition: String? = null
    private var selectedSort: SortOption = SortOption.TITLE_ASC
    private var currentPage = 1
    private var lastPage = 1
    private var isLoading = false
    private var isSearching = false
    private var currentQuery = ""
    private var catalogJob: Job? = null
    private var searchJob: Job? = null

    private companion object {
        const val PAGE_SIZE = 20
        const val NEXT_PAGE_DELAY_MS = 1_000L
    }

    fun loadBooks(refresh: Boolean = false) {
        if (isLoading) return
        if (refresh) resetPagination(keepQuery = false)

        isSearching = false
        currentQuery = ""
        refreshStudentRequestStates()
        loadPage(page = if (refresh) 1 else currentPage, append = !refresh && loadedBooks.isNotEmpty())
    }

    fun searchBooks(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isBlank()) {
                resetPagination(keepQuery = false)
                loadBooks(refresh = true)
                return@launch
            }

            delay(350)
            resetPagination(keepQuery = true)
            isSearching = true
            currentQuery = trimmedQuery
            refreshStudentRequestStates()
            loadSearchPage(trimmedQuery, page = 1, append = false)
        }
    }

    fun loadNextPage() {
        if (isLoading || currentPage >= lastPage) return

        val nextPage = currentPage + 1
        if (isSearching) {
            loadSearchPage(currentQuery, nextPage, append = true)
        } else {
            loadPage(nextPage, append = true)
        }
    }

    fun refresh() {
        if (isSearching && currentQuery.isNotBlank()) {
            searchBooks(currentQuery)
        } else {
            loadBooks(refresh = true)
        }
    }

    fun setCategory(category: String?) {
        selectedCategory = category
        reloadCurrentCatalog()
    }

    fun setAvailabilityFilter(filter: AvailabilityFilter) {
        availabilityFilter = filter
        reloadCurrentCatalog()
    }

    fun setCondition(condition: String?) {
        selectedCondition = condition
        reloadCurrentCatalog()
    }

    fun setSort(sortOption: SortOption) {
        selectedSort = sortOption
        reloadCurrentCatalog()
    }

    fun resetFilters() {
        searchJob?.cancel()
        selectedCategory = null
        availabilityFilter = AvailabilityFilter.ALL
        selectedCondition = null
        selectedSort = SortOption.TITLE_ASC
        resetPagination(keepQuery = false)
        refreshStudentRequestStates()
        loadPage(page = 1, append = false)
    }

    fun refreshStudentRequestStates() {
        viewModelScope.launch {
            bookRepository.getStudentBookRequests(page = 1).collect { result ->
                if (result is NetworkResult.Success) {
                    studentRequestStates.clear()
                    result.data.data.forEach { request ->
                        val bookId = request.bookId ?: request.book?.id ?: return@forEach
                        val state = request.status.toBookRequestState(availableCopies = 1)
                        if (state != BookRequestState.NONE) {
                            studentRequestStates[bookId] = state
                        }
                    }
                    applyStudentRequestStatesToLoadedBooks()
                }
            }
        }
    }

    fun categories(): List<String> = loadedBooks.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    fun conditions(): List<String> = listOf("New", "Good", "Damaged")

    fun currentFilterState(): FilterState {
        return FilterState(selectedCategory, availabilityFilter, selectedCondition, selectedSort)
    }

    fun submitRequest(book: StudentSearchBookUiModel) {
        if (book.requestState == BookRequestState.LOADING) return
        updateBookState(book.id, BookRequestState.LOADING)

        viewModelScope.launch {
            bookRepository.submitStudentBookRequest(book.id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> Unit
                    is NetworkResult.Success -> {
                        updateBookState(book.id, BookRequestState.PENDING)
                        _requestResult.value = NetworkResult.Success(
                            result.data.message ?: "Request submitted successfully."
                        )
                        refreshStudentRequestStates()
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        val likelyExistingRequest = result.code == Constants.HTTP_UNPROCESSABLE_ENTITY &&
                            result.message.contains("request", ignoreCase = true) &&
                            (result.message.contains("pending", ignoreCase = true) ||
                                result.message.contains("already", ignoreCase = true))
                        updateBookState(
                            book.id,
                            if (likelyExistingRequest) BookRequestState.PENDING else book.requestState
                        )
                        _requestResult.value = NetworkResult.Error(result.message, result.code)
                    }
                    is NetworkResult.Unauthorized -> {
                        updateBookState(book.id, book.requestState)
                        _requestResult.value = NetworkResult.Unauthorized()
                    }
                }
            }
        }
    }

    private fun loadPage(page: Int, append: Boolean) {
        if (isLoading) return
        isLoading = true
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            if (append) {
                _bottomLoading.value = true
                delay(NEXT_PAGE_DELAY_MS)
            }
            if (!append) _booksState.value = NetworkResult.Loading()
            bookRepository.getBooks(
                page = page,
                category = selectedCategory,
                availability = availabilityParam(),
                condition = selectedCondition,
                sort = selectedSort.apiValue,
                pageSize = PAGE_SIZE
            ).collect { result ->
                handleBooksResult(result, append)
            }
        }
    }

    private fun loadSearchPage(query: String, page: Int, append: Boolean) {
        if (isLoading) return
        isLoading = true
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            if (append) {
                _bottomLoading.value = true
                delay(NEXT_PAGE_DELAY_MS)
            }
            if (!append) _booksState.value = NetworkResult.Loading()
            bookRepository.searchBooks(
                query = query,
                page = page,
                category = selectedCategory,
                availability = availabilityParam(),
                condition = selectedCondition,
                sort = selectedSort.apiValue,
                pageSize = PAGE_SIZE
            ).collect { result ->
                handleBooksResult(result, append)
            }
        }
    }

    private fun handleBooksResult(
        result: NetworkResult<com.saroj.lmsmobile.data.models.common.PaginatedResponse<Book>>,
        append: Boolean
    ) {
        when (result) {
            is NetworkResult.Loading -> Unit
            is NetworkResult.Success -> {
                currentPage = result.data.meta?.current_page ?: currentPage
                lastPage = result.data.meta?.last_page ?: currentPage
                _totalBooks.value = result.data.meta?.total ?: result.data.data.size

                if (!append) loadedBooks.clear()
                val mappedBooks = result.data.data.map { it.toStudentSearchUiModel() }
                loadedBooks.addAll(mappedBooks)
                val uniqueBooks = loadedBooks.distinctBy { it.id }
                loadedBooks.clear()
                loadedBooks.addAll(uniqueBooks)

                isLoading = false
                _bottomLoading.value = false
                publishFilteredBooks()
            }
            is NetworkResult.Error -> {
                isLoading = false
                _bottomLoading.value = false
                _booksState.value = NetworkResult.Error(result.message, result.code)
            }
            is NetworkResult.Unauthorized -> {
                isLoading = false
                _bottomLoading.value = false
                _booksState.value = NetworkResult.Unauthorized()
            }
        }
    }

    private fun updateBookState(bookId: Int, requestState: BookRequestState) {
        studentRequestStates[bookId] = requestState
        val updatedBooks = loadedBooks.map { book ->
            if (book.id == bookId) book.copy(requestState = requestState) else book
        }
        loadedBooks.clear()
        loadedBooks.addAll(updatedBooks)
        publishFilteredBooks()
    }

    private fun publishFilteredBooks() {
        _booksState.value = NetworkResult.Success(loadedBooks.sortedWith(selectedSort.comparator))
    }

    private fun applyStudentRequestStatesToLoadedBooks() {
        if (loadedBooks.isEmpty()) return
        val updatedBooks = loadedBooks.map { book ->
            val requestState = studentRequestStates[book.id]
            if (requestState != null) book.copy(requestState = requestState) else book
        }
        loadedBooks.clear()
        loadedBooks.addAll(updatedBooks)
        publishFilteredBooks()
    }

    private fun resetPagination(keepQuery: Boolean) {
        currentPage = 1
        lastPage = 1
        catalogJob?.cancel()
        isLoading = false
        _bottomLoading.value = false
        if (!keepQuery) {
            currentQuery = ""
            isSearching = false
        }
        loadedBooks.clear()
    }

    private fun reloadCurrentCatalog() {
        searchJob?.cancel()
        resetPagination(keepQuery = true)
        refreshStudentRequestStates()
        if (isSearching && currentQuery.isNotBlank()) {
            loadSearchPage(currentQuery, page = 1, append = false)
        } else {
            loadPage(page = 1, append = false)
        }
    }

    private fun availabilityParam(): String? {
        return when (availabilityFilter) {
            AvailabilityFilter.ALL -> null
            AvailabilityFilter.AVAILABLE_ONLY -> "available"
            AvailabilityFilter.UNAVAILABLE_ONLY -> "unavailable"
        }
    }

    private fun Book.toStudentSearchUiModel(): StudentSearchBookUiModel {
        val availableCopies = available_quantity ?: 0
        val totalCopies = quantity ?: 0
        return StudentSearchBookUiModel(
            id = id,
            title = title.ifBlank { "Untitled Book" },
            author = author.orFallback("Unknown author"),
            category = category.orFallback("General"),
            publisher = publisher.orFallback("Unknown publisher"),
            condition = condition.orFallback("Good"),
            accessionNo = accessionNo ?: isbn.orFallback("-"),
            location = location.orFallback("-"),
            quantity = totalCopies,
            availableQuantity = availableCopies,
            availabilityStatus = availabilityStatus.orFallback(
                if (availableCopies > 0) "available" else "unavailable"
            ),
            coverImageUrl = cover_image,
            createdAt = createdAt,
            requestState = studentRequestStates[id] ?: requestState.toBookRequestState(availableCopies)
        )
    }

    private fun String?.toBookRequestState(availableCopies: Int): BookRequestState {
        if (availableCopies <= 0) return BookRequestState.UNAVAILABLE
        return when (this?.trim()?.lowercase()) {
            "pending", "pending_request" -> BookRequestState.PENDING
            "approved" -> BookRequestState.APPROVED
            "already_issued", "issued", "borrowed" -> BookRequestState.ALREADY_ISSUED
            "rejected", "request_again" -> BookRequestState.REJECTED
            "unavailable" -> BookRequestState.UNAVAILABLE
            "loading" -> BookRequestState.LOADING
            else -> BookRequestState.NONE
        }
    }

    private fun String?.orFallback(fallback: String): String {
        return if (isNullOrBlank()) fallback else this
    }

    data class FilterState(
        val category: String?,
        val availabilityFilter: AvailabilityFilter,
        val condition: String?,
        val sortOption: SortOption
    )

    enum class AvailabilityFilter(val label: String) {
        ALL("All Availability"),
        AVAILABLE_ONLY("Available Only"),
        UNAVAILABLE_ONLY("Unavailable Only")
    }

    enum class SortOption(
        val label: String,
        val apiValue: String,
        val comparator: Comparator<StudentSearchBookUiModel>
    ) {
        TITLE_ASC("Title A-Z", "title_asc", compareBy { it.title.lowercase() }),
        TITLE_DESC("Title Z-A", "title_desc", compareByDescending { it.title.lowercase() }),
        AUTHOR_ASC("Author A-Z", "author_asc", compareBy { it.author.lowercase() }),
        AVAILABLE_DESC("Most Available", "available_desc", compareByDescending { it.availableQuantity }),
        RECENTLY_ADDED(
            "Recently Added",
            "recently_added",
            compareByDescending<StudentSearchBookUiModel> { it.createdAt.orEmpty() }
                .thenByDescending { it.id }
        )
    }
}
