package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.repository.UserSelectionRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class CitySelectionViewModel(
    private val userSelectionRepository: UserSelectionRepository
) : ViewModel() {

    /**
     * 城市选择的共享流
     *
     * 注意：这里使用 SharedFlow 而不是常规的 StateFlow
     * 原因：避免重复的初始null值发射，保持语义清晰（null = 用户未选择城市）
     *
     * @see UserSelectionRepository.selectedCityFlow
     */
    val selectedCitySharedFlow: SharedFlow<String?> = userSelectionRepository.selectedCityFlow
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    fun setCity(cityCode: String) {
        viewModelScope.launch {
            userSelectionRepository.updateSelectedCity(cityCode)
        }
    }

    class Factory(
        private val userSelectionRepository: UserSelectionRepository = UserSelectionRepository.getInstance(App.INSTANCE)
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CitySelectionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CitySelectionViewModel(userSelectionRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}