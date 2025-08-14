package com.jiyingcao.a51fengliu.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jiyingcao.a51fengliu.config.AppConfig
import com.jiyingcao.a51fengliu.di.AuthDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Singleton
class TokenManager @Inject constructor(
    @AuthDataStore private val dataStore: DataStore<Preferences>
) {
    // 获取 Token 的 Flow，用于需要观察 Token 变化的场景
    val token: Flow<String?> = dataStore.data
        .map { preferences ->
            // 如果是debug模式且启用了debug token，直接返回debug token
            if (AppConfig.Debug.useDebugToken()) {
                AppConfig.Debug.DEFAULT_DEBUG_TOKEN
            } else {
                preferences[TOKEN_KEY]
            }
        }

    // 用于非 UI 场景获取 Token 的挂起函数
    suspend fun getToken(): String? {
        // 如果是debug模式且启用了debug token，直接返回debug token
        if (AppConfig.Debug.useDebugToken()) {
            return AppConfig.Debug.DEFAULT_DEBUG_TOKEN
        }
        
        return dataStore.data
            .map { preferences ->
                preferences[TOKEN_KEY]
            }
            .firstOrNull()
    }

    // 保存 Token
    suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    // 清除 Token
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }

    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }
}