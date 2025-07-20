package com.jiyingcao.a51fengliu.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CitySelectionViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    val selectedCity: StateFlow<String?> = dataStore.data
        .map { preferences ->
            preferences[SELECTED_CITY_KEY]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun setCity(cityCode: String) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[SELECTED_CITY_KEY] = cityCode
            }
        }
    }

    companion object {
        val SELECTED_CITY_KEY: Preferences.Key<String> =
            stringPreferencesKey("selected_city")
    }

    class Factory(private val dataStore: DataStore<Preferences>) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CitySelectionViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CitySelectionViewModel(dataStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}