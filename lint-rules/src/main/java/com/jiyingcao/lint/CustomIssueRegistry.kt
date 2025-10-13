package com.jiyingcao.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/**
 * 自定义 Lint 规则注册器
 */
class CustomIssueRegistry : IssueRegistry() {

    override val issues: List<Issue>
        get() = listOf(
            // 在这里添加自定义规则
        )

    override val api: Int
        get() = CURRENT_API

    override val minApi: Int
        get() = 8 // 支持的最低 API 版本

    // 定义 vendor 信息（可选，但推荐）
    override val vendor: Vendor = Vendor(
        vendorName = "Cao Jiying",
        feedbackUrl = "https://github.com/caojiying002/51fengliu/issues",
        contact = "caojiying002@126.com"
    )
}