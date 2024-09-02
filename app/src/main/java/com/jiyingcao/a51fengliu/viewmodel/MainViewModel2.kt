package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient2
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.viewmodel.UiState.Error
import com.jiyingcao.a51fengliu.viewmodel.UiState.Loading
import com.jiyingcao.a51fengliu.viewmodel.UiState.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel2: ViewModel() {
    private val _data = MutableLiveData<UiState<PageData>>()
    val data: LiveData<UiState<PageData>> = _data

    fun fetchByPage(
        showFullScreenLoading: Boolean = false,
        page: Int = 1
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO showFullScreenLoading不再使用, 待移除
            val loadingState =
                if (showFullScreenLoading) Loading.fullScreen() else Loading.pullRefresh()
            _data.postValue(loadingState)

            try {
                // sort=daily是热门，sort=publish是最新
                val response = RetrofitClient2.apiService.getPageData(/*sort = "publish", */page = page)
                if (response.code != 0) {
                    _data.postValue(Error("API状态码 code=${response.code}, msg=${response.msg}"))
                    Log.w(TAG, "API状态码 code=${response.code}, msg=${response.msg}")
                    return@launch
                }
                _data.postValue(Success(response.data!!))
            } catch (e: Exception) {
                // 处理错误
                _data.postValue(Error("网络请求失败"))
                Log.w(TAG, "网络请求失败: ", e)
            }
        }
    }

    companion object {
        private const val TAG: String = "MainViewModel2"
    }
}