package com.saroj.lmsmobile.ui.student.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StudentProfileRepository
import com.saroj.lmsmobile.ui.student.model.DeleteEligibilityUiModel
import com.saroj.lmsmobile.ui.student.model.StudentProfileUiModel
import kotlinx.coroutines.launch
import java.io.File

class StudentProfileViewModel(
    private val repository: StudentProfileRepository
) : ViewModel() {

    private val _profileState = MutableLiveData<NetworkResult<StudentProfileUiModel>>()
    val profileState: LiveData<NetworkResult<StudentProfileUiModel>> = _profileState

    private val _deleteEligibilityState = MutableLiveData<NetworkResult<DeleteEligibilityUiModel>>()
    val deleteEligibilityState: LiveData<NetworkResult<DeleteEligibilityUiModel>> = _deleteEligibilityState

    private val _profileActionState = MutableLiveData<NetworkResult<String>>()
    val profileActionState: LiveData<NetworkResult<String>> = _profileActionState

    private var currentProfile: StudentProfileUiModel? = null
    private var hasLoaded = false

    fun loadInitial() {
        if (hasLoaded) return
        hasLoaded = true
        refresh()
    }

    fun refresh() {
        loadProfile()
        loadDeleteEligibility()
    }

    fun updateProfile(profile: StudentProfileUiModel) {
        viewModelScope.launch {
            repository.updateProfile(profile).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _profileActionState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        currentProfile = result.data
                        _profileState.value = NetworkResult.Success(result.data)
                        _profileActionState.value = NetworkResult.Success("Profile updated successfully")
                    }
                    is NetworkResult.Error -> _profileActionState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _profileActionState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            repository.changePassword(currentPassword, newPassword, confirmPassword).collect { result ->
                _profileActionState.value = result
            }
        }
    }

    fun uploadPhoto(file: File) {
        viewModelScope.launch {
            repository.uploadPhoto(file, currentProfile).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _profileActionState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        currentProfile = result.data
                        _profileState.value = NetworkResult.Success(result.data)
                        _profileActionState.value = NetworkResult.Success("Profile photo uploaded successfully")
                    }
                    is NetworkResult.Error -> _profileActionState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _profileActionState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun removePhoto() {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            repository.removePhoto(profile).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _profileActionState.value = NetworkResult.Loading()
                    is NetworkResult.Success -> {
                        currentProfile = result.data
                        _profileState.value = NetworkResult.Success(result.data)
                        _profileActionState.value = NetworkResult.Success("Profile photo removed")
                    }
                    is NetworkResult.Error -> _profileActionState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _profileActionState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount().collect { result ->
                _profileActionState.value = result
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            repository.getProfile().collect { result ->
                if (result is NetworkResult.Success) {
                    currentProfile = result.data
                }
                _profileState.value = result
            }
        }
    }

    private fun loadDeleteEligibility() {
        viewModelScope.launch {
            repository.getDeleteEligibility().collect { result ->
                _deleteEligibilityState.value = result
            }
        }
    }
}
