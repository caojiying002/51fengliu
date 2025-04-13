package com.jiyingcao.a51fengliu.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.MainActivity

/**
 * 对话框样式的异地登录提示
 */
class RemoteLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_login)

        // 禁止通过系统返回按钮/手势返回
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 不执行任何操作，阻止返回
            }
        })

        findViewById<Button>(R.id.btn_relogin).setOnClickListener {
            RemoteLoginManager.reset()

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("IS_RELOGIN", true)
            }
            startActivity(intent)
            finish()
        }
    }

    // 禁止点击对话框外部区域关闭
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setFinishOnTouchOutside(false)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RemoteLoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}