package com.jiyingcao.a51fengliu.ui.common

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.jiyingcao.a51fengliu.databinding.ActivityWebViewBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.AppLogger

class WebViewActivity : BaseActivity() {
    private lateinit var binding: ActivityWebViewBinding
    private var webView: WebView? = null

    private val webViewGoBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBackPressedCallback()
        setupClickListeners()
        if (!initializeWebView())
            return

        setupWebView()
        // 先尝试恢复状态，失败则加载初始URL
        if (!restoreWebViewState(savedInstanceState)) {
            loadInitialUrl()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, webViewGoBackCallback)
    }

    private fun setupClickListeners() {
        binding.titleBar.titleBarBack.setOnClickListener {
            // 禁用WebView返回, 执行常规返回(Activity关闭)
            webViewGoBackCallback.isEnabled = false
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
            lifecycle.addObserver(WebViewLifecycleObserver(webView!!))
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.let { webView ->
            val webViewState = Bundle()
            webView.saveState(webViewState)
            outState.putBundle(WEBVIEW_STATE_KEY, webViewState)
        }
    }

    /**
     * 从Bundle恢复WebView状态，返回布尔值表示是否成功恢复
     */
    private fun restoreWebViewState(savedInstanceState: Bundle?): Boolean {
        val webViewState = savedInstanceState?.getBundle(WEBVIEW_STATE_KEY)
        return if (webViewState != null) {
            webView?.restoreState(webViewState)
            true
        } else {
            false
        }
    }

    private fun loadInitialUrl() {
        intent.getUrl()?.let {
            webView?.loadUrl(it)
        }
    }

    private class WebViewLifecycleObserver(private val webView: WebView) : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            webView.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            webView.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            val isFinishing = when (owner) {
                is Activity -> owner.isFinishing
                is Fragment -> owner.activity?.isFinishing ?: true
                is android.app.Fragment -> owner.activity?.isFinishing ?: true
                else -> true
            }

            if (isFinishing) {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.clearView()
            }
            webView.destroy()
        }
    }

    companion object {
        private const val TAG = "WebViewActivity"
        private const val WEBVIEW_STATE_KEY = "webview_state"

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