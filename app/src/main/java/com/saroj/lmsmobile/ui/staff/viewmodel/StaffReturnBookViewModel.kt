package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.models.returnbook.ActiveIssueItem
import com.saroj.lmsmobile.data.models.returnbook.ProcessReturnResponse
import com.saroj.lmsmobile.data.models.returnbook.ReturnPreviewData
import com.saroj.lmsmobile.data.models.returnbook.ReturnRulesData
import com.saroj.lmsmobile.data.models.returnbook.ReturnStudent
import com.saroj.lmsmobile.data.repository.StaffReturnBookRepository
import com.saroj.lmsmobile.utils.NotificationRefreshBus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StaffReturnBookViewModel(
    private val repository: StaffReturnBookRepository
) : ViewModel() {

    private val _returnSettings = MutableLiveData<ReturnRulesData?>()
    val returnSettings: LiveData<ReturnRulesData?> = _returnSettings

    private val _returnRules = MutableLiveData<ReturnRulesData?>()
    val returnRules: LiveData<ReturnRulesData?> = _returnRules

    private val _studentSearchResults = MutableLiveData<List<ReturnStudent>>(emptyList())
    val studentSearchResults: LiveData<List<ReturnStudent>> = _studentSearchResults

    private val _selectedStudent = MutableLiveData<ReturnStudent?>()
    val selectedStudent: LiveData<ReturnStudent?> = _selectedStudent

    private val _activeIssues = MutableLiveData<List<ActiveIssueItem>>(emptyList())
    val activeIssues: LiveData<List<ActiveIssueItem>> = _activeIssues

    private val _selectedIssueIds = MutableLiveData<Set<Int>>(emptySet())
    val selectedIssueIds: LiveData<Set<Int>> = _selectedIssueIds

    private val _selectedCondition = MutableLiveData<String?>()
    val selectedCondition: LiveData<String?> = _selectedCondition

    private val _finePreview = MutableLiveData<ReturnPreviewData?>()
    val finePreview: LiveData<ReturnPreviewData?> = _finePreview

    private val _isLoadingReturnSettings = MutableLiveData(false)
    val isLoadingReturnSettings: LiveData<Boolean> = _isLoadingReturnSettings

    private val _isSearchingStudents = MutableLiveData(false)
    val isSearchingStudents: LiveData<Boolean> = _isSearchingStudents

    private val _isLoadingStudentReturnData = MutableLiveData(false)
    val isLoadingStudentReturnData: LiveData<Boolean> = _isLoadingStudentReturnData

    private val _isPreviewingFine = MutableLiveData(false)
    val isPreviewingFine: LiveData<Boolean> = _isPreviewingFine

    private val _isProcessingReturn = MutableLiveData(false)
    val isProcessingReturn: LiveData<Boolean> = _isProcessingReturn

    private val _settingsError = MutableLiveData<String?>()
    val settingsError: LiveData<String?> = _settingsError

    private val _studentSearchError = MutableLiveData<String?>()
    val studentSearchError: LiveData<String?> = _studentSearchError

    private val _studentReturnDataError = MutableLiveData<String?>()
    val studentReturnDataError: LiveData<String?> = _studentReturnDataError

    private val _previewError = MutableLiveData<String?>()
    val previewError: LiveData<String?> = _previewError

    private val _actionMessage = MutableLiveData<String?>()
    val actionMessage: LiveData<String?> = _actionMessage

    private val _returnSuccess = MutableLiveData<ProcessReturnResponse?>()
    val returnSuccess: LiveData<ProcessReturnResponse?> = _returnSuccess

    private val _unauthorized = MutableLiveData(false)
    val unauthorized: LiveData<Boolean> = _unauthorized

    var studentSearchQuery: String = ""
        private set

    private var studentSearchJob: Job? = null
    private var previewJob: Job? = null

    init {
        loadReturnSettings()
    }

    fun loadReturnSettings() {
        viewModelScope.launch {
            repository.getReturnSettings().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingReturnSettings.value = true
                        _settingsError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingReturnSettings.value = false
                        _returnSettings.value = result.data.data
                        if (_selectedStudent.value == null) _returnRules.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isLoadingReturnSettings.value = false
                        _settingsError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingReturnSettings.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun searchStudents(query: String) {
        studentSearchQuery = query
        studentSearchJob?.cancel()
        if (query.trim().length < 2) {
            _studentSearchResults.value = emptyList()
            _studentSearchError.value = null
            _isSearchingStudents.value = false
            return
        }

        studentSearchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            repository.searchStudents(query.trim()).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isSearchingStudents.value = true
                        _studentSearchError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isSearchingStudents.value = false
                        _studentSearchResults.value = result.data.data
                    }
                    is NetworkResult.Error -> {
                        _isSearchingStudents.value = false
                        _studentSearchResults.value = emptyList()
                        _studentSearchError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isSearchingStudents.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun selectStudent(student: ReturnStudent) {
        _selectedStudent.value = student
        _studentSearchResults.value = emptyList()
        studentSearchQuery = ""
        clearReturnSelections()
        loadStudentReturnData(student)
    }

    fun clearSelectedStudent() {
        _selectedStudent.value = null
        _activeIssues.value = emptyList()
        _returnRules.value = _returnSettings.value
        _studentSearchResults.value = emptyList()
        _studentReturnDataError.value = null
        clearReturnSelections()
    }

    fun loadStudentReturnData(student: ReturnStudent? = _selectedStudent.value) {
        student ?: return
        viewModelScope.launch {
            repository.getStudentReturnData(student).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isLoadingStudentReturnData.value = true
                        _studentReturnDataError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isLoadingStudentReturnData.value = false
                        val data = result.data.data
                        _selectedStudent.value = data?.student ?: student
                        _activeIssues.value = data?.activeIssues.orEmpty()
                        _returnRules.value = data?.returnRules ?: _returnSettings.value
                        _selectedIssueIds.value = _selectedIssueIds.value.orEmpty()
                            .filter { id -> data?.activeIssues.orEmpty().any { it.issueId == id } }
                            .toSet()
                        refreshPreviewIfReady()
                    }
                    is NetworkResult.Error -> {
                        _isLoadingStudentReturnData.value = false
                        _activeIssues.value = emptyList()
                        _studentReturnDataError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isLoadingStudentReturnData.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun toggleIssueSelection(issueId: Int) {
        val current = _selectedIssueIds.value.orEmpty()
        _selectedIssueIds.value = if (current.contains(issueId)) current - issueId else current + issueId
        _finePreview.value = null
        _previewError.value = null
        refreshPreviewIfReady()
    }

    fun selectAllIssues() {
        val allIds = _activeIssues.value.orEmpty().map { it.issueId }.toSet()
        _selectedIssueIds.value = if (_selectedIssueIds.value.orEmpty().size == allIds.size) emptySet() else allIds
        _finePreview.value = null
        _previewError.value = null
        refreshPreviewIfReady()
    }

    fun clearSelectedIssues() {
        _selectedIssueIds.value = emptySet()
        _selectedCondition.value = null
        _finePreview.value = null
        _previewError.value = null
    }

    fun selectCondition(condition: String) {
        _selectedCondition.value = condition
        _finePreview.value = null
        _previewError.value = null
        previewFine()
    }

    fun previewFine() {
        val student = _selectedStudent.value
        val ids = _selectedIssueIds.value.orEmpty().toList()
        val condition = _selectedCondition.value
        when {
            student == null -> {
                _actionMessage.value = "Select a student first."
                return
            }
            ids.isEmpty() -> {
                _actionMessage.value = "Select at least one book."
                return
            }
            condition.isNullOrBlank() -> {
                _actionMessage.value = "Select a return condition."
                return
            }
        }
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            repository.previewFine(ids, condition).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isPreviewingFine.value = true
                        _previewError.value = null
                    }
                    is NetworkResult.Success -> {
                        _isPreviewingFine.value = false
                        _finePreview.value = result.data.data
                        result.data.data?.rules?.let { _returnRules.value = it }
                    }
                    is NetworkResult.Error -> {
                        _isPreviewingFine.value = false
                        _finePreview.value = null
                        _previewError.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isPreviewingFine.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun processReturn() {
        val student = _selectedStudent.value
        val ids = _selectedIssueIds.value.orEmpty().toList()
        val condition = _selectedCondition.value
        when {
            student == null -> {
                _actionMessage.value = "Select a student first."
                return
            }
            ids.isEmpty() -> {
                _actionMessage.value = "Select at least one book."
                return
            }
            condition.isNullOrBlank() -> {
                _actionMessage.value = "Select a return condition."
                return
            }
            _finePreview.value == null -> {
                _actionMessage.value = "Preview the fine before processing return."
                return
            }
            !_previewError.value.isNullOrBlank() -> {
                _actionMessage.value = "Resolve the preview error before processing return."
                return
            }
        }

        viewModelScope.launch {
            repository.processReturn(ids, condition).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _isProcessingReturn.value = true
                    is NetworkResult.Success -> {
                        _isProcessingReturn.value = false
                        _returnSuccess.value = result.data
                        clearSelectedStudent()
                        NotificationRefreshBus.requestRefresh()
                    }
                    is NetworkResult.Error -> {
                        _isProcessingReturn.value = false
                        _actionMessage.value = result.message
                    }
                    is NetworkResult.Unauthorized -> {
                        _isProcessingReturn.value = false
                        _unauthorized.value = true
                    }
                }
            }
        }
    }

    fun refreshStudentReturnData() {
        _selectedStudent.value?.let(::loadStudentReturnData) ?: loadReturnSettings()
    }

    fun clearReturnForm() {
        clearReturnSelections()
        _studentSearchResults.value = emptyList()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun clearReturnSuccess() {
        _returnSuccess.value = null
    }

    private fun clearReturnSelections() {
        _selectedIssueIds.value = emptySet()
        _selectedCondition.value = null
        _finePreview.value = null
        _previewError.value = null
    }

    private fun refreshPreviewIfReady() {
        if (_selectedIssueIds.value.orEmpty().isNotEmpty() && !_selectedCondition.value.isNullOrBlank()) {
            previewFine()
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
