package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.RecordInfo
import com.jiyingcao.a51fengliu.domain.exception.BusinessException
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class DetailState {
    object Init : DetailState()
    object Loading : DetailState()
    data class Success(val record: RecordInfo) : DetailState()
    data class Error(val message: String) : DetailState()
}

sealed class DetailIntent {
    object LoadDetail : DetailIntent()
    object Refresh : DetailIntent()
    object Favorite : DetailIntent()
    object Unfavorite : DetailIntent()
}

sealed class DetailEffect {
    object ShowLoadingDialog : DetailEffect()
    object DismissLoadingDialog : DetailEffect()
    object FinishRefresh : DetailEffect()
    data class ShowToast(val message: String) : DetailEffect()
}

class DetailViewModel2(
    private val infoId: String,
    private val repository: RecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow<DetailState>(DetailState.Init)
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effect = Channel<DetailEffect>()
    val effect = _effect.receiveAsFlow()

    fun processIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.LoadDetail -> loadDetail()
            is DetailIntent.Refresh -> refresh()
            is DetailIntent.Favorite -> favorite()
            is DetailIntent.Unfavorite -> unfavorite()
        }
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            repository.getDetail(infoId)
                .collect { result ->
                    result.onSuccess { record ->
                        // 处理成功情况
                        _state.value = DetailState.Success(record)
                    }.onFailure { e ->
                        // 处理错误情况
                        var errMsg = e.toUserFriendlyMessage()
                        _state.value = DetailState.Error(errMsg)
                    }
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            repository.getDetail(infoId)
                .collect { result ->
                    result.onSuccess { record ->
                        _state.value = DetailState.Success(record)
                        _effect.send(DetailEffect.FinishRefresh)
                    }.onFailure { e ->
                        var errMsg = e.toUserFriendlyMessage()
                        _effect.send(DetailEffect.FinishRefresh)
                        _effect.send(DetailEffect.ShowToast(errMsg))
                    }
                }
        }
    }

    private fun favorite() {}
    private fun unfavorite() {}
}

class DetailViewModel2Factory(
    private val infoId: String,
    private val repository: RecordRepository
): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel2::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel2(infoId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}