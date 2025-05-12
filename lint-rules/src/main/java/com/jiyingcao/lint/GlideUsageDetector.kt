package com.jiyingcao.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.getQualifiedName

class GlideUsageDetector : Detector(), SourceCodeScanner {

    companion object {
        private val IMPLEMENTATION = Implementation(
            GlideUsageDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE = Issue.create(
            id = "DirectGlideStringUsage",
            briefDescription = "应使用 HostInvariantGlideUrl 包装 URL",
            explanation = """
                直接使用 String 或 URL 加载图片会导致主机变化时缓存失效。
                请使用 HostInvariantGlideUrl 包装图片地址，或使用 ImageLoader 工具类。
                
                不推荐：
                ```kotlin
                GlideApp.with(context).load(imageUrl).into(imageView)
                ```
                
                推荐方案一：
                ```kotlin
                GlideApp.with(context).load(HostInvariantGlideUrl(imageUrl)).into(imageView)
                ```
                
                推荐方案二：
                ```kotlin
                ImageLoader.load(context, imageUrl, imageView)
                ```
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        ).setAndroidSpecific(true) // 标记为 Android 特定的规则
    }

    override fun getApplicableMethodNames(): List<String> = listOf("load")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod
    ) {
        // 检查是否是 Glide 的 load 方法
        if (!isGlideLoadMethod(method)) {
            return
        }

        // 如果在 ImageLoader 类中，跳过检查
        val containingClass = node.getContainingUClass()
        if (containingClass?.qualifiedName?.contains("ImageLoader") == true) {
            return
        }

        // 检查参数
        val argument = node.valueArguments.firstOrNull() ?: return
        val argumentType = argument.getExpressionType()?.canonicalText ?: return

        // 检查是否是需要警告的类型
        if (isProblematicType(argumentType)) {
            // 检查参数是否已经是 HostInvariantGlideUrl
            val argumentText = argument.asSourceString()
            if (!argumentText.contains("HostInvariantGlideUrl")) {
                reportUsage(context, node, argument)
            }
        }
    }

    private fun isGlideLoadMethod(method: PsiMethod): Boolean {
        val containingClass = method.containingClass ?: return false
        val className = containingClass.qualifiedName ?: return false

        return method.name == "load" && (
                className.startsWith("com.bumptech.glide") ||
                        //className.startsWith("com.jiyingcao.a51fengliu.glide") ||
                        className.contains("GlideApp") ||
                        className.contains("GlideRequests") ||
                        className.contains("RequestBuilder")
                )
    }

    private fun isProblematicType(type: String): Boolean {
        return type in listOf(
            "java.lang.String",
            "kotlin.String",
            "java.net.URL",
            "android.net.Uri",
            "java.net.URI"
        )
    }

    private fun reportUsage(
        context: JavaContext,
        node: UCallExpression,
        argument: UExpression
    ) {
        // 创建修复建议
        val quickFix = LintFix.create()
            .name("使用 HostInvariantGlideUrl")
            .replace()
            .text(argument.asSourceString())
            .with("HostInvariantGlideUrl(${argument.asSourceString()})")
            .autoFix()
            .build()

        context.report(
            issue = ISSUE,
            location = context.getLocation(argument),
            message = "避免直接使用 String 加载图片，请使用 HostInvariantGlideUrl 或 ImageLoader",
            quickfixData = quickFix
        )
    }
}