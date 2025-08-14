package com.jiyingcao.a51fengliu.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jiyingcao.a51fengliu.di.UserSelectionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore Repository：依赖DataStore，处理本地数据
 * 用户选择相关的本地数据持久化
 * - 目前暂时只有**选择城市**场景
 */
@Singleton
class UserSelectionRepository @Inject constructor(
    @UserSelectionDataStore private val dataStore: DataStore<Preferences>
) {
    // 使用应用级别的CoroutineScope，确保数据流在应用生命周期内保持活跃
    private val repositoryScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.IO
    )
    
    /**
     * 共享的城市选择流
     * - replay = 1: 新订阅者立即获得最新值
     * - SharingStarted.WhileSubscribed(5000): 在最后一个订阅者取消订阅后保持5秒活跃
     * - 使用应用级CoroutineScope确保数据一致性
     */
    val selectedCityFlow: SharedFlow<String?> = dataStore.data
        .map { preferences ->
            preferences[SELECTED_CITY_KEY]
        }
        .shareIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000L),
            replay = 1
        )
    
    /**
     * 更新选中的城市
     */
    suspend fun updateSelectedCity(city: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_CITY_KEY] = city
        }
    }
    
    /**
     * 清除选中的城市
     */
    suspend fun clearSelectedCity() {
        dataStore.edit { preferences ->
            preferences.remove(SELECTED_CITY_KEY)
        }
    }

    companion object {
        private val SELECTED_CITY_KEY = stringPreferencesKey("selected_city")
    }
}