package com.jiyingcao.a51fengliu.repository

import com.jiyingcao.a51fengliu.api.ApiService
import com.jiyingcao.a51fengliu.api.response.AppPopupNotice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置相关Repository
 * 处理应用配置相关的数据操作
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val apiService: ApiService
) : BaseRepository() {

    /**
     * 获取APP弹窗通知
     * 用于APP启动时显示促销等信息的弹窗
     *
     * @return Flow<Result<AppPopupNotice>> 弹窗通知数据流
     */
    fun getAppPopupNotice(): Flow<Result<AppPopupNotice>> {
        return apiCallStrict {
            apiService.getAppPopupNotice()
        }
    }
}
