package com.jiyingcao.a51fengliu.api.parse

import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.domain.exception.RemoteLoginException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import java.lang.reflect.Type

class ApiCallAdapter<T>(
    private val successType: Type
) : CallAdapter<T, Flow<T>> {
    override fun responseType() = successType

    override fun adapt(call: Call<T>): Flow<T> = flow {
        val response = call.execute()
        if (response.isSuccessful) {
            val body = response.body()
            when {
                body == null -> throw NullPointerException("Response body is null")
                body is ApiResponse<*> && body.code == 1003 -> throw RemoteLoginException(body.code, body.msg)
                else -> emit(body)
            }
        } else {
            throw HttpException(response)
        }
    }
}