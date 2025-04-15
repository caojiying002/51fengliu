package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.util.City
import com.jiyingcao.a51fengliu.util.getCitiesForProvince
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import com.jiyingcao.a51fengliu.util.provinceList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

    /**
     *（市级列表）表示当前选中的省级代码；
     *（省级列表）-1 表示未选择任何省份。
     */
    private val _selectedProvinceIndexFlow = MutableStateFlow(-1) // -1 表示在省级列表
    val selectedProvinceIndexFlow: StateFlow<Int> = _selectedProvinceIndexFlow

    /**
     * 是否处于未选择省份的状态（省级列表）。
     */
    val isProvinceLevelFlow: StateFlow<Boolean> = _selectedProvinceIndexFlow
        .map { it == -1 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

    /**
     * 当前应该显示的列表（省级或者市级列表）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentListFlow: StateFlow<List<City>> = _selectedProvinceIndexFlow
        .flatMapLatest { index ->
            flow {
                emit(if (index == -1) provinceList else getCitiesForProvince(provinceList[index].code))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), provinceList)

    /**
     * 选中的城市
     */
    private val _selectedCity = MutableStateFlow<City?>(null)
    val selectedCity: StateFlow<City?> = _selectedCity

    // 处理列表项点击
    fun onItemClick(city: City) {
        if (isProvinceLevelFlow.value) {
            // 如果在省级列表，切换到该省的市级列表
            setSelectedProvinceIndex(provinceList.indexOfFirst { it.code == city.code })
        } else {
            // 如果在市级列表，选中该城市
            _selectedCity.value = city
        }
    }

    // （点击时）设置选中的省份
    fun setSelectedProvinceIndex(index: Int) {
        if (index in provinceList.indices || index == -1) {
            _selectedProvinceIndexFlow.value = index
            _selectedCity.value = null // 清除之前选中的城市
        }
    }

    // 获取当前选中的省份
    fun getSelectedProvince(): City? =
        _selectedProvinceIndexFlow.value.let { if (it != -1) provinceList[it] else null }

    // 返回上一级（从市级返回省级）
    fun backToProvinceLevel() {
        _selectedProvinceIndexFlow.value = -1
        _selectedCity.value = null
    }
}