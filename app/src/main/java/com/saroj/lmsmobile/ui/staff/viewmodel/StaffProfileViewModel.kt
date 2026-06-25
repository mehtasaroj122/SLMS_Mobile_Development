package com.saroj.lmsmobile.ui.staff.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saroj.lmsmobile.data.models.common.NetworkResult
import com.saroj.lmsmobile.data.repository.StaffProfileRepository
import com.saroj.lmsmobile.ui.staff.model.StaffProfileUiModel
import kotlinx.coroutines.launch
import java.io.File

class StaffProfileViewModel(
    private val repository: StaffProfileRepository
) : ViewModel() {

    private val _profileState = MutableLiveData<NetworkResult<StaffProfileUiModel>>()
    val profileState: LiveData<NetworkResult<StaffProfileUiModel>> = _profileState

    private val _profileActionState = MutableLiveData<NetworkResult<String>>()
    val profileActionState: LiveData<NetworkResult<String>> = _profileActionState

    private var currentProfile: StaffProfileUiModel? = null
    private var hasLoaded = false

    fun loadInitial() {
        if (hasLoaded) return
        hasLoaded = true
        refresh()
    }

    fun refresh() {
        loadProfile()
    }

    fun updateProfile(profile: StaffProfileUiModel) {
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
                        _profileActionState.value = NetworkResult.Success("Profile photo updated successfully")
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
                        _profileActionState.value = NetworkResult.Success("Profile photo removed successfully")
                    }
                    is NetworkResult.Error -> _profileActionState.value = NetworkResult.Error(result.message, result.code)
                    is NetworkResult.Unauthorized -> _profileActionState.value = NetworkResult.Unauthorized()
                }
            }
        }
    }

    fun deleteAccount(password: String, confirmation: String) {
        viewModelScope.launch {
            repository.deleteAccount(password, confirmation).collect { result ->
                _profileActionState.value = result
            }
        }
    }

    fun logoutAfterDelete() {
        viewModelScope.launch {
            repository.logout().collect { result ->
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
}
