package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.studentdashboard.StudentDashboardResponse
import com.saroj.lmsmobile.data.repository.StudentDashboardRepository
import kotlinx.coroutines.launch

class StudentDashboardViewModel(
    private val repository: StudentDashboardRepository
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
            repository.getStudentDashboard().collect { result ->
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
