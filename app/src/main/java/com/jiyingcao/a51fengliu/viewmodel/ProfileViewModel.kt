package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Init : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: Profile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ProfileIntent {
    object LoadProfile : ProfileIntent()
    object Refresh : ProfileIntent()
    object Retry : ProfileIntent()
}

class ProfileViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Init)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun processIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> fetchProfile()
            is ProfileIntent.Refresh -> fetchProfile()
            is ProfileIntent.Retry -> fetchProfile()
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            repository.getProfile().collect { result ->
                result.onSuccess { profile ->
                    _state.value = ProfileState.Success(profile)
                }.onFailure { e ->
                    _state.value = ProfileState.Error(e.toUserFriendlyMessage())
                }
            }
        }
    }
}

class ProfileViewModelFactory(
    private val repository: UserRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}