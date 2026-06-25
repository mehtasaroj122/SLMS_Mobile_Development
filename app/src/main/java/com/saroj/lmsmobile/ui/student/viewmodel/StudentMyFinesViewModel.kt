package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StudentMyFinesRepository
import com.saroj.lmsmobile.ui.student.model.MyFineStatus
import com.saroj.lmsmobile.ui.student.model.MyFineUiModel
import com.saroj.lmsmobile.ui.student.model.MyFinesSummaryUiModel
import com.saroj.lmsmobile.ui.student.model.MyFinesTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StudentMyFinesViewModel(
    private val repository: StudentMyFinesRepository
) : ViewModel() {

    private val _summaryState = MutableLiveData<NetworkResult<MyFinesSummaryUiModel>>()
    val summaryState: LiveData<NetworkResult<MyFinesSummaryUiModel>> = _summaryState

    private val _finesState = MutableLiveData<NetworkResult<List<MyFineUiModel>>>()
    val finesState: LiveData<NetworkResult<List<MyFineUiModel>>> = _finesState

    private val _fineDetailState = MutableLiveData<NetworkResult<MyFineUiModel>>()
    val fineDetailState: LiveData<NetworkResult<MyFineUiModel>> = _fineDetailState

    private val tabCache = mutableMapOf<MyFinesTab, List<MyFineUiModel>>()
    private var selectedTab = MyFinesTab.ALL
    private var query = ""
    private var finesJob: Job? = null
    private var serverSummary: MyFinesSummaryUiModel? = null

    fun loadInitial() {
        loadSummary()
        switchTab(MyFinesTab.ALL, forceRefresh = true)
    }

    fun refresh() {
        loadSummary()
        switchTab(selectedTab, forceRefresh = true)
    }

    fun switchTab(tab: MyFinesTab, forceRefresh: Boolean = false) {
        selectedTab = tab
        val cachedFines = tabCache[tab]
        if (!forceRefresh && cachedFines != null) {
            publishFilteredFines(cachedFines)
            return
        }

        finesJob?.cancel()
        finesJob = viewModelScope.launch {
            val flow = when (tab) {
                MyFinesTab.ALL -> repository.getAllFines()
                MyFinesTab.PENDING -> repository.getPendingFines()
                MyFinesTab.PAID -> repository.getPaidFines()
                MyFinesTab.WAIVED -> repository.getAllFines()
            }

            flow.collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _finesState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        if (tab == MyFinesTab.ALL || tab == MyFinesTab.WAIVED) {
                            tabCache[MyFinesTab.ALL] = result.data
                        }
                        val tabFines = if (tab == MyFinesTab.WAIVED) {
                            result.data.filter { it.status == MyFineStatus.WAIVED }
                        } else {
                            result.data
                        }
                        tabCache[tab] = tabFines
                        publishSummaryFromCache()
                        publishFilteredFines(tabFines)
                    }
                    is NetworkResult.Error -> _finesState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _finesState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun setSearchQuery(value: String) {
        query = value.trim()
        publishFilteredFines()
    }

    fun resetFilters() {
        query = ""
        selectedTab = MyFinesTab.ALL
        publishFilteredFines(tabCache[MyFinesTab.ALL])
    }

    fun loadFineDetail(fine: MyFineUiModel) {
        if (fine.id < 0) {
            // Local fallback for items that don't have a backend fine ID yet (e.g. overdue books)
            _fineDetailState.value = NetworkResult.Success(fine)
            return
        }
        viewModelScope.launch {
            repository.getFineDetail(fine.id).collect { result ->
                _fineDetailState.value = result
            }
        }
    }

    fun currentTab(): MyFinesTab = selectedTab

    private fun loadSummary() {
        viewModelScope.launch {
            repository.getSummary().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        serverSummary = result.data
                        publishSummary(result.data)
                    }
                    else -> _summaryState.value = result
                }
            }
        }
    }

    private fun publishSummaryFromCache() {
        val allFines = tabCache[MyFinesTab.ALL].orEmpty()
        if (allFines.isEmpty()) return
        val summary = serverSummary
        val cacheSummary = repository.buildSummaryFromFines(allFines)
        if (summary != null && !summary.looksEmpty()) {
            publishSummary(summary)
        } else {
            _summaryState.value = NetworkResult.Success(cacheSummary)
        }
    }

    private fun publishSummary(summary: MyFinesSummaryUiModel) {
        val cacheSummary = tabCache[MyFinesTab.ALL]
            ?.takeIf { it.isNotEmpty() }
            ?.let { repository.buildSummaryFromFines(it) }
        
        val totalOutstandingAmount = cacheSummary?.outstandingAmountValue?.coerceAtLeast(summary.outstandingAmountValue)
            ?: summary.outstandingAmountValue
        val totalOutstandingStr = String.format(java.util.Locale.US, "₹%.2f", totalOutstandingAmount)

        val overdueBooks = cacheSummary?.overdueBooks?.coerceAtLeast(summary.overdueBooks)
            ?: summary.overdueBooks
        
        // Also use cache summary for paid and waived totals if they are larger (ensure consistency with the list)
        val finalPaidStr = cacheSummary?.paidAmount ?: summary.paidAmount
        val finalWaivedStr = cacheSummary?.waivedAmount ?: summary.waivedAmount
        val finalPaidVal = cacheSummary?.paidAmountValue ?: summary.paidAmountValue
        val finalWaivedVal = cacheSummary?.waivedAmountValue ?: summary.waivedAmountValue
        val finalPaidCount = cacheSummary?.paidCount ?: summary.paidCount
        val finalWaivedCount = cacheSummary?.waivedCount ?: summary.waivedCount

        _summaryState.value = NetworkResult.Success(summary.copy(
            outstandingAmount = totalOutstandingStr,
            outstandingAmountValue = totalOutstandingAmount,
            overdueBooks = overdueBooks,
            paidAmount = finalPaidStr,
            paidAmountValue = finalPaidVal,
            paidCount = finalPaidCount,
            waivedAmount = finalWaivedStr,
            waivedAmountValue = finalWaivedVal,
            waivedCount = finalWaivedCount
        ))
    }

    private fun publishFilteredFines(source: List<MyFineUiModel>? = tabCache[selectedTab]) {
        val fines = source ?: return
        val filtered = fines.filter { fine -> matchesSearch(fine) }
        _finesState.value = NetworkResult.Success(filtered)
    }

    private fun matchesSearch(fine: MyFineUiModel): Boolean {
        if (query.isBlank()) return true
        return fine.bookTitle.contains(query, ignoreCase = true) ||
            fine.author.contains(query, ignoreCase = true) ||
            fine.isbn.contains(query, ignoreCase = true) ||
            fine.publisher.contains(query, ignoreCase = true) ||
            fine.reason.contains(query, ignoreCase = true) ||
            fine.status.name.contains(query, ignoreCase = true)
    }

    private fun MyFinesSummaryUiModel.looksEmpty(): Boolean {
        return outstandingCount == 0 &&
            paidCount == 0 &&
            waivedCount == 0 &&
            overdueBooks == 0 &&
            outstandingAmountValue == 0.0 &&
            paidAmountValue == 0.0 &&
            waivedAmountValue == 0.0
    }
}
