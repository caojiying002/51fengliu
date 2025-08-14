package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.repository.UserSelectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CitySelectionViewModel @Inject constructor(
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
}