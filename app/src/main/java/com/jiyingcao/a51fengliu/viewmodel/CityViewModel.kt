package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.request.RecordsRequest
import com.jiyingcao.a51fengliu.api.response.*
import com.jiyingcao.a51fengliu.viewmodel.UiState0.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO 重命名为CityRecordsViewModel？
class CityViewModel: ViewModel() {
    private val _data = MutableLiveData<UiState0<PageData>>()
    val data: LiveData<UiState0<PageData>> = _data

    fun fetchCityDataByPage(
        cityCode: String = "330100",
        sort: String = "publish",
        page: Int = 1
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadingState =
                if (page == 1) Loading.fullScreen() else Loading.pullRefresh()
            _data.postValue(loadingState)
            try {
                val queryMap = RecordsRequest
                    .forCity(cityCode, sort, page)
                    .toMap()
                val response = RetrofitClient.apiService.getRecords(queryMap)
                if (response.code != 0) {
                    _data.postValue(Error("API状态码 code=${response.code}, msg=${response.msg}"))
                    Log.w(TAG, "API状态码 code=${response.code}, msg=${response.msg}")
                    return@launch
                }
                _data.postValue(Success(response.data!!))
            } catch (e: Exception) {
                _data.postValue(Error("网络请求失败"))
                Log.w(TAG, "fetchData error: ", e)
            }

        }
    }

    companion object {
        private const val TAG: String = "CityViewModel"
    }
}