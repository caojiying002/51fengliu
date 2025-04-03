package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jiyingcao.a51fengliu.ui.MainActivity

class RemoteLoginDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("登录已失效")
            .setMessage("您的账号已在其他设备登录")
            .setCancelable(false)
            .setPositiveButton("重新登录") { _, _ ->
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
            .create()
    }
}