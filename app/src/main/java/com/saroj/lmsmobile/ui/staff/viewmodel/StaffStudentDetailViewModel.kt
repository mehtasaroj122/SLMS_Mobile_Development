package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentBookRequestItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentFineItem
import com.saroj.lmsmobile.data.models.staffstudents.StudentFineSummary
import com.saroj.lmsmobile.data.models.staffstudents.StudentIssuedBookItem
import com.saroj.lmsmobile.data.repository.StaffStudentsRepository
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.launch

enum class StaffStudentDetailTab(val label: String) {
    ISSUED_BOOKS("Issued Books"),
    FINE_OVERVIEW("Fine Overview"),
    BOOK_REQUESTS("Book Requests")
}

class StaffStudentDetailViewModel(
    private val repository: StaffStudentsRepository
) : ViewModel() {

    private val _student = MutableLiveData<StaffStudentItem?>()
    val student: LiveData<StaffStudentItem?> = _student

    private val _issuedBooks = MutableLiveData<List<StudentIssuedBookItem>>(emptyList())
    val issuedBooks: LiveData<List<StudentIssuedBookItem>> = _issuedBooks

    private val _fineSummary = MutableLiveData(StudentFineSummary())
    val fineSummary: LiveData<StudentFineSummary> = _fineSummary

    private val _fines = MutableLiveData<List<StudentFineItem>>(emptyList())
    val fines: LiveData<List<StudentFineItem>> = _fines

    private val _bookRequests = MutableLiveData<List<StudentBookRequestItem>>(emptyList())
    val bookRequests: LiveData<List<StudentBookRequestItem>> = _bookRequests

    private val _selectedTab = MutableLiveData(StaffStudentDetailTab.ISSUED_BOOKS)
    val selectedTab: LiveData<StaffStudentDetailTab> = _selectedTab

    private val _isLoadingStudent = MutableLiveData(false)
    val isLoadingStudent: LiveData<Boolean> = _isLoadingStudent

    private val _isLoadingIssuedBooks = MutableLiveData(false)
    val isLoadingIssuedBooks: LiveData<Boolean> = _isLoadingIssuedBooks

    private val _isLoadingFines = MutableLiveData(false)
    val isLoadingFines: LiveData<Boolean> = _isLoadingFines

    private val _isLoadingBookRequests = MutableLiveData(false)
    val isLoadingBookRequests: LiveData<Boolean> = _isLoadingBookRequests

    private val _isPayingFine = MutableLiveData(false)
    val isPayingFine: LiveData<Boolean> = _isPayingFine

    private val _isWaivingFine = MutableLiveData(false)
    val isWaivingFine: LiveData<Boolean> = _isWaivingFine

    private val _isApprovingRequest = MutableLiveData(false)
    val isApprovingRequest: LiveData<Boolean> = _isApprovingRequest

    private val _isRejectingRequest = MutableLiveData(false)
    val isRejectingRequest: LiveData<Boolean> = _isRejectingRequest

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var currentStudentId: Int = 0
    private var issuedLoaded = false
    private var finesLoaded = false
    private var requestsLoaded = false
    private var studentLoaded = false

    fun loadStudentDetails(studentId: Int) {
        currentStudentId = studentId
        viewModelScope.launch {
            repository.getStudentDetails(studentId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingStudent.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingStudent.value = false
                        studentLoaded = true
                        _student.value = result.data.data?.student
                        loadIssuedBooks(studentId)
                    }
                    is NetworkResult.Error -> {
                        _isLoadingStudent.value = false
                        if (!studentLoaded) {
                            _errorMessage.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingStudent.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadIssuedBooks(studentId: Int = currentStudentId, force: Boolean = false) {
        if (studentId <= 0 || (issuedLoaded && !force)) return
        viewModelScope.launch {
            repository.getIssuedBooks(studentId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingIssuedBooks.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingIssuedBooks.value = false
                        issuedLoaded = true
                        _issuedBooks.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoadingIssuedBooks.value = false
                        if (!issuedLoaded) {
                            _errorMessage.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingIssuedBooks.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadFines(studentId: Int = currentStudentId, force: Boolean = false) {
        if (studentId <= 0 || (finesLoaded && !force)) return
        viewModelScope.launch {
            repository.getStudentFines(studentId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingFines.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingFines.value = false
                        finesLoaded = true
                        _fineSummary.value = result.data.data?.summary ?: StudentFineSummary()
                        _fines.value = result.data.data?.fines.orEmpty()
                    }
                    is NetworkResult.Error -> {
                        _isLoadingFines.value = false
                        if (!finesLoaded) {
                            _errorMessage.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingFines.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadBookRequests(studentId: Int = currentStudentId, force: Boolean = false) {
        if (studentId <= 0 || (requestsLoaded && !force)) return
        viewModelScope.launch {
            repository.getStudentBookRequests(studentId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingBookRequests.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingBookRequests.value = false
                        requestsLoaded = true
                        _bookRequests.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoadingBookRequests.value = false
                        if (!requestsLoaded) {
                            _errorMessage.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingBookRequests.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun changeTab(tab: StaffStudentDetailTab) {
        _selectedTab.value = tab
        when (tab) {
            StaffStudentDetailTab.ISSUED_BOOKS -> loadIssuedBooks()
            StaffStudentDetailTab.FINE_OVERVIEW -> loadFines()
            StaffStudentDetailTab.BOOK_REQUESTS -> loadBookRequests()
        }
    }

    fun markFinePaid(fineId: Int) {
        viewModelScope.launch {
            repository.markFinePaid(fineId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isPayingFine.value = true
                    is NetworkResult.Success -> {
                        _isPayingFine.value = false
                        _successMessage.value = result.data.message ?: "Fine marked as paid successfully"
                        loadFines(force = true)
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isPayingFine.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isPayingFine.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun waiveFine(fineId: Int, reason: String) {
        val trimmedReason = reason.trim()
        if (trimmedReason.length < 3) {
            _errorMessage.value = "Reason must be at least 3 characters."
            return
        }
        viewModelScope.launch {
            repository.waiveFine(fineId, trimmedReason).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isWaivingFine.value = true
                    is NetworkResult.Success -> {
                        _isWaivingFine.value = false
                        _successMessage.value = result.data.message ?: "Fine waived successfully"
                        loadFines(force = true)
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isWaivingFine.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isWaivingFine.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun approveBookRequest(requestId: Int) {
        viewModelScope.launch {
            repository.approveBookRequest(requestId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isApprovingRequest.value = true
                    is NetworkResult.Success -> {
                        _isApprovingRequest.value = false
                        _successMessage.value = result.data.message ?: "Request accepted successfully"
                        loadBookRequests(force = true)
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isApprovingRequest.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isApprovingRequest.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun rejectBookRequest(requestId: Int) {
        viewModelScope.launch {
            repository.rejectBookRequest(requestId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isRejectingRequest.value = true
                    is NetworkResult.Success -> {
                        _isRejectingRequest.value = false
                        _successMessage.value = result.data.message ?: "Request rejected successfully"
                        loadBookRequests(force = true)
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isRejectingRequest.value = false
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isRejectingRequest.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun refreshCurrentTab() {
        when (_selectedTab.value ?: StaffStudentDetailTab.ISSUED_BOOKS) {
            StaffStudentDetailTab.ISSUED_BOOKS -> loadIssuedBooks(force = true)
            StaffStudentDetailTab.FINE_OVERVIEW -> loadFines(force = true)
            StaffStudentDetailTab.BOOK_REQUESTS -> loadBookRequests(force = true)
        }
    }

    fun refreshAll() {
        if (currentStudentId <= 0) return
        loadStudentDetails(currentStudentId)
        refreshCurrentTab()
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
