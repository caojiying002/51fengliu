package com.jiyingcao.a51fengliu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.PageData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel2 : ViewModel() {
    private val _keywords = MutableStateFlow("")
    val keywords: StateFlow<String> = _keywords

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private var pendingPage: Int = 1

    private val _searchResults = MutableStateFlow<PageData?>(null)
    val searchResults: StateFlow<PageData?> = _searchResults

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        sealed class Loading : UiState() {
            object Initial : Loading()
            object Refresh : Loading()
            object Pagination : Loading()
        }
        sealed class Error : UiState() {
//            data class Initial(val error: ErrorEntity) : Error()
//            data class Refresh(val error: ErrorEntity) : Error()
//            data class Pagination(val error: ErrorEntity) : Error()
        }
    }

    fun setKeywords(newKeywords: String) {
        if (newKeywords != _keywords.value) {
            _keywords.value = newKeywords
            _currentPage.value = 0  // Reset to initial state
            pendingPage = 1         // Set pending page to first page
            //_searchResults.value = null  // Clear existing results
            triggerSearch()
        }
    }

    fun loadNextPage() {
        if (_uiState.value !is UiState.Loading) {   // TODO is能否判断子类？
            pendingPage = _currentPage.value + 1
            triggerSearch()
        }
    }

    fun refresh() {
        if (_uiState.value !is UiState.Loading) {
            // TODO _currentPage要改吗？
            pendingPage = 1
            //_searchResults.value = null  // Clear results on refresh
            triggerSearch()
        }
    }

    private fun triggerSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            performSearch(_keywords.value)
        }
    }

    private suspend fun performSearch(keywords: String/*, page: Int*/) {
        if (keywords.isBlank()) {
            _searchResults.value = null
            _uiState.value = UiState.Idle
            return
        }

        _uiState.value = when {
            _currentPage.value == 0 -> UiState.Loading.Initial
            pendingPage == 1 -> UiState.Loading.Refresh
            else -> UiState.Loading.Pagination
        }

        try {
            val response = RetrofitClient.apiService.search2(keywords, pendingPage)
            if (response.isSuccessful()) {
                _searchResults.value = response.data
                // TODO 增加一种空数据的状态
                _currentPage.value = pendingPage
                _uiState.value = UiState.Idle
            } else {
                Log.w(TAG, "API状态码 code=${response.code}, msg=${response.msg}")
                // TODO _uiState.value = UiState.Error
            }
        } catch (e: Exception) {
            /*val errorEntity = errorHandler.handleError(e)
            _uiState.value = when (_uiState.value) {
                is UiState.Loading.Initial -> UiState.Error.Initial(errorEntity)
                is UiState.Loading.Refresh -> UiState.Error.Refresh(errorEntity)
                is UiState.Loading.Pagination -> UiState.Error.Pagination(errorEntity)
                else -> UiState.Error.Initial(errorEntity) // Fallback
            }*/
            // pendingPage remains unchanged, allowing for retry
        }
    }

    companion object {
        private const val TAG: String = "SearchViewModel2"
    }
}