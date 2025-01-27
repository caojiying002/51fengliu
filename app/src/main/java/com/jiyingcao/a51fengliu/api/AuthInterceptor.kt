package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.data.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.runBlocking

/**
 * 正式环境使用的认证拦截器
 * @param tokenManager Token管理器实例
 * @param noAuthPaths 不需要认证的API路径列表
 */
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val noAuthPaths: List<String> = listOf(
        "/api/web/auth/login.json",
        "/api/web/auth/register.json",
        // 在此添加其他不需要认证的路径
    )
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // 检查是否是不需要认证的路径
        if (noAuthPaths.any { path.startsWith(it) }) {
            return chain.proceed(request)
        }

        // 使用协程获取token
        val token: String? = runBlocking {
            tokenManager.getToken()
        }

        // 如果有token则添加到请求头中
        val newRequest = if (!token.isNullOrEmpty()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }
}

/**
 * 调试环境使用的认证拦截器，可以手动指定一个固定的token
 * @param debugToken 调试用的固定token
 * @param noAuthPaths 不需要认证的API路径列表
 * @param enabled 是否启用调试拦截器
 */
class DebugAuthInterceptor(
    private val debugToken: String,
    private val noAuthPaths: List<String> = listOf(
        "/api/web/auth/login.json",
        "/api/web/auth/register.json",
        // 在此添加其他不需要认证的路径
    ),
    private val enabled: Boolean = true
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 如果拦截器未启用，直接放行请求
        if (!enabled) {
            return chain.proceed(request)
        }

        val path = request.url.encodedPath

        // 检查是否是不需要认证的路径
        if (noAuthPaths.any { path.startsWith(it) }) {
            return chain.proceed(request)
        }

        // 添加调试token到请求头
        val newRequest = request.newBuilder()
            .header("Authorization", "Bearer $debugToken")
            .build()

        return chain.proceed(newRequest)
    }
}