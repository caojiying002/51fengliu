package com.jiyingcao.a51fengliu.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.jiyingcao.a51fengliu.databinding.ActivityWebViewBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.AppLogger

class WebViewActivity : BaseActivity() {
    private lateinit var binding: ActivityWebViewBinding
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        if (initializeWebView()) {
            setupWebView()
            intent.getUrl()?.let {
                webView?.loadUrl(it)
            }
        }
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun initializeWebView(): Boolean {
        return try {
            webView = WebView(this)
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            webView?.layoutParams = layoutParams
            binding.webViewContainer.addView(webView)
            hideWebViewError()
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to initialize WebView", e)
            showWebViewError()
            false
        }
    }

    private fun hideWebViewError() {
        binding.errorTextView.isVisible = false
    }

    private fun showWebViewError() {
        binding.errorTextView.isVisible = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView?.settings?.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView?.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    view?.loadUrl(it)
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBar.visibility = View.INVISIBLE
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                super.onReceivedSslError(view, handler, error)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedClientCertRequest(view: WebView?, request: android.webkit.ClientCertRequest?) {
                super.onReceivedClientCertRequest(view, request)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedHttpAuthRequest(
                view: WebView?,
                handler: android.webkit.HttpAuthHandler?,
                host: String?,
                realm: String?
            ) {
                super.onReceivedHttpAuthRequest(view, handler, host, realm)
                binding.progressBar.visibility = View.INVISIBLE
            }

            override fun onReceivedLoginRequest(
                view: WebView?,
                realm: String?,
                account: String?,
                args: String?
            ) {
                super.onReceivedLoginRequest(view, realm, account, args)
                binding.progressBar.visibility = View.INVISIBLE
            }
        }
        webView?.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.progress = newProgress
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let {
                    supportActionBar?.title = it
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                // icon?.let { supportActionBar?.setIcon(it) }
            }

            override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
                super.onReceivedTouchIconUrl(view, url, precomposed)
                // url?.let { supportActionBar?.setIcon(it) }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
            }

            @Deprecated("Deprecated in Java")
            override fun onShowCustomView(
                view: View?,
                requestedOrientation: Int,
                callback: CustomViewCallback?
            ) {
                super.onShowCustomView(view, requestedOrientation, callback)
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onRequestFocus(view: WebView?) {
                super.onRequestFocus(view)
            }

            override fun onCloseWindow(window: WebView?) {
                super.onCloseWindow(window)
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                return super.onJsConfirm(view, url, message, result)
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult?): Boolean {
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                return super.onJsBeforeUnload(view, url, message, result)
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: android.webkit.GeolocationPermissions.Callback?) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

            override fun onGeolocationPermissionsHidePrompt() {
                super.onGeolocationPermissionsHidePrompt()
            }

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                super.onPermissionRequest(request)
            }

            override fun onPermissionRequestCanceled(request: android.webkit.PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
            }

            @Deprecated("Deprecated in Java")
            override fun onJsTimeout(): Boolean {
                return super.onJsTimeout()
            }

            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                super.onConsoleMessage(message, lineNumber, sourceID)
            }

            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                return super.onConsoleMessage(consoleMessage)
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return super.getDefaultVideoPoster()
            }

            override fun getVideoLoadingProgressView(): View? {
                return super.getVideoLoadingProgressView()
            }

            override fun getVisitedHistory(callback: android.webkit.ValueCallback<Array<String>>?) {
                super.getVisitedHistory(callback)
            }

            @Deprecated("Deprecated in Java")
            override fun onExceededDatabaseQuota(
                url: String?,
                databaseIdentifier: String?,
                quota: Long,
                estimatedDatabaseSize: Long,
                totalQuota: Long,
                quotaUpdater: android.webkit.WebStorage.QuotaUpdater?
            ) {
                super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getUrl()?.let {
            webView?.loadUrl(it)
        }
    }

    companion object {
        private const val TAG = "WebViewActivity"

        @JvmStatic
        fun createIntent(context: Context, url: String? = null) =
            Intent(context, WebViewActivity::class.java).apply {
                putExtra("KEY_URL", url)
            }

        @JvmStatic
        fun start(context: Context, url: String? = null) {
            context.startActivity(createIntent(context, url))
        }

        private fun Intent.getUrl(): String? = getStringExtra("KEY_URL")
    }
}