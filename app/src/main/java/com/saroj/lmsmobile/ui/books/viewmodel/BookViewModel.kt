package com.saroj.lmsmobile.ui.books.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.saroj.lmsmobile.data.models.book.Book
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.BookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for listing, searching, and viewing books.
 */
class BookViewModel(private val bookRepository: BookRepository) : ViewModel() {

    private val _books = MutableLiveData<List<Book>>(emptyList())
    val books: LiveData<List<Book>> = _books

    private val _booksState = MutableLiveData<NetworkResult<List<Book>>>()
    val booksState: LiveData<NetworkResult<List<Book>>> = _booksState

    private val _bottomLoading = MutableLiveData(false)
    val bottomLoading: LiveData<Boolean> = _bottomLoading

    private val _bookDetailState = MutableLiveData<NetworkResult<Book>>()
    val bookDetailState: LiveData<NetworkResult<Book>> = _bookDetailState

    private var searchJob: Job? = null
    private var currentPage = 1
    private var lastPage = 1
    private var isLoading = false
    private var isSearching = false
    private var currentSearchQuery = ""

    fun loadBooks(refresh: Boolean = false) {
        if (isLoading) return
        if (refresh) {
            resetPagination()
        }

        val pageToLoad = if (refresh) 1 else currentPage
        loadPage(pageToLoad, append = !refresh && books.value.orEmpty().isNotEmpty())
    }

    fun loadNextPage() {
        if (isLoading || currentPage >= lastPage) return

        val nextPage = currentPage + 1
        if (isSearching) {
            searchBooks(currentSearchQuery, refresh = false)
        } else {
            loadPage(nextPage, append = true)
        }
    }

    fun searchBooks(query: String, refresh: Boolean = true) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isEmpty()) {
                resetPagination()
                isSearching = false
                currentSearchQuery = ""
                _books.value = emptyList()
                return@launch
            }

            if (isLoading) return@launch

            if (refresh) {
                delay(350)
                resetPagination()
                isSearching = true
                currentSearchQuery = trimmedQuery
            } else if (isLoading || currentPage >= lastPage) {
                return@launch
            }

            val pageToLoad = if (refresh) 1 else currentPage + 1
            loadSearchPage(trimmedQuery, pageToLoad, append = !refresh)
        }
    }

    fun resetPagination() {
        currentPage = 1
        lastPage = 1
        isLoading = false
        isSearching = false
        currentSearchQuery = ""
        _books.value = emptyList()
        _bottomLoading.value = false
    }

    fun loadAvailableBooks() {
        loadBooks(refresh = true)
    }

    private fun loadPage(page: Int, append: Boolean) {
        viewModelScope.launch {
            isLoading = true
            _bottomLoading.value = append
            if (!append) _booksState.value = NetworkResult.Loading()

            bookRepository.getBooks(page).collect { result ->
                handleBooksResult(result, append)
            }
        }
    }

    private suspend fun loadSearchPage(query: String, page: Int, append: Boolean) {
        isLoading = true
        _bottomLoading.value = append
        if (!append) _booksState.value = NetworkResult.Loading()

        bookRepository.searchBooks(query, page).collect { result ->
            handleBooksResult(result, append)
        }
    }

    fun loadBookDetail(bookId: Int) {
        viewModelScope.launch {
            bookRepository.getBookDetail(bookId).collect { result ->
                _bookDetailState.value = result
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

                val currentBooks = if (append) books.value.orEmpty() else emptyList()
                val mergedBooks = (currentBooks + result.data.data).distinctBy { it.id }
                _books.value = mergedBooks
                _booksState.value = NetworkResult.Success(mergedBooks)
                Log.d("BooksPagination", "Loaded page: $currentPage / $lastPage, total books: ${mergedBooks.size}")
                isLoading = false
                _bottomLoading.value = false
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
}
