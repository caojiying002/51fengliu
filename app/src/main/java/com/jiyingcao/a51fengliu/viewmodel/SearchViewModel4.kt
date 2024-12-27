package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.PageData
import kotlinx.coroutines.flow.*

enum class LoadingType4 {
    NONE,
    FULL_SCREEN,
    PULL_TO_REFRESH,
    PAGINATION,
    DIALOG
}

data class SearchRequest(
    val keywords: String,
    val city: String,
    val page: Int,
    val shouldClearList: Boolean,
    val loadingType: LoadingType4
)

data class SearchState(
    val results: PageData? = null,
    val loadingType: LoadingType4 = LoadingType4.NONE,
    val error: String? = null,
    val shouldClearList: Boolean = false
)

class SearchViewModel4 : ViewModel() {
    private val _searchRequest = MutableStateFlow(
        SearchRequest("", "", 1, false, LoadingType4.NONE)
    )

    private val _searchState = MutableStateFlow(SearchState())
    val searchState = _searchState.asStateFlow()

    init {
        _searchRequest
            .debounce(300)
            .flatMapLatest { request ->
                flow {
                    emit(SearchState(loadingType = request.loadingType, shouldClearList = request.shouldClearList))
                    try {
                        val response = RetrofitClient.apiService.search4(request.keywords, request.city, request.page)
                        if (response.isSuccessful()) {
                            emit(SearchState(results = response.data, shouldClearList = request.shouldClearList))
                        } else {
                            Log.w(TAG, "API状态码 code=${response.code}, msg=${response.msg}")
                            // TODO 发送API错误和状态码到UI层
                        }

                    } catch (e: Exception) {
                        emit(SearchState(error = e.message, shouldClearList = request.shouldClearList))
                    }
                }
            }
            .onEach { _searchState.value = it }
            .launchIn(viewModelScope)
    }

    fun updateKeywords(keywords: String) {
        _searchRequest.update {
            it.copy(
                keywords = keywords,
                page = 1,
                shouldClearList = true,
                loadingType = LoadingType4.FULL_SCREEN
            )
        }
    }

    fun updateCity(city: String) {
        _searchRequest.update {
            it.copy(
                city = city,
                page = 1,
                shouldClearList = true,
                loadingType = LoadingType4.FULL_SCREEN
            )
        }
    }

    fun refreshList() {
        _searchRequest.update {
            it.copy(
                page = 1,
                shouldClearList = true,
                loadingType = LoadingType4.PULL_TO_REFRESH
            )
        }
    }

    fun nextPage() {
        _searchRequest.update {
            it.copy(
                page = it.page + 1,
                shouldClearList = false,
                loadingType = LoadingType4.PAGINATION
            )
        }
    }

    companion object {
        const val TAG: String = "SearchViewModel4"
    }
}