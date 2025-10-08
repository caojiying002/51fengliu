package com.jiyingcao.a51fengliu.api.interceptor

import com.google.gson.stream.JsonReader
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.domain.exception.ApiException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 业务错误拦截器
 *
 * 统一处理服务端返回的全局业务错误码，如：
 * - 1003: 异地登录
 * - 未来可扩展：强制更新、系统维护等
 *
 * ## 职责边界
 * - 仅处理HTTP成功（2xx）但业务失败的场景
 * - 通过发送全局事件通知UI层，不直接抛异常
 * - Repository层保留Fallback机制作为双重保障
 *
 * ## 性能优化
 * - 使用流式JSON读取，避免大响应导致OOM
 * - 找到code字段后立即停止解析，减少CPU消耗
 * - 解析失败不影响主流程，让Repository层继续处理
 */
class BusinessErrorInterceptor(
    private val remoteLoginManager: RemoteLoginManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        // 仅处理HTTP成功的响应
        if (response.isSuccessful) {
            try {
                // 使用流式读取提取code字段，避免读取整个响应体
                val code = extractCodeFromResponse(response)

                // 全局业务错误码处理
                when (code) {
                    ApiException.CODE_REMOTE_LOGIN -> {
                        remoteLoginManager.handleRemoteLogin()
                    }
                    // 未来可扩展其他全局错误码
                    // 4000 -> maintenanceModeManager.showMaintenance()
                    // 5000 -> forceUpdateManager.showUpdate()
                }
            } catch (e: Exception) {
                // JSON解析失败忽略，让后续流程（Repository层）处理
            }
        }

        return response
    }

    /**
     * 流式读取响应体中的code字段
     *
     * 使用Gson的JsonReader进行流式解析，优点：
     * - 内存安全：不读取完整响应体
     * - 性能优化：找到code字段后立即停止
     * - 位置无关：无论code字段在JSON的什么位置都能正确读取
     *
     * @return code字段的值，解析失败返回null
     */
    private fun extractCodeFromResponse(response: Response): Int? {
        // peekBody避免消费原始响应体，让Repository层仍能正常读取
        // 使用合理的缓冲区大小（8KB），兼顾性能和内存
        val source = response.peekBody(8192).source()
        val reader = JsonReader(source.inputStream().reader())

        try {
            reader.beginObject()
            var code: Int? = null

            // 逐字段读取，找到code即停止
            while (reader.hasNext() && code == null) {
                when (reader.nextName()) {
                    "code" -> code = reader.nextInt()
                    else -> reader.skipValue()
                }
            }

            return code
        } finally {
            reader.close()
        }
    }
}
