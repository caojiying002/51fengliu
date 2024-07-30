package com.jiyingcao.a51fengliu.util

import android.content.Context
import android.os.Looper
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * 在任意线程弹出Toast
 */
fun Context.showToast(text: CharSequence) {
    if (Looper.getMainLooper() == Looper.myLooper())
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    else
        ContextCompat.getMainExecutor(this).execute {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
}