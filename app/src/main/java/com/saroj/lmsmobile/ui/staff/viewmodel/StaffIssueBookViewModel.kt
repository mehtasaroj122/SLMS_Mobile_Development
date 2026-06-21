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

    fun searchStudents(query: String) {
        studentSearchQuery = query
        studentSearchJob?.cancel()
        if (query.trim().length < 2) {
            _studentSearchResults.value = emptyList()
            _studentSearchError.value = null
            _isSearchingStudents.value = false
            return
        }

        studentSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            repository.searchStudents(query.trim()).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isSearchingStudents.value = true
                        _studentSearchError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isSearchingStudents.value = false
                        _studentSearchResults.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isSearchingStudents.value = false
                        _studentSearchResults.value = emptyList()
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
        _studentSearchResults.value = emptyList()
        studentSearchQuery = ""
        bookSearchQuery = ""
        _bookSearchResults.value = emptyList()
        _selectedBooks.value = emptyList()
        loadStudentIssueContext(student)
    }

    fun clearSelectedStudent() {
        _selectedStudent.value = null
        _issuePrivileges.value = null
        _studentSearchResults.value = emptyList()
        _bookSearchResults.value = emptyList()
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
        val student = _selectedStudent.value
        val privileges = _issuePrivileges.value
        if (student == null || privileges?.allowed != true) {
            _bookSearchResults.value = emptyList()
            _bookSearchError.value = null
            _isSearchingBooks.value = false
            return
        }
        if (query.trim().length < 2) {
            _bookSearchResults.value = emptyList()
            _bookSearchError.value = null
            _isSearchingBooks.value = false
            return
        }

        bookSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            repository.searchBooks(query.trim(), student.id).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isSearchingBooks.value = true
                        _bookSearchError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isSearchingBooks.value = false
                        _bookSearchResults.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isSearchingBooks.value = false
                        _bookSearchResults.value = emptyList()
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
                        _bookSearchResults.value = emptyList()
                        _selectedStudent.value = null
                        _issuePrivileges.value = null
                        bookSearchQuery = ""
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
        _bookSearchResults.value = emptyList()
        bookSearchQuery = ""
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearIssueSuccess() {
        _issueSuccess.value = null
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
