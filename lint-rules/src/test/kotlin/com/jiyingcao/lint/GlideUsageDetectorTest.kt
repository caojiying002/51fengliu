package com.jiyingcao.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class GlideUsageDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = GlideUsageDetector()

    override fun getIssues(): List<Issue> = listOf(GlideUsageDetector.ISSUE)

    fun testDirectStringUsage() {
        lint().files(
            // 提供 Glide 的 stub 定义
            kotlin("""
                package com.bumptech.glide

                object Glide {
                    fun with(context: android.content.Context): RequestBuilder<*> = RequestBuilder()
                }
                
                class RequestBuilder<T> {
                    fun load(url: String): RequestBuilder<T> = this
                    fun load(url: Any): RequestBuilder<T> = this
                    fun into(target: Any): Any = target
                }
            """.trimIndent()),

            // 提供 Android 相关的 stub
            kotlin("""
                package android.content
                
                class Context
            """.trimIndent()),

            kotlin("""
                package android.widget
                
                class ImageView
            """.trimIndent()),

            // 测试代码
            kotlin("""
                package com.example
                
                import android.content.Context
                import android.widget.ImageView
                import com.bumptech.glide.Glide
                
                class TestClass {
                    fun loadImage(context: Context, imageView: ImageView, url: String) {
                        Glide.with(context).load(url).into(imageView)
                    }
                }
            """.trimIndent())
        )
            .run()
            .expect("""
            src/com/example/TestClass.kt:9: Error: 避免直接使用 String 加载图片，请使用 HostInvariantGlideUrl 或 ImageLoader [DirectGlideStringUsage]
                    Glide.with(context).load(url).into(imageView)
                                             ~~~
            1 errors, 0 warnings
        """.trimIndent())
    }

    fun testHostInvariantGlideUrlUsage() {
        lint().files(
            // Glide stub
            kotlin("""
                package com.bumptech.glide

                object Glide {
                    fun with(context: android.content.Context): RequestBuilder<*> = RequestBuilder()
                }
                
                class RequestBuilder<T> {
                    fun load(url: String): RequestBuilder<T> = this
                    fun load(url: Any): RequestBuilder<T> = this
                    fun into(target: Any): Any = target
                }
            """.trimIndent()),

            // Android stubs
            kotlin("""
                package android.content
                
                class Context
            """.trimIndent()),

            kotlin("""
                package android.widget
                
                class ImageView
            """.trimIndent()),

            // HostInvariantGlideUrl stub
            kotlin("""
                package com.jiyingcao.a51fengliu.glide
                
                class HostInvariantGlideUrl(val url: String)
            """.trimIndent()),

            // 测试代码
            kotlin("""
                package com.example
                
                import android.content.Context
                import android.widget.ImageView
                import com.bumptech.glide.Glide
                import com.jiyingcao.a51fengliu.glide.HostInvariantGlideUrl
                
                class TestClass {
                    fun loadImage(context: Context, imageView: ImageView, url: String) {
                        Glide.with(context).load(HostInvariantGlideUrl(url)).into(imageView)
                    }
                }
            """.trimIndent())
        )
            .run()
            .expectClean()
    }

    // 测试 GlideApp 的使用
    fun testGlideAppUsage() {
        lint().files(
            // GlideApp stub
            kotlin("""
                package com.jiyingcao.a51fengliu.glide
                
                import com.bumptech.glide.RequestBuilder

                object GlideApp {
                    fun with(context: android.content.Context): GlideRequests = GlideRequests()
                }
                
                class GlideRequests {
                    fun load(url: String): RequestBuilder<*> = RequestBuilder()
                    fun load(url: Any): RequestBuilder<*> = RequestBuilder()
                }
            """.trimIndent()),

            // Glide RequestBuilder stub
            kotlin("""
                package com.bumptech.glide
                
                class RequestBuilder<T> {
                    fun into(target: Any): Any = target
                }
            """.trimIndent()),

            // Android stubs
            kotlin("""
                package android.content
                
                class Context
            """.trimIndent()),

            kotlin("""
                package android.widget
                
                class ImageView
            """.trimIndent()),

            // 测试代码
            kotlin("""
                package com.example
                
                import android.content.Context
                import android.widget.ImageView
                import com.jiyingcao.a51fengliu.glide.GlideApp
                
                class TestClass {
                    fun loadImage(context: Context, imageView: ImageView, url: String) {
                        GlideApp.with(context).load(url).into(imageView)
                    }
                }
            """.trimIndent())
        )
            .run()
            .expect("""
            src/com/example/TestClass.kt:9: Error: 避免直接使用 String 加载图片，请使用 HostInvariantGlideUrl 或 ImageLoader [DirectGlideStringUsage]
                    GlideApp.with(context).load(url).into(imageView)
                                                ~~~
            1 errors, 0 warnings
        """.trimIndent())
    }

    // 测试在 ImageLoader 中的使用不应该报错
    fun testImageLoaderUsage() {
        lint().files(
            // Glide stubs
            kotlin("""
                package com.bumptech.glide

                object Glide {
                    fun with(context: android.content.Context): RequestBuilder<*> = RequestBuilder()
                }
                
                class RequestBuilder<T> {
                    fun load(url: String): RequestBuilder<T> = this
                    fun load(url: Any): RequestBuilder<T> = this
                    fun into(target: Any): Any = target
                }
            """.trimIndent()),

            // Android stubs
            kotlin("""
                package android.content
                
                class Context
            """.trimIndent()),

            kotlin("""
                package android.widget
                
                class ImageView
            """.trimIndent()),

            // 测试代码 - ImageLoader 类中的使用
            kotlin("""
                package com.jiyingcao.a51fengliu.util
                
                import android.content.Context
                import android.widget.ImageView
                import com.bumptech.glide.Glide
                
                object ImageLoader {
                    fun load(context: Context, url: String, imageView: ImageView) {
                        // 在 ImageLoader 中直接使用 String 应该被允许
                        Glide.with(context).load(url).into(imageView)
                    }
                }
            """.trimIndent())
        )
            .run()
            .expectClean()
    }
}