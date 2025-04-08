package com.jiyingcao.a51fengliu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.api.response.ReportErrorData
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.ReportException
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class ReportState {
    object Initial : ReportState()
    sealed class Loading : ReportState() {
        object UploadingImage : Loading()
        object SubmittingReport : Loading()
    }
    sealed class Success : ReportState() {
        data class ImageUploaded(val relativeUrl: String) : Success()
        object ReportSubmitted : Success()
    }
    sealed class Error : ReportState() {
        data class ImageUploadError(val message: String) : Error()
        data class ReportSubmissionError(val message: String) : Error()
    }
}

sealed class ReportIntent {
    data class UploadImage(val file: File) : ReportIntent()
    object ClearUploadedImage : ReportIntent()
    data class SubmitReport(val reason: String) : ReportIntent()
}

sealed class ReportEffect {
    data class ShowToast(val message: String) : ReportEffect()
    object DismissDialog : ReportEffect()
}

class ReportViewModel(
    private val repository: RecordRepository,
    private val infoId: String
) : BaseViewModel() {
    
    private val _state = MutableStateFlow<ReportState>(ReportState.Initial)
    val state: StateFlow<ReportState> = _state.asStateFlow()
    
    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _effect = Channel<ReportEffect>()
    val effect = _effect.receiveAsFlow()
    
    fun processIntent(intent: ReportIntent) {
        when (intent) {
            is ReportIntent.UploadImage -> uploadImage(intent.file)
            is ReportIntent.ClearUploadedImage -> clearUploadedImage()
            is ReportIntent.SubmitReport -> submitReport(intent.reason)
        }
    }
    
    /**
     * 上传图片
     * @param file 要上传的图片文件
     */
    private fun uploadImage(file: File) {
        if (_isUploading.value) return
        
        _isUploading.value = true
        _state.value = ReportState.Loading.UploadingImage
        
        viewModelScope.launch(remoteLoginCoroutineContext) {
            repository.uploadImage(file)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .fold(
                            onSuccess = { url ->
                                _uploadedImageUrl.value = url
                                _state.value = ReportState.Success.ImageUploaded(url)
                                _effect.send(ReportEffect.ShowToast("图片上传成功"))
                            },
                            onFailure = { e ->
                                if (!handleFailure(e)) {
                                    _state.value = ReportState.Error.ImageUploadError(e.toUserFriendlyMessage())
                                    _effect.send(ReportEffect.ShowToast("图片上传失败：${e.toUserFriendlyMessage()}"))
                                }
                            }
                        )
                    _isUploading.value = false
                }
        }
    }
    
    /**
     * 清除上传的图片
     */
    private fun clearUploadedImage() {
        _uploadedImageUrl.value = null
        _state.value = ReportState.Initial
    }
    
    /**
     * 提交举报
     * @param reason 举报原因
     */
    private fun submitReport(reason: String) {
        /*if (reason.isEmpty()) {
            viewModelScope.launch {
                _effect.send(ReportEffect.ShowToast("请输入举报原因"))
            }
            return
        }*/
        
        _state.value = ReportState.Loading.SubmittingReport
        
        viewModelScope.launch(remoteLoginCoroutineContext) {
            val picture = _uploadedImageUrl.value.orEmpty()
            repository.report(infoId, reason, picture)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            _state.value = ReportState.Success.ReportSubmitted
                            _effect.send(ReportEffect.ShowToast("举报已提交，感谢您的反馈"))
                            _effect.send(ReportEffect.DismissDialog)
                        },
                        onFailure = { e ->
                            if (!handleFailure(e)) {
                                if (e is ReportException && e.errorData is ReportErrorData) {
                                    // 处理特定的举报错误
                                    _state.value = ReportState.Error.ReportSubmissionError(e.errorData.content?: e.toUserFriendlyMessage())
                                    _effect.send(ReportEffect.ShowToast(e.errorData.content?: "举报提交失败"))
                                } else {
                                    _state.value = ReportState.Error.ReportSubmissionError(e.toUserFriendlyMessage())
                                    _effect.send(ReportEffect.ShowToast("举报提交失败：${e.toUserFriendlyMessage()}"))
                                }
                            }
                        }
                    )
                }
        }
    }
}

class ReportViewModelFactory(
    private val repository: RecordRepository,
    private val infoId: String
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(repository, infoId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}