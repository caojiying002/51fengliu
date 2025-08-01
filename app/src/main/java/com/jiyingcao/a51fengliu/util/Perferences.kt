package com.jiyingcao.a51fengliu.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

const val PREFS_NAME = "51fengliu_prefs"
const val PREFS_KEY_SELECTED_CITY = "selected_city"

@Deprecated("使用单独的 DataStore 实例代替")
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFS_NAME)