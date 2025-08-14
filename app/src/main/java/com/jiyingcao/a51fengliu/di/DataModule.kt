package com.jiyingcao.a51fengliu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jiyingcao.a51fengliu.data.LoginStateManager
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.util.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据层依赖注入模块
 * 
 * 负责提供数据持久化相关的依赖，包括：
 * - DataStore实例
 * - TokenManager：JWT令牌管理
 * - LoginStateManager：登录状态管理
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideTokenManager(dataStore: DataStore<Preferences>): TokenManager {
        return TokenManager.getInstance(dataStore)
    }

    @Provides
    @Singleton
    fun provideLoginStateManager(tokenManager: TokenManager): LoginStateManager {
        return LoginStateManager.getInstance(tokenManager)
    }
}