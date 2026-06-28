package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.fine.FineRecord
import com.saroj.lmsmobile.data.models.fine.FineStudentInfo
import com.saroj.lmsmobile.data.models.fine.StudentFineSummary
import com.saroj.lmsmobile.data.repository.StaffFinesRepository
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.launch

class StaffFineDetailViewModel(
    private val repository: StaffFinesRepository
) : ViewModel() {

    private val _student = MutableLiveData<FineStudentInfo?>()
    val student: LiveData<FineStudentInfo?> = _student

    private val _summary = MutableLiveData<StudentFineSummary?>()
    val summary: LiveData<StudentFineSummary?> = _summary

    private val _fines = MutableLiveData<List<FineRecord>>(emptyList())
    val fines: LiveData<List<FineRecord>> = _fines

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPayingFine = MutableLiveData(false)
    val isPayingFine: LiveData<Boolean> = _isPayingFine

    private val _isWaivingFine = MutableLiveData(false)
    val isWaivingFine: LiveData<Boolean> = _isWaivingFine

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var currentStudentId: Int = 0
    private var detailsLoaded = false

    fun loadStudentFineDetails(studentId: Int) {
        currentStudentId = studentId
        viewModelScope.launch {
            repository.getStudentFineDetails(studentId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        detailsLoaded = true
                        val data = result.data.data
                        _student.value = data?.student
                        _summary.value = data?.summary
                        _fines.value = data?.fines.orEmpty()
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        if (!detailsLoaded) {
                            _errorMessage.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoading.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun markFinePaid(fineId: Int) {
        viewModelScope.launch {
            repository.markFinePaid(fineId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isPayingFine.value = true
                    is NetworkResult.Success -> {
                        _isPayingFine.value = false
                        _successMessage.value = result.data.message ?: "Fine marked as paid successfully."
                        refresh()
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
                        _successMessage.value = result.data.message ?: "Fine waived successfully."
                        refresh()
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

    fun refresh() {
        if (currentStudentId > 0) {
            loadStudentFineDetails(currentStudentId)
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
