package com.jiyingcao.a51fengliu.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.userSelectionDataStore: DataStore<Preferences> by preferencesDataStore("user_selections")

//val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore("app_settings")

val Context.authDataStore: DataStore<Preferences> by preferencesDataStore("auth")