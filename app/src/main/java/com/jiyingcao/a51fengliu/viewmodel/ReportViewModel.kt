package com.jiyingcao.a51fengliu.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    object UploadingImage : ReportState()
    data class ImageUploaded(val relativeUrl: String) : ReportState()
    object SubmittingReport : ReportState()
    object ReportSubmitted : ReportState()
    data class Error(val message: String) : ReportState()
}

sealed class ReportEffect {
    data class ShowToast(val message: String) : ReportEffect()
    object DismissDialog : ReportEffect()
}

class ReportViewModel(
    private val repository: RecordRepository,
    private val infoId: String
) : ViewModel() {
    
    private val _state = MutableStateFlow<ReportState>(ReportState.Initial)
    val state: StateFlow<ReportState> = _state.asStateFlow()
    
    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _effect = Channel<ReportEffect>()
    val effect = _effect.receiveAsFlow()
    
    /**
     * 上传图片
     * @param file 要上传的图片文件
     */
    fun uploadImage(file: File) {
        if (_isUploading.value) return
        
        _isUploading.value = true
        _state.value = ReportState.UploadingImage
        
        viewModelScope.launch {
            repository.uploadImage(file)
                .collect { result ->
                    result.fold(
                        onSuccess = { url ->
                            if (url != null) {
                                _uploadedImageUrl.value = url
                                _state.value = ReportState.ImageUploaded(url)
                                _effect.send(ReportEffect.ShowToast("图片上传成功"))
                            } else {
                                _state.value = ReportState.Error("图片上传失败：服务器未返回URL")
                                _effect.send(ReportEffect.ShowToast("图片上传失败：服务器未返回URL"))
                            }
                        },
                        onFailure = { e ->
                            _state.value = ReportState.Error(e.toUserFriendlyMessage())
                            _effect.send(ReportEffect.ShowToast("图片上传失败：${e.toUserFriendlyMessage()}"))
                        }
                    )
                    _isUploading.value = false
                }
        }
    }
    
    /**
     * 清除上传的图片
     */
    fun clearUploadedImage() {
        _uploadedImageUrl.value = null
        _state.value = ReportState.Initial
    }
    
    /**
     * 提交举报
     * @param reason 举报原因
     */
    fun submitReport(reason: String) {
        if (reason.isEmpty()) {
            viewModelScope.launch {
                _effect.send(ReportEffect.ShowToast("请输入举报原因"))
            }
            return
        }
        
        _state.value = ReportState.SubmittingReport
        
        viewModelScope.launch {
            val picture = _uploadedImageUrl.value ?: ""
            repository.report(infoId, reason, picture)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            _state.value = ReportState.ReportSubmitted
                            _effect.send(ReportEffect.ShowToast("举报已提交，感谢您的反馈"))
                            _effect.send(ReportEffect.DismissDialog)
                        },
                        onFailure = { e ->
                            _state.value = ReportState.Error(e.toUserFriendlyMessage())
                            _effect.send(ReportEffect.ShowToast("举报提交失败：${e.toUserFriendlyMessage()}"))
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