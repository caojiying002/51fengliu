package com.jiyingcao.a51fengliu.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.api.BASE_URL_FIXED
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.fetchLatestUrl
import com.jiyingcao.a51fengliu.api.fetchLatestUrlV2
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.ItemDataList
import com.jiyingcao.a51fengliu.util.PREFS_KEY_LAST_SUCCESS_URL
import com.jiyingcao.a51fengliu.util.getPrefs
import com.jiyingcao.a51fengliu.viewmodel.UiState.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val _data = MutableLiveData<UiState<ItemDataList>>()
    val data: LiveData<UiState<ItemDataList>> = _data

    fun fetchData(showFullScreenLoading: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO showFullScreenLoading不再使用, 待移除
            val loadingState =
                if (showFullScreenLoading) Loading.fullScreen() else Loading.pullRefresh()
            _data.postValue(loadingState)

            // 情况1：如果上次请求成功的URL存在，那么使用它
            val lastSuccessUrl = getPrefs().getString(PREFS_KEY_LAST_SUCCESS_URL, null)
            if (lastSuccessUrl != null) {
                // 将Retrofit使用的URL设置为上次成功的URL
                RetrofitClient.retrofitUrl = lastSuccessUrl
                try {
                    val response = RetrofitClient.apiService.getData()
                    _data.postValue(Success(response.data))

                    // 已经成功过的URL请求再次成功，返回
                    Log.i(TAG, "lastSuccessUrl ($lastSuccessUrl) success")
                    return@launch
                } catch (ignored: Exception) {
                    // 上次请求成功的URL这次失败了，请求获取最新URL
                    // _data.postValue(Error("网络请求失败"))
                    Log.w(TAG, "lastSuccessUrl ($lastSuccessUrl) fail, fetching dynamic url...")
                }
            }

            // 情况2：从99khredira.xyz获取最新的URL
            val dynamicUrl = fetchLatestUrlV2()
            if (dynamicUrl != null) {
                // 将Retrofit使用的URL设置为动态获取的最新URL
                RetrofitClient.retrofitUrl = dynamicUrl
                try {
                    val response = RetrofitClient.apiService.getData()
                    _data.postValue(Success(response.data))

                    // dynamicUrl请求成功，保存到本地以供下次使用
                    getPrefs().edit(true) {
                        putString(PREFS_KEY_LAST_SUCCESS_URL, dynamicUrl)
                    }
                    Log.i(TAG, "dynamicUrl ($dynamicUrl) success, saved to prefs")
                    return@launch
                } catch (e: Exception) {
                    // dynamicUrl请求失败，只好用固定的URL了
                    //_data.postValue(Error("网络请求失败"))
                    Log.w(TAG, "dynamicUrl ($dynamicUrl) fail, falling back to fixed url", e)
                }
            } else {
                //_data.postValue(Error("获取最新URL失败"))
                Log.w(TAG, "dynamicUrl null, falling back to fixed url")
            }

            // 情况3：使用写死的URL，如果(大概率)最终失败，通知UI
            RetrofitClient.retrofitUrl = BASE_URL_FIXED
            try {
                val response = RetrofitClient.apiService.getData()
                _data.postValue(Success(response.data))

                // 居然成功了，恭喜！
                // 您可能刚刚升级到最新版本，将成功的URL保存到本地以供下次使用
                getPrefs().edit(true) {
                    putString(PREFS_KEY_LAST_SUCCESS_URL, BASE_URL_FIXED)
                }
            } catch (e: Exception) {
                // 处理错误
                _data.postValue(Error("网络请求失败"))
                Log.w(TAG, "fixedUrl ($BASE_URL_FIXED) fetch error: ", e)
            }
        }
    }

    companion object {
        private const val TAG: String = "MainViewModel"
    }
}