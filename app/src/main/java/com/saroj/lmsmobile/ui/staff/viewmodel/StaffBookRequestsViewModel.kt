package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StaffBookRequestRepository
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestActionResult
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsSummaryUiModel
import com.saroj.lmsmobile.ui.staff.model.StaffBookRequestsTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StaffBookRequestsViewModel(
    private val repository: StaffBookRequestRepository
) : ViewModel() {

    private val _summaryState = MutableLiveData<NetworkResult<StaffBookRequestsSummaryUiModel>>()
    val summaryState: LiveData<NetworkResult<StaffBookRequestsSummaryUiModel>> = _summaryState

    private val _requestsState = MutableLiveData<NetworkResult<List<StaffBookRequestUiModel>>>()
    val requestsState: LiveData<NetworkResult<List<StaffBookRequestUiModel>>> = _requestsState

    private val _actionState = MutableLiveData<NetworkResult<StaffBookRequestActionResult>>()
    val actionState: LiveData<NetworkResult<StaffBookRequestActionResult>> = _actionState

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _actionRequestId = MutableLiveData<Int?>(null)
    val actionRequestId: LiveData<Int?> = _actionRequestId

    private var selectedTab = StaffBookRequestsTab.ALL
    private var query = ""
    private var searchJob: Job? = null
    private var requestsJob: Job? = null
    private var loadMoreJob: Job? = null
    private val loadedRequests = mutableListOf<StaffBookRequestUiModel>()
    private var currentPage = 1
    private var lastPage = 1

    fun loadInitial() {
        loadSummary()
        loadRequests()
    }

    fun refresh() {
        loadSummary()
        loadRequests()
    }

    fun searchRequests(value: String) {
        query = value.trim()
        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(SEARCH_DEBOUNCE_MS)
            loadRequests(page = 1, append = false)
        }
    }

    fun changeStatusChip(tab: StaffBookRequestsTab) {
        if (selectedTab == tab) return
        selectedTab = tab
        loadRequests(page = 1, append = false)
    }

    fun loadNextPage() {
        if (_isLoadingMore.value == true ||
            _isSearching.value == true ||
            currentPage >= lastPage
        ) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _isLoadingMore.value = true
            delay(LOAD_MORE_DELAY_MS)
            repository.getRequests(
                status = selectedTab.apiValue,
                search = query,
                page = currentPage + 1
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> Unit
                    is NetworkResult.Success -> {
                        currentPage = result.data.currentPage
                        lastPage = result.data.lastPage
                        val knownIds = loadedRequests.map { it.id }.toMutableSet()
                        result.data.requests.forEach { request ->
                            if (knownIds.add(request.id)) loadedRequests.add(request)
                        }
                        _requestsState.value = NetworkResult.Success(loadedRequests.toList())
                        _isLoadingMore.value = false
                    }
                    is NetworkResult.Error -> {
                        _requestsState.value = NetworkResult.Error(result.message, result.code)
                        _isLoadingMore.value = false
                    }
                    is NetworkResult.Unauthorized -> {
                        _requestsState.value = NetworkResult.Unauthorized()
                        _isLoadingMore.value = false
                    }
                }
            }
        }
    }

    fun approveRequest(request: StaffBookRequestUiModel) {
        _actionRequestId.value = request.id
        viewModelScope.launch {
            repository.approveRequest(request).collect { result ->
                handleActionResult(result)
            }
        }
    }

    fun rejectRequest(request: StaffBookRequestUiModel) {
        _actionRequestId.value = request.id
        viewModelScope.launch {
            repository.rejectRequest(request).collect { result ->
                handleActionResult(result)
            }
        }
    }

    fun selectedTab(): StaffBookRequestsTab = selectedTab

    fun currentQuery(): String = query

    fun clearActionMessage() {
        _actionState.value = null
    }

    private fun loadSummary() {
        viewModelScope.launch {
            repository.getSummary().collect { result ->
                _summaryState.value = result
            }
        }
    }

    private fun loadRequests(page: Int = 1, append: Boolean = false) {
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            repository.getRequests(selectedTab.apiValue, query, page = page).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        if (!append) _requestsState.value = NetworkResult.Loading()
                    }
                    is NetworkResult.Success -> {
                        _isSearching.value = false
                        currentPage = result.data.currentPage
                        lastPage = result.data.lastPage
                        if (!append) loadedRequests.clear()
                        val knownIds = loadedRequests.map { it.id }.toMutableSet()
                        result.data.requests.forEach { request ->
                            if (knownIds.add(request.id)) loadedRequests.add(request)
                        }
                        _requestsState.value = NetworkResult.Success(loadedRequests.toList())
                    }
                    is NetworkResult.Error -> {
                        _isSearching.value = false
                        if (!append) loadedRequests.clear()
                        _requestsState.value = NetworkResult.Error(result.message, result.code)
                    }
                    is NetworkResult.Unauthorized -> {
                        _isSearching.value = false
                        _requestsState.value = NetworkResult.Unauthorized()
                    }
                }
            }
        }
    }

    private fun handleActionResult(result: NetworkResult<StaffBookRequestActionResult>) {
        when (result) {
            is NetworkResult.Loading -> {
                _actionState.value = result
            }
            is NetworkResult.Success -> {
                _actionRequestId.value = null
                _actionState.value = result
                refresh()
            }
            is NetworkResult.Error -> {
                _actionRequestId.value = null
                _actionState.value = result
            }
            is NetworkResult.Unauthorized -> {
                _actionRequestId.value = null
                _actionState.value = result
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 500L
        const val LOAD_MORE_DELAY_MS = 1_000L
    }
}
