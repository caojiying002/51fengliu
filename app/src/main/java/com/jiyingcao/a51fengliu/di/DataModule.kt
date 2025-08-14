package com.jiyingcao.a51fengliu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jiyingcao.a51fengliu.datastore.authDataStore
import com.jiyingcao.a51fengliu.datastore.userSelectionDataStore
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
 * - DataStore实例（使用Qualifier区分不同用途）
 * - TokenManager：JWT令牌管理
 * - LoginStateManager：登录状态管理
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @AuthDataStore
    fun provideAuthDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.authDataStore
    }

    @Provides
    @Singleton
    @UserSelectionDataStore
    fun provideUserSelectionDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.userSelectionDataStore
    }

}