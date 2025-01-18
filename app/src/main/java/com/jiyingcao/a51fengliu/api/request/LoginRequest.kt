package com.jiyingcao.a51fengliu.api.request

/**
 * {"name":"jiyingcao","password":"xxxxxx"}
 */
data class LoginRequest(
    val name: String = "",
    val password: String = ""
)