package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StudentMyRequestsRepository
import com.saroj.lmsmobile.ui.student.model.MyRequestActionResult
import com.saroj.lmsmobile.ui.student.model.MyRequestStatus
import com.saroj.lmsmobile.ui.student.model.MyRequestUiModel
import com.saroj.lmsmobile.ui.student.model.MyRequestsSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyRequestsTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StudentMyRequestsViewModel(
    private val repository: StudentMyRequestsRepository
) : ViewModel() {

    private val _summaryState = MutableLiveData<NetworkResult<MyRequestsSummaryUiModel>>()
    val summaryState: LiveData<NetworkResult<MyRequestsSummaryUiModel>> = _summaryState

    private val _requestsState = MutableLiveData<NetworkResult<List<MyRequestUiModel>>>()
    val requestsState: LiveData<NetworkResult<List<MyRequestUiModel>>> = _requestsState

    private val _requestDetailState = MutableLiveData<NetworkResult<MyRequestUiModel>>()
    val requestDetailState: LiveData<NetworkResult<MyRequestUiModel>> = _requestDetailState

    private val _cancelState = MutableLiveData<NetworkResult<MyRequestActionResult>>()
    val cancelState: LiveData<NetworkResult<MyRequestActionResult>> = _cancelState

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private var selectedTab = MyRequestsTab.ALL
    private var query = ""
    private var requestsCache: List<MyRequestUiModel> = emptyList()
    private var filteredRequestsCache: List<MyRequestUiModel> = emptyList()
    private var visibleLimit = PAGE_SIZE
    private var loadingMoreJob: Job? = null
    private var requestsJob: Job? = null
    private var serverSummary: MyRequestsSummaryUiModel? = null

    fun loadInitial() {
        loadSummary()
        loadRequests(forceRefresh = true)
    }

    fun refresh() {
        visibleLimit = PAGE_SIZE
        loadSummary()
        loadRequests(forceRefresh = true)
    }

    fun switchTab(tab: MyRequestsTab) {
        selectedTab = tab
        visibleLimit = PAGE_SIZE
        publishFilteredRequests()
    }

    fun setSearchQuery(value: String) {
        query = value.trim()
        visibleLimit = PAGE_SIZE
        publishFilteredRequests()
    }

    fun resetFilters() {
        query = ""
        selectedTab = MyRequestsTab.ALL
        visibleLimit = PAGE_SIZE
        publishFilteredRequests()
    }

    fun loadNextPage() {
        if (_isLoadingMore.value == true || !hasMoreRequests()) return
        loadingMoreJob?.cancel()
        loadingMoreJob = viewModelScope.launch {
            _isLoadingMore.value = true
            delay(1_000L)
            visibleLimit += PAGE_SIZE
            publishFilteredRequests()
            _isLoadingMore.value = false
        }
    }

    fun hasMoreRequests(): Boolean {
        return visibleLimit < filteredRequestsCache.size
    }

    fun filteredTotalCount(): Int = filteredRequestsCache.size

    fun loadRequestDetail(request: MyRequestUiModel) {
        viewModelScope.launch {
            repository.getRequestDetail(request.id).collect { result ->
                _requestDetailState.value = result
            }
        }
    }

    fun cancelRequest(request: MyRequestUiModel) {
        viewModelScope.launch {
            repository.cancelRequest(request).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val updatedRequest = result.data.request
                        if (updatedRequest != null) {
                            requestsCache = requestsCache.map {
                                if (it.id == updatedRequest.id) updatedRequest else it
                            }
                            publishSummaryFromCache()
                            publishFilteredRequests()
                        }
                        _cancelState.value = result
                        loadSummary()
                        loadRequests(forceRefresh = true)
                    }
                    is NetworkResult.Loading -> _cancelState.value = NetworkResult.Loading()
                    is NetworkResult.Error -> _cancelState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _cancelState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun currentTab(): MyRequestsTab = selectedTab

    private fun loadSummary() {
        viewModelScope.launch {
            repository.getSummary().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        serverSummary = result.data
                        if (result.data.looksEmpty() && requestsCache.isNotEmpty()) {
                            publishSummaryFromCache()
                        } else {
                            _summaryState.value = result
                        }
                    }
                    else -> _summaryState.value = result
                }
            }
        }
    }

    private fun loadRequests(forceRefresh: Boolean) {
        if (!forceRefresh && requestsCache.isNotEmpty()) {
            publishFilteredRequests()
            return
        }

        visibleLimit = PAGE_SIZE
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            repository.getRequests().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _requestsState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        requestsCache = result.data
                        publishSummaryFromCache()
                        publishFilteredRequests()
                    }
                    is NetworkResult.Error -> _requestsState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _requestsState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    private fun publishSummaryFromCache() {
        val cacheSummary = repository.buildSummaryFromRequests(requestsCache)
        val summary = serverSummary
        _summaryState.value = NetworkResult.Success(
            if (summary != null && !summary.looksEmpty()) summary else cacheSummary
        )
    }

    private fun publishFilteredRequests() {
        filteredRequestsCache = requestsCache
            .filter { request -> matchesTab(request) }
            .filter { request -> matchesSearch(request) }

        _requestsState.value = NetworkResult.Success(filteredRequestsCache.take(visibleLimit))
    }

    private fun matchesTab(request: MyRequestUiModel): Boolean {
        return when (selectedTab) {
            MyRequestsTab.ALL -> true
            MyRequestsTab.PENDING -> request.status == MyRequestStatus.PENDING
            MyRequestsTab.APPROVED -> request.status == MyRequestStatus.APPROVED
            MyRequestsTab.REJECTED -> request.status == MyRequestStatus.REJECTED
        }
    }

    private fun matchesSearch(request: MyRequestUiModel): Boolean {
        if (query.isBlank()) return true
        return request.bookTitle.contains(query, ignoreCase = true) ||
            request.author.contains(query, ignoreCase = true) ||
            request.status.name.contains(query, ignoreCase = true) ||
            request.processedBy.contains(query, ignoreCase = true) ||
            request.message.contains(query, ignoreCase = true)
    }

    private fun MyRequestsSummaryUiModel.looksEmpty(): Boolean {
        return totalRequests == 0 && pendingRequests == 0 && approvedRequests == 0 && rejectedRequests == 0
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
