package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.studentdashboard.DashboardStats
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardResponse
import com.saroj.lmsmobile.data.repository.StudentDashboardRepository
import com.saroj.lmsmobile.data.repository.StudentMyFinesRepository
import com.saroj.lmsmobile.ui.student.model.MyFineStatus
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class StudentDashboardViewModel(
    private val repository: StudentDashboardRepository,
    private val finesRepository: StudentMyFinesRepository
) : ViewModel() {

    private val _dashboardData = MutableLiveData<StudentDashboardResponse>()
    val dashboardData: LiveData<StudentDashboardResponse> = _dashboardData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var hasLoaded = false

    fun loadDashboard() {
        if (hasLoaded) return
        hasLoaded = true
        fetchDashboard()
    }

    fun refreshDashboard() {
        fetchDashboard()
    }

    private fun fetchDashboard() {
        viewModelScope.launch {
            repository.getStudentDashboard()
                .combine(finesRepository.getAllFines()) { dashboardResult, finesResult ->
                    if (dashboardResult is NetworkResult.Success && finesResult is NetworkResult.Success) {
                        val calculatedPendingFine = finesResult.data
                            .filter { it.status == MyFineStatus.PENDING }
                            .sumOf { it.amountValue }

                        // Take the larger of the two (calculated vs server) to match the Fine page logic
                        val serverPendingFine = dashboardResult.data.stats?.pending_fines
                            ?: dashboardResult.data.data?.stats?.pending_fines
                            ?: 0.0
                        val finalPendingFine = maxOf(calculatedPendingFine, serverPendingFine)

                        val updatedStats = dashboardResult.data.stats?.copy(pending_fines = finalPendingFine)
                            ?: DashboardStats(
                                issued_books = null,
                                returned_books = null,
                                pending_fines = finalPendingFine,
                                active_requests = null,
                                total_requests = null,
                                approved_requests = null,
                                rejected_requests = null
                            )

                        val updatedData = dashboardResult.data.data?.let { d ->
                            d.copy(stats = d.stats?.copy(pending_fines = finalPendingFine) ?: updatedStats)
                        }

                        NetworkResult.Success(
                            dashboardResult.data.copy(
                                stats = updatedStats,
                                data = updatedData
                            )
                        )
                    } else {
                        dashboardResult
                    }
                }.collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _isLoading.value = true
                            _errorMessage.value = null
                        }
                        is NetworkResult.Success -> {
                            _isLoading.value = false
                            _dashboardData.value = result.data
                        }
                        is NetworkResult.Error -> {
                            _isLoading.value = false
                            _errorMessage.value = result.message
                        }
                        is NetworkResult.Unauthorized -> {
                            _isLoading.value = false
                            _unauthorized.value = true
                        }
                    }
                }
        }
    }
}
