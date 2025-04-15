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

sealed class ChooseCityState {
    data class ProvinceList(val provinces: List<City>) : ChooseCityState()
    data class CityList(val province: City, val cities: List<City>) : ChooseCityState()
}

sealed class ChooseCityEffect {
    data class CitySelected(val city: City) : ChooseCityEffect()
}

class ChooseCityViewModel : ViewModel() {

    private val _state = MutableStateFlow<ChooseCityState>(
        ChooseCityState.ProvinceList(provinceList)
    )
    val state: StateFlow<ChooseCityState> = _state

    private val _effect = Channel<ChooseCityEffect>()
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: ChooseCityIntent) {
        when (intent) {
            is ChooseCityIntent.Load -> {
                _state.value = ChooseCityState.ProvinceList(provinceList)
            }
            is ChooseCityIntent.SelectProvince -> {
                val province = provinceList[intent.index]
                val cities = getCitiesForProvince(province.code)
                _state.value = ChooseCityState.CityList(province, cities)
            }
            is ChooseCityIntent.SelectCity -> {
                val currentState = _state.value
                if (currentState is ChooseCityState.CityList) {
                    val city = currentState.cities[intent.index]
                    viewModelScope.launch { _effect.send(ChooseCityEffect.CitySelected(city)) }
                }
            }
            is ChooseCityIntent.BackToProvince -> {
                _state.value = ChooseCityState.ProvinceList(provinceList)
            }
        }
    }
}