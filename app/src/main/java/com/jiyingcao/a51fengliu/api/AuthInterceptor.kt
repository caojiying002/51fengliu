package com.jiyingcao.a51fengliu.api

import com.jiyingcao.a51fengliu.api.TokenPolicy.Policy
import com.jiyingcao.a51fengliu.data.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import retrofit2.Invocation

/**
 * 正式环境使用的认证拦截器
 * @param tokenManager Token管理器实例
 */
class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val token = runBlocking { tokenManager.getToken() }

        val tokenPolicy = request.findTokenPolicy()

        return when (tokenPolicy?.value) {
            null -> chain.proceed(request.addTokenIfAny(token)) // 未标注的接口采用默认策略
            Policy.OPTIONAL -> chain.proceed(request.addTokenIfAny(token))
            Policy.FORBIDDEN -> chain.proceed(request)
            Policy.REQUIRED -> {
                if (token.isNullOrEmpty()) {
                    // TODO 可以根据需求决定是抛异常还是返回401响应
                    throw IllegalStateException("Required token not found")
                }
                chain.proceed(request.addToken(token))
            }
        }
    }

    private fun Request.addTokenIfAny(token: String?): Request =
        if (token.isNullOrEmpty()) this else addToken(token)

    private fun Request.addToken(token: String): Request =
        this.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    /**
     * 获取请求对应的TokenPolicy注解
     */
    private fun Request.findTokenPolicy(): TokenPolicy? {
        return this.tag(Invocation::class.java)
            ?.method()
            ?.getAnnotation(TokenPolicy::class.java)
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