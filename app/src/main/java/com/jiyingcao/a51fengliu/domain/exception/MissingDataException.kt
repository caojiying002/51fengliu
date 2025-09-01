package com.jiyingcao.a51fengliu.domain.exception

/**
 * 业务成功 (code == 0) 但 data 为空的异常。
 * 与 [HttpEmptyResponseException] 区分：后者表示 Retrofit 的 Response.body() 直接为 null，
 * 而本异常表示拿到了 ApiResponse 结构，但其中的 data 违反“成功必须有数据”的语义。
 */
class MissingDataException(
    override val message: String = "Missing required data in successful ApiResponse",
    override val cause: Throwable? = null
) : IllegalStateException()