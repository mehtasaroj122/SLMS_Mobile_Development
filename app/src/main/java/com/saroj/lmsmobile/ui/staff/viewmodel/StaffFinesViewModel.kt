package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.fine.FineStudentSummary
import com.saroj.lmsmobile.data.models.fine.FineSummaryData
import com.saroj.lmsmobile.data.repository.StaffFinesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StaffFinesViewModel(
    private val repository: StaffFinesRepository
) : ViewModel() {

    private val _summary = MutableLiveData<FineSummaryData?>()
    val summary: LiveData<FineSummaryData?> = _summary

    private val _students = MutableLiveData<List<FineStudentSummary>>(emptyList())
    val students: LiveData<List<FineStudentSummary>> = _students

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _isLoadingSummary = MutableLiveData(false)
    val isLoadingSummary: LiveData<Boolean> = _isLoadingSummary

    private val _isLoadingStudents = MutableLiveData(false)
    val isLoadingStudents: LiveData<Boolean> = _isLoadingStudents

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _summaryError = MutableLiveData<String?>()
    val summaryError: LiveData<String?> = _summaryError

    private val _studentsError = MutableLiveData<String?>()
    val studentsError: LiveData<String?> = _studentsError

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    private var searchJob: Job? = null
    private var loadedOnce = false
    private var summaryLoaded = false
    private var studentsLoaded = false

    fun loadInitial() {
        if (loadedOnce) return
        loadedOnce = true
        refresh()
    }

    fun loadFineSummary() {
        viewModelScope.launch {
            repository.getFineSummary().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingSummary.value = true
                        _summaryError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingSummary.value = false
                        summaryLoaded = true
                        _summary.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoadingSummary.value = false
                        if (!summaryLoaded) {
                            _summaryError.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingSummary.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun loadFineStudents(search: String? = _searchQuery.value) {
        viewModelScope.launch {
            repository.getFineStudents(search).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingStudents.value = true
                        _isSearching.value = !search.isNullOrBlank()
                        _studentsError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingStudents.value = false
                        _isSearching.value = false
                        studentsLoaded = true
                        _students.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoadingStudents.value = false
                        _isSearching.value = false
                        if (!studentsLoaded) {
                            _students.value = emptyList()
                            _studentsError.value = result.message
                        }
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingStudents.value = false
                        _isSearching.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun searchFineStudents(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            loadFineStudents(query.trim())
        }
    }

    fun refresh() {
        loadFineSummary()
        loadFineStudents(_searchQuery.value)
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
