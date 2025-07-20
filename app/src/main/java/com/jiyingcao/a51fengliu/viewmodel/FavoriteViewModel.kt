package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.PageData
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus

/**
 * 收藏页面的分页数据源实现
 */
private class FavoriteDataSource(
    private val repository: RecordRepository
) : PagingDataSource<RecordInfo> {
    override suspend fun loadPage(page: Int, params: Map<String, Any>?): Result<PageData<RecordInfo>?> {
        return repository.getFavorites(page).first()
    }
}

/**
 * 收藏页面Intent - 继承通用分页Intent
 */
sealed interface FavoriteIntent : BasePagingIntent {
    // 可以在这里添加收藏页面特有的Intent
}

class FavoriteViewModel(
    private val repository: RecordRepository
) : BaseViewModel() {
    
    // 使用组合模式 - 核心分页逻辑委托给PagingStateManager
    private val dataSource = FavoriteDataSource(repository)
    private val pagingManager = PagingStateManager(
        dataSource = dataSource,
        scope = viewModelScope.plus(remoteLoginCoroutineContext),
        handleFailure = ::handleFailure
    )
    
    // 暴露分页状态给UI层
    val uiState: StateFlow<PagingUiState<RecordInfo>> = pagingManager.uiState
    
    // 便捷属性 - 委托给分页状态
    val records: StateFlow<List<RecordInfo>> = uiState.map { it.items }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun processIntent(intent: BasePagingIntent) {
        // 直接转发给分页管理器处理
        pagingManager.processIntent(intent)
    }
    
    override fun onCleared() {
        super.onCleared()
        pagingManager.clear()
    }

    companion object {
        private const val TAG: String = "FavoriteViewModel"
    }
}

class FavoriteViewModelFactory(
    private val repository: RecordRepository = RecordRepository.getInstance(RetrofitClient.apiService)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoriteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}