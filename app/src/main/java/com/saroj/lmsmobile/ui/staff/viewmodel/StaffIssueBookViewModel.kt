package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.issue.IssueBookItem
import com.saroj.lmsmobile.data.models.issue.IssueBooksResponse
import com.saroj.lmsmobile.data.models.issue.IssuePrivilegesData
import com.saroj.lmsmobile.data.models.issue.IssueStudent
import com.saroj.lmsmobile.data.repository.StaffIssueBookRepository
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StaffIssueBookViewModel(
    private val repository: StaffIssueBookRepository
) : ViewModel() {

    private val _studentSearchResults = MutableLiveData<List<IssueStudent>>(emptyList())
    val studentSearchResults: LiveData<List<IssueStudent>> = _studentSearchResults

    private val _selectedStudent = MutableLiveData<IssueStudent?>()
    val selectedStudent: LiveData<IssueStudent?> = _selectedStudent

    private val _issuePrivileges = MutableLiveData<IssuePrivilegesData?>()
    val issuePrivileges: LiveData<IssuePrivilegesData?> = _issuePrivileges

    private val _bookSearchResults = MutableLiveData<List<IssueBookItem>>(emptyList())
    val bookSearchResults: LiveData<List<IssueBookItem>> = _bookSearchResults

    private val _selectedBooks = MutableLiveData<List<IssueBookItem>>(emptyList())
    val selectedBooks: LiveData<List<IssueBookItem>> = _selectedBooks

    private val _isSearchingStudents = MutableLiveData(false)
    val isSearchingStudents: LiveData<Boolean> = _isSearchingStudents

    private val _isSearchingBooks = MutableLiveData(false)
    val isSearchingBooks: LiveData<Boolean> = _isSearchingBooks

    private val _isLoadingMoreStudents = MutableLiveData(false)
    val isLoadingMoreStudents: LiveData<Boolean> = _isLoadingMoreStudents

    private val _isLoadingMoreBooks = MutableLiveData(false)
    val isLoadingMoreBooks: LiveData<Boolean> = _isLoadingMoreBooks

    private val _isLoadingPrivileges = MutableLiveData(false)
    val isLoadingPrivileges: LiveData<Boolean> = _isLoadingPrivileges

    private val _isIssuing = MutableLiveData(false)
    val isIssuing: LiveData<Boolean> = _isIssuing

    private val _studentSearchError = MutableLiveData<String?>()
    val studentSearchError: LiveData<String?> = _studentSearchError

    private val _bookSearchError = MutableLiveData<String?>()
    val bookSearchError: LiveData<String?> = _bookSearchError

    private val _privilegeError = MutableLiveData<String?>()
    val privilegeError: LiveData<String?> = _privilegeError

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    private val _issueSuccess = MutableLiveData<IssueBooksResponse?>()
    val issueSuccess: LiveData<IssueBooksResponse?> = _issueSuccess

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    var studentSearchQuery: String = ""
        private set
    var bookSearchQuery: String = ""
        private set

    private var studentSearchJob: Job? = null
    private var bookSearchJob: Job? = null
    private var studentLoadMoreJob: Job? = null
    private var bookLoadMoreJob: Job? = null
    private val loadedStudents = mutableListOf<IssueStudent>()
    private val loadedBooks = mutableListOf<IssueBookItem>()
    private var studentPage = 1
    private var studentLastPage = 1
    private var bookPage = 1
    private var bookLastPage = 1

    fun searchStudents(query: String) {
        studentSearchQuery = query
        studentSearchJob?.cancel()
        studentLoadMoreJob?.cancel()
        if (query.trim().length < 2) {
            resetStudentPaging()
            _studentSearchError.value = null
            _isSearchingStudents.value = false
            _isLoadingMoreStudents.value = false
            return
        }

        studentSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            studentPage = 1
            studentLastPage = 1
            loadedStudents.clear()
            repository.searchStudents(query.trim(), page = studentPage, pageSize = PAGE_SIZE).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isSearchingStudents.value = true
                        _studentSearchError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isSearchingStudents.value = false
                        studentPage = result.data.meta?.currentPage ?: 1
                        studentLastPage = result.data.meta?.lastPage ?: 1
                        loadedStudents.clear()
                        loadedStudents.addAll(result.data.data)
                        _studentSearchResults.value = loadedStudents.toList()
                    }
                    is NetworkResult.Error -> {
                        _isSearchingStudents.value = false
                        resetStudentPaging()
                        _studentSearchError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isSearchingStudents.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun selectStudent(student: IssueStudent) {
        _selectedStudent.value = student
        resetStudentPaging()
        studentSearchQuery = ""
        bookSearchQuery = ""
        resetBookPaging()
        _selectedBooks.value = emptyList()
        loadStudentIssueContext(student)
    }

    fun clearSelectedStudent() {
        _selectedStudent.value = null
        _issuePrivileges.value = null
        resetStudentPaging()
        resetBookPaging()
        _selectedBooks.value = emptyList()
        _privilegeError.value = null
        bookSearchQuery = ""
    }

    fun loadStudentIssueContext(student: IssueStudent) {
        viewModelScope.launch {
            repository.getStudentIssueContext(student).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingPrivileges.value = true
                        _privilegeError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingPrivileges.value = false
                        _selectedStudent.value = result.data.student
                        _issuePrivileges.value = result.data.privileges
                    }
                    is NetworkResult.Error -> {
                        _isLoadingPrivileges.value = false
                        _issuePrivileges.value = null
                        _privilegeError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingPrivileges.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun searchBooks(query: String) {
        bookSearchQuery = query
        bookSearchJob?.cancel()
        bookLoadMoreJob?.cancel()
        val student = _selectedStudent.value
        val privileges = _issuePrivileges.value
        if (student == null || privileges?.allowed != true) {
            resetBookPaging()
            _bookSearchError.value = null
            _isSearchingBooks.value = false
            _isLoadingMoreBooks.value = false
            return
        }
        if (query.trim().length < 2) {
            resetBookPaging()
            _bookSearchError.value = null
            _isSearchingBooks.value = false
            _isLoadingMoreBooks.value = false
            return
        }

        bookSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            bookPage = 1
            bookLastPage = 1
            loadedBooks.clear()
            repository.searchBooks(query.trim(), student.id, page = bookPage, pageSize = PAGE_SIZE).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isSearchingBooks.value = true
                        _bookSearchError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isSearchingBooks.value = false
                        bookPage = result.data.meta?.currentPage ?: 1
                        bookLastPage = result.data.meta?.lastPage ?: 1
                        loadedBooks.clear()
                        loadedBooks.addAll(result.data.data)
                        _bookSearchResults.value = loadedBooks.toList()
                    }
                    is NetworkResult.Error -> {
                        _isSearchingBooks.value = false
                        resetBookPaging()
                        _bookSearchError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isSearchingBooks.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadMoreStudents() {
        val query = studentSearchQuery.trim()
        if (query.length < 2 ||
            _isSearchingStudents.value == true ||
            _isLoadingMoreStudents.value == true ||
            studentPage >= studentLastPage
        ) {
            return
        }

        studentLoadMoreJob?.cancel()
        studentLoadMoreJob = viewModelScope.launch {
            _isLoadingMoreStudents.value = true
            delay(LOAD_MORE_DELAY_MS)
            repository.searchStudents(query, page = studentPage + 1, pageSize = PAGE_SIZE).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> Unit
                    is NetworkResult.Success -> {
                        studentPage = result.data.meta?.currentPage ?: (studentPage + 1)
                        studentLastPage = result.data.meta?.lastPage ?: studentPage
                        val knownIds = loadedStudents.map { it.id }.toMutableSet()
                        result.data.data.forEach { student ->
                            if (knownIds.add(student.id)) loadedStudents.add(student)
                        }
                        _studentSearchResults.value = loadedStudents.toList()
                        _studentSearchError.value = null
                        _isLoadingMoreStudents.value = false
                    }
                    is NetworkResult.Error -> {
                        _studentSearchError.value = result.message
                        _isLoadingMoreStudents.value = false
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingMoreStudents.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadMoreBooks() {
        val student = _selectedStudent.value ?: return
        val query = bookSearchQuery.trim()
        if (query.length < 2 ||
            _isSearchingBooks.value == true ||
            _isLoadingMoreBooks.value == true ||
            bookPage >= bookLastPage
        ) {
            return
        }

        bookLoadMoreJob?.cancel()
        bookLoadMoreJob = viewModelScope.launch {
            _isLoadingMoreBooks.value = true
            delay(LOAD_MORE_DELAY_MS)
            repository.searchBooks(query, student.id, page = bookPage + 1, pageSize = PAGE_SIZE).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> Unit
                    is NetworkResult.Success -> {
                        bookPage = result.data.meta?.currentPage ?: (bookPage + 1)
                        bookLastPage = result.data.meta?.lastPage ?: bookPage
                        val knownIds = loadedBooks.map { it.requestId }.toMutableSet()
                        result.data.data.forEach { book ->
                            if (knownIds.add(book.requestId)) loadedBooks.add(book)
                        }
                        _bookSearchResults.value = loadedBooks.toList()
                        _bookSearchError.value = null
                        _isLoadingMoreBooks.value = false
                    }
                    is NetworkResult.Error -> {
                        _bookSearchError.value = result.message
                        _isLoadingMoreBooks.value = false
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingMoreBooks.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun selectBook(book: IssueBookItem) {
        val privileges = _issuePrivileges.value
        val current = _selectedBooks.value.orEmpty()
        val limit = privileges?.slotLimit ?: 0

        when {
            _selectedStudent.value == null -> _actionMessage.value = "Select a student first."
            privileges?.allowed != true -> _actionMessage.value = privileges?.reason ?: "This student is not allowed to issue books."
            !book.canSelect -> _actionMessage.value = "This book is not available for issue."
            current.any { it.requestId == book.requestId } -> _actionMessage.value = "Book already selected."
            current.size >= limit -> _actionMessage.value = "Book issue limit reached."
            else -> _selectedBooks.value = current + book
        }
    }

    fun removeSelectedBook(book: IssueBookItem) {
        _selectedBooks.value = _selectedBooks.value.orEmpty().filterNot { it.requestId == book.requestId }
    }

    fun issueSelectedBooks() {
        val student = _selectedStudent.value
        val privileges = _issuePrivileges.value
        val books = _selectedBooks.value.orEmpty()
        val limit = privileges?.slotLimit ?: 0

        when {
            student == null -> {
                _actionMessage.value = "Select a student first."
                return
            }
            privileges?.allowed != true -> {
                _actionMessage.value = privileges?.reason ?: "This student is not allowed to issue books."
                return
            }
            books.isEmpty() -> {
                _actionMessage.value = "Select at least one book."
                return
            }
            books.size > limit -> {
                _actionMessage.value = "Selected books exceed the allowed limit."
                return
            }
            books.distinctBy { it.requestId }.size != books.size -> {
                _actionMessage.value = "Duplicate books are not allowed."
                return
            }
            books.any { !it.canSelect } -> {
                _actionMessage.value = "One or more books are unavailable."
                return
            }
        }

        viewModelScope.launch {
            repository.issueBooks(student.id, books.map { it.requestId }).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isIssuing.value = true
                    is NetworkResult.Success -> {
                        _isIssuing.value = false
                        _issueSuccess.value = result.data
                        _selectedBooks.value = emptyList()
                        resetBookPaging()
                        _selectedStudent.value = null
                        _issuePrivileges.value = null
                        bookSearchQuery = ""
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isIssuing.value = false
                        _actionMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isIssuing.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun refreshStudentPrivileges() {
        _selectedStudent.value?.let(::loadStudentIssueContext)
    }

    fun refreshPage(studentQuery: String, bookQuery: String) {
        val student = _selectedStudent.value
        if (student != null) {
            loadStudentIssueContext(student)
            if (bookQuery.trim().length >= 2) {
                searchBooks(bookQuery)
            }
        } else if (studentQuery.trim().length >= 2) {
            searchStudents(studentQuery)
        } else {
            _isSearchingStudents.value = false
            _isSearchingBooks.value = false
            _isLoadingPrivileges.value = false
        }
    }

    fun clearIssueForm() {
        _selectedBooks.value = emptyList()
        resetBookPaging()
        bookSearchQuery = ""
    }

    private fun resetStudentPaging() {
        studentPage = 1
        studentLastPage = 1
        loadedStudents.clear()
        _studentSearchResults.value = emptyList()
    }

    private fun resetBookPaging() {
        bookPage = 1
        bookLastPage = 1
        loadedBooks.clear()
        _bookSearchResults.value = emptyList()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearIssueSuccess() {
        _issueSuccess.value = null
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
        const val LOAD_MORE_DELAY_MS = 1_000L
        const val PAGE_SIZE = 20
    }
}
