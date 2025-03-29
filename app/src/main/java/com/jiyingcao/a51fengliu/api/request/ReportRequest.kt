package com.jiyingcao.a51fengliu.api.request

/*
{"infoId":900710,"content":"","picture":""}
 */
data class ReportRequest(
    val infoId: String,      // Int
    val content: String = "",
    val picture: String = ""
)