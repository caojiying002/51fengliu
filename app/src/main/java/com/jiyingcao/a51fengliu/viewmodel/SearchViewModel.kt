package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.viewmodel.LoadingType.*
import com.jiyingcao.a51fengliu.viewmodel.UiState2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO [CityViewModel]和[SearchViewModel]是否需要抽取共同父类[PagedViewModel]？
class SearchViewModel: ViewModel() {
    private val _keywords = MutableLiveData<String>()
    val keywords: LiveData<String> = _keywords

    private val _page = MutableLiveData<Int>()
    val page: LiveData<Int> = _page

    private val _uiState = MutableLiveData<UiState2>()
    val uiState: LiveData<UiState2> = _uiState

    init {
        _keywords.value = ""
        _page.value = 1
        //_uiState.value = Success({})
    }

    /** 一次全新的搜索，需要显示全屏Loading */
    fun startNewSearch(newKeywords: String) {
        _keywords.value = newKeywords
        _page.value = 1 // 每次更新关键词时重置页码为1
        doSearch(FULL_SCREEN)
    }

    /** 用于下拉刷新，所以不用显示全屏Loading */
    fun pullRefresh() {
        _page.value = 1 // 下拉刷新时重置页码为1
        doSearch(PULL_REFRESH)
    }

    /** 上拉加载更多 */
    fun loadMore() {
        _page.value = (_page.value ?: 1) + 1
        doSearch(LOAD_MORE)
    }

    fun doSearch(loadingType: LoadingType = NONE) {
        if (_keywords.value == null || _page.value == null)
            return
        doSearch(_keywords.value!!, _page.value!!, loadingType)
    }

    private fun doSearch(keywords: String = "", page: Int = 1, loadingType: LoadingType = NONE) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.postValue(Loading(loadingType))
            try {
                val response = RetrofitClient.apiService.search2(keywords = keywords, page = page)
                if (response.code != 0) {
                    Log.w(TAG, "API状态码 code=${response.code}, msg=${response.msg}")
                    _uiState.postValue(Error(loadingType))
                    return@launch
                }
                _uiState.postValue(Success(response.data!!, loadingType))
            } catch (e: Exception) {
                _uiState.postValue(Error(loadingType))
                if (_page.value!! > 1) _page.postValue(page-1)     // 出错了重置页数
                Log.w(TAG, "fetchData error: ", e)
            }
        }
    }

    companion object {
        private const val TAG: String = "SearchViewModel"
    }
}