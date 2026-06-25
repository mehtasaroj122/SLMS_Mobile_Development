package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.staffstudents.StaffStudentItem
import com.saroj.lmsmobile.data.repository.StaffStudentsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class StaffStudentsStatus(val apiValue: String, val label: String) {
    ALL("all", "All"),
    ACTIVE("active", "Active"),
    INACTIVE("inactive", "Inactive")
}

class StaffStudentsViewModel(
    private val repository: StaffStudentsRepository
) : ViewModel() {

    private val _students = MutableLiveData<List<StaffStudentItem>>(emptyList())
    val students: LiveData<List<StaffStudentItem>> = _students

    private val _selectedStatusChip = MutableLiveData(StaffStudentsStatus.ALL)
    val selectedStatusChip: LiveData<StaffStudentsStatus> = _selectedStatusChip

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var searchJob: Job? = null
    private var loadedOnce = false

    fun loadStudents() {
        loadStudentsInternal(
            status = _selectedStatusChip.value ?: StaffStudentsStatus.ALL,
            search = _searchQuery.value.orEmpty()
        )
    }

    fun loadInitial() {
        if (loadedOnce) return
        loadedOnce = true
        loadStudents()
    }

    fun searchStudents(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            loadStudentsInternal(_selectedStatusChip.value ?: StaffStudentsStatus.ALL, query.trim())
        }
    }

    fun changeStatusChip(status: StaffStudentsStatus) {
        if (_selectedStatusChip.value == status) return
        _selectedStatusChip.value = status
        loadStudentsInternal(status, _searchQuery.value.orEmpty())
    }

    fun refresh() {
        loadStudents()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun loadStudentsInternal(status: StaffStudentsStatus, search: String) {
        viewModelScope.launch {
            repository.getStudents(status.apiValue, search).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoading.value = true
                        _isSearching.value = search.isNotBlank()
                        _errorMessage.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoading.value = false
                        _isSearching.value = false
                        _students.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoading.value = false
                        _isSearching.value = false
                        _students.value = emptyList()
                        _errorMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoading.value = false
                        _isSearching.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
