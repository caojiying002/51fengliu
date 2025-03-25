package com.jiyingcao.a51fengliu.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val density: Float = Resources.getSystem().displayMetrics.density

fun setEdgeToEdgePaddings(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

        // remove the listener to avoid status bar padding being removed when open full-screen activities
        ViewCompat.setOnApplyWindowInsetsListener(v, null)
        insets
    }
}

fun Activity.setContentViewWithSystemBarPaddings(view: View) {
    setContentView(view)
    setEdgeToEdgePaddings(view)
}

fun Activity.setContentViewWithSystemBarPaddings(@LayoutRes layoutResID: Int, @IdRes rootId: Int) {
    setContentView(layoutResID)
    setEdgeToEdgePaddings(findViewById(rootId))
}

fun TextView.copyOnLongClick(
    copySuccess: () -> Unit = { Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show() }
) {
    setOnLongClickListener {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
        copySuccess()
        true
    }
}

fun RecyclerView.scrollToTopIfEmpty(c: Collection<*>) {
    if (c.isEmpty()) scrollToPosition(0)
}