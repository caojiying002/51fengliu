package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.util.City
import com.jiyingcao.a51fengliu.util.getCitiesForProvince
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.jiyingcao.a51fengliu.util.provinceList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class ChooseCityIntent {
    object Load : ChooseCityIntent()
    data class SelectProvince(val index: Int) : ChooseCityIntent()
    data class SelectCity(val index: Int) : ChooseCityIntent()
    object BackToProvince : ChooseCityIntent()
}

data class ChooseCityState(
    val isProvinceLevel: Boolean = true,
    val provinceList: List<City> = emptyList(),
    val cityList: List<City> = emptyList(),
    val selectedProvince: City? = null,
    val selectedCity: City? = null,
    val title: String = "请选择省份"
)

sealed class ChooseCityEffect {
    data class CitySelected(val city: City) : ChooseCityEffect()
}

class ChooseCityViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        ChooseCityState(
            isProvinceLevel = true,
            provinceList = provinceList,
            cityList = emptyList(),
            selectedProvince = null,
            selectedCity = null,
            title = "请选择省市"
        )
    )
    val state: StateFlow<ChooseCityState> = _state

    private val _effect = Channel<ChooseCityEffect>()
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: ChooseCityIntent) {
        when (intent) {
            is ChooseCityIntent.Load -> {
                _state.value = _state.value.copy(
                    isProvinceLevel = true,
                    provinceList = provinceList,
                    cityList = emptyList(),
                    selectedProvince = null,
                    selectedCity = null,
                    title = "请选择省市"
                )
            }
            is ChooseCityIntent.SelectProvince -> {
                val province = provinceList[intent.index]
                _state.value = _state.value.copy(
                    isProvinceLevel = false,
                    selectedProvince = province,
                    cityList = getCitiesForProvince(province.code),
                    title = province.name
                )
            }
            is ChooseCityIntent.SelectCity -> {
                val currentState = _state.value
                if (!currentState.isProvinceLevel
                    && currentState.selectedProvince != null
                    && currentState.cityList.isNotEmpty()) {

                    val city = currentState.cityList[intent.index]
                    _state.value = _state.value.copy(selectedCity = city)
                    viewModelScope.launch { _effect.send(ChooseCityEffect.CitySelected(city)) }
                }
            }
            is ChooseCityIntent.BackToProvince -> {
                _state.value = _state.value.copy(
                    isProvinceLevel = true,
                    selectedProvince = null,
                    cityList = emptyList(),
                    title = "请选择省市"
                )
            }
        }
    }
}