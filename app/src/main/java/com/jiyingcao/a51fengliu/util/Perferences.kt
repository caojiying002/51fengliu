package com.jiyingcao.a51fengliu.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.jiyingcao.a51fengliu.App

const val PREFS_NAME = "91kuaihuo_prefs"
const val PREFS_KEY_LAST_SUCCESS_URL = "last_success_url"

fun getPrefs(context: Context = App.INSTANCE): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
}