package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.repository.UserSelectionRepository
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class CitySelectionViewModel(
    private val userSelectionRepository: UserSelectionRepository
) : ViewModel() {

    /**
     * 城市选择的共享流
     *
     * 直接暴露Repository的SharedFlow，避免不必要的二次包装
     * Repository层已经配置了合适的sharing策略和replay缓存
     *
     * @see UserSelectionRepository.selectedCityFlow
     */
    val selectedCitySharedFlow: SharedFlow<String?> = userSelectionRepository.selectedCityFlow

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