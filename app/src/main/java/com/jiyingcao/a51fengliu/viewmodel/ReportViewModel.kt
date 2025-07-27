package com.jiyingcao.a51fengliu.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.api.response.ReportErrorData
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.ReportException
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// 举报状态的主State类
data class ReportState2(
    // 用户输入内容
    //val subject: String = "",  // 举报标题，如"温柔雨梦"
    val reason: String = "",   // 举报原因详情

    // 图片相关
    val selectedImageUri: Uri? = null,
    val uploadedImageUrl: String? = null,
    val imageUploadState: ImageUploadState = ImageUploadState.Idle,

    // 表单验证
    //val subjectError: String? = null,
    //val reasonError: String? = null,

    // 提交状态
    val submitState: SubmitState = SubmitState.Idle,

    // 通用状态
    //val isLoading: Boolean = false,
    //val error: String? = null
)

// 图片上传状态
sealed class ImageUploadState {
    object Idle : ImageUploadState()
    data class Uploading(val progress: Int) : ImageUploadState()
    data class Success(val imageUrl: String) : ImageUploadState()
    data class Error(val message: String) : ImageUploadState()
}

// 提交状态
sealed class SubmitState {
    object Idle : SubmitState()
    object Submitting : SubmitState()
    object Success/*(val reportId: String)*/ : SubmitState()
    data class Error(val message: String) : SubmitState()
}

@Deprecated("将被ReportState2替换，更符合MVI单一可信数据源原则")
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
    data class UploadImage(val uri: Uri) : ReportIntent()
    object ClearUploadedImage : ReportIntent()
    data class SubmitReport(val reason: String) : ReportIntent()
}

sealed class ReportEffect {
    data class ShowToast(val message: String) : ReportEffect()
    object DismissDialog : ReportEffect()
    //object NavigateBack : ReportEffect()
    //object OpenGallery : ReportEffect()
    //data class ShowError(val message: String) : ReportEffect()
    //object ScrollToError : ReportEffect()
}

class ReportViewModel(
    private val repository: RecordRepository,
    private val infoId: String
) : BaseViewModel() {

    private val _state2 = MutableStateFlow(ReportState2())
    val state2: StateFlow<ReportState2> = _state2.asStateFlow()

    @Deprecated("将被_state2替换")
    private val _state = MutableStateFlow<ReportState>(ReportState.Initial)
    val state: StateFlow<ReportState> = _state.asStateFlow()

    @Deprecated("将被ReportState2替换，更符合MVI单一可信数据源原则")
    private val _uploadedImageUrl = MutableStateFlow<String?>(null)
    val uploadedImageUrl: StateFlow<String?> = _uploadedImageUrl.asStateFlow()

    @Deprecated("将被ReportState2替换，更符合MVI单一可信数据源原则")
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _effect = Channel<ReportEffect>()
    val effect = _effect.receiveAsFlow()
    
    fun processIntent(intent: ReportIntent) {
        when (intent) {
            is ReportIntent.UploadImage -> convertUriToFileAndUpload(intent.uri)
            is ReportIntent.ClearUploadedImage -> clearUploadedImage()
            is ReportIntent.SubmitReport -> submitReport(intent.reason)
        }
    }
    
    /**
     * 上传图片
     * @param file 要上传的图片文件
     */
    private fun uploadImage(file: File) {
        if (_state2.value.imageUploadState is ImageUploadState.Uploading) return
        
        // 更新旧状态 (保持向后兼容)
        _isUploading.value = true
        _state.value = ReportState.Loading.UploadingImage
        
        // 更新新状态
        _state2.value = _state2.value.copy(
            imageUploadState = ImageUploadState.Uploading(0)
        )
        
        viewModelScope.launch(remoteLoginCoroutineContext) {
            repository.uploadImage(file)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .fold(
                            onSuccess = { url ->
                                // 更新旧状态
                                _uploadedImageUrl.value = url
                                _state.value = ReportState.Success.ImageUploaded(url)
                                
                                // 更新新状态
                                _state2.value = _state2.value.copy(
                                    uploadedImageUrl = url,
                                    imageUploadState = ImageUploadState.Success(url)
                                )
                                
                                _effect.send(ReportEffect.ShowToast("图片上传成功"))
                            },
                            onFailure = { e ->
                                if (!handleFailure(e)) {
                                    val errorMessage = e.toUserFriendlyMessage()
                                    
                                    // 更新旧状态
                                    _state.value = ReportState.Error.ImageUploadError(errorMessage)
                                    
                                    // 更新新状态
                                    _state2.value = _state2.value.copy(
                                        imageUploadState = ImageUploadState.Error(errorMessage)
                                    )
                                    
                                    _effect.send(ReportEffect.ShowToast("图片上传失败：$errorMessage"))
                                }
                            }
                        )
                    // 更新旧状态
                    _isUploading.value = false
                }
        }
    }
    
    /**
     * 清除上传的图片
     */
    private fun clearUploadedImage() {
        // 更新旧状态
        _uploadedImageUrl.value = null
        _state.value = ReportState.Initial
        
        // 更新新状态
        _state2.value = _state2.value.copy(
            selectedImageUri = null,
            uploadedImageUrl = null,
            imageUploadState = ImageUploadState.Idle
        )
    }
    
    /**
     * 提交举报
     * @param reason 举报原因
     */
    private fun submitReport(reason: String) {
        // TODO 如果图片上传中，提醒用户等待上传完成再提交举报

        // 更新旧状态
        _state.value = ReportState.Loading.SubmittingReport
        
        // 更新新状态
        _state2.value = _state2.value.copy(
            reason = reason,
            submitState = SubmitState.Submitting
        )
        
        viewModelScope.launch(remoteLoginCoroutineContext) {
            // 使用新状态中的图片URL
            val picture = _state2.value.uploadedImageUrl.orEmpty()
            repository.report(infoId, reason, picture)
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            // 更新旧状态
                            _state.value = ReportState.Success.ReportSubmitted
                            
                            // 更新新状态
                            _state2.value = _state2.value.copy(
                                submitState = SubmitState.Success
                            )
                            
                            _effect.send(ReportEffect.ShowToast("举报已提交，感谢您的反馈"))
                            _effect.send(ReportEffect.DismissDialog)
                        },
                        onFailure = { e ->
                            if (!handleFailure(e)) {
                                // 处理特定的举报错误
                                val errorMessage = if (e is ReportException && e.errorData is ReportErrorData) {
                                    // 优先处理内容错误，其次处理图片错误，最后使用通用错误信息
                                    e.errorData.content ?: e.errorData.picture ?: e.toUserFriendlyMessage()
                                } else {
                                    e.toUserFriendlyMessage()
                                }
                                
                                // 更新旧状态
                                _state.value = ReportState.Error.ReportSubmissionError(errorMessage)
                                
                                // 更新新状态
                                _state2.value = _state2.value.copy(
                                    submitState = SubmitState.Error(errorMessage)
                                )
                                
                                _effect.send(ReportEffect.ShowToast("举报提交失败：$errorMessage"))
                            }
                        }
                    )
                }
        }
    }

    private fun convertUriToFileAndUpload(uri: Uri) {
        // 立即更新选择的URI
        _state2.value = _state2.value.copy(selectedImageUri = uri)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = convertUriToFile(uri)
                if (file != null) {
                    uploadImage(file)
                } else {
                    _effect.send(ReportEffect.ShowToast("图片处理失败"))
                    
                    // 重置状态
                    _state2.value = _state2.value.copy(
                        imageUploadState = ImageUploadState.Error("图片处理失败")
                    )
                }
            } catch (e: Exception) {
                AppLogger.w("ReportViewModel", e)
                _effect.send(ReportEffect.ShowToast("图片处理失败"))
                
                // 重置状态
                _state2.value = _state2.value.copy(
                    imageUploadState = ImageUploadState.Error("图片处理失败: ${e.message}")
                )
            }
        }
    }

    // TODO 放到合适的Util中
    private fun convertUriToFile(uri: Uri): File? {
        val context = App.INSTANCE
        val tempFile = File(context.cacheDir, "upload_image_${System.currentTimeMillis()}.jpg")

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    class Factory(
        private val infoId: String,
        private val repository: RecordRepository = RecordRepository.getInstance()
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReportViewModel(repository, infoId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}