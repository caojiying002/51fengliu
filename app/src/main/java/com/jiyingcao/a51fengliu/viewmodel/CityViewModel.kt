package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.ApiResponse
import com.jiyingcao.a51fengliu.api.response.PagedItemData
import com.jiyingcao.a51fengliu.viewmodel.UiState.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO [CityViewModel]和[SearchViewModel]是否需要抽取共同父类[PagedViewModel]？
class CityViewModel: ViewModel() {
    private val _data = MutableLiveData<UiState<PagedItemData>>()
    val data: LiveData<UiState<PagedItemData>> = _data

    fun fetchCityDataByPage(page: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            val loadingState =
                if (page == 1) Loading.fullScreen() else Loading.pullRefresh()
            _data.postValue(loadingState)
            try {
                val response = RetrofitClient.apiService.getCityData(page = page)
                val pageWrapped = PagedItemData(response.data, page)
                _data.postValue(Success(pageWrapped))
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