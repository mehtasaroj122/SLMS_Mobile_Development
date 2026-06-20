package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.staffdashboard.StaffDashboardData
import com.saroj.lmsmobile.data.repository.StaffDashboardRepository
import kotlinx.coroutines.launch

class StaffDashboardViewModel(
    private val repository: StaffDashboardRepository
) : ViewModel() {

    private val _dashboardData = MutableLiveData<StaffDashboardData>()
    val dashboardData: LiveData<StaffDashboardData> = _dashboardData

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

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

    fun approveRequest(requestId: Int) {
        viewModelScope.launch {
            repository.approveRequest(requestId).collect { result ->
                handleActionResult(result)
            }
        }
    }

    fun rejectRequest(requestId: Int) {
        viewModelScope.launch {
            repository.rejectRequest(requestId).collect { result ->
                handleActionResult(result)
            }
        }
    }

    private fun fetchDashboard() {
        viewModelScope.launch {
            repository.getStaffDashboard().collect { result ->
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

    private fun handleActionResult(result: NetworkResult<String>) {
        when (result) {
            is NetworkResult.Loading -> {
                _isLoading.value = true
                _errorMessage.value = null
            }
            is NetworkResult.Success -> {
                _actionMessage.value = result.data
                fetchDashboard()
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

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
