package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jiyingcao.a51fengliu.data.RemoteLoginManager
import com.jiyingcao.a51fengliu.ui.MainActivity

class RemoteLoginDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("登录已失效")
            .setMessage("您的账号已在其他设备登录")
            .setCancelable(false)
            .setPositiveButton("重新登录") { _, _ ->
                RemoteLoginManager.reset()

                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    // 添加一个标志表示这是重新登录
                    putExtra("IS_RELOGIN", true)
                }
                startActivity(intent)
                activity?.finish()
            }
            .create()
    }
}