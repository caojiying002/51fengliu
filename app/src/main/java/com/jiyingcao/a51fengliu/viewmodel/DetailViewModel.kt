package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.viewmodel.UiState.Error
import com.jiyingcao.a51fengliu.viewmodel.UiState.Loading
import com.jiyingcao.a51fengliu.viewmodel.UiState.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class DetailIntent {
    object LoadDetail : DetailIntent()
    object Favorite : DetailIntent()
    object Unfavorite : DetailIntent()
}

class DetailViewModel: ViewModel() {
    private val _data = MutableLiveData<UiState<RecordInfo>>()
    val data: LiveData<UiState<RecordInfo>> = _data

    fun processIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> loadDetail()
            is DetailIntent.Favorite -> favorite()
            is DetailIntent.Unfavorite -> unfavorite()
        }
    }

    private fun loadDetail() {}
    private fun favorite() {}
    private fun unfavorite() {}

    fun fetchRecordById(
        showFullScreenLoading: Boolean = false,
        id: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO showFullScreenLoading不再使用, 待移除
            val loadingState =
                if (showFullScreenLoading) Loading.fullScreen() else Loading.pullRefresh()
            _data.postValue(loadingState)

            try {
                val response = RetrofitClient.apiService.getDetail(id)
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
        private const val TAG: String = "DetailViewModel"
    }
}