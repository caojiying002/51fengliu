package com.jiyingcao.a51fengliu.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.data.RemoteLoginManager.remoteLoginCoroutineContext
import com.jiyingcao.a51fengliu.domain.exception.toUserFriendlyMessage
import com.jiyingcao.a51fengliu.domain.model.ApiResult
import com.jiyingcao.a51fengliu.util.getErrorMessage
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class ReportUiState(
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

@HiltViewModel(assistedFactory = ReportViewModel.Factory::class)
class ReportViewModel @AssistedInject constructor(
    @Assisted private val infoId: String,
    private val repository: RecordRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
    
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
        if (_uiState.value.imageUploadState is ImageUploadState.Uploading) return

        _uiState.value = _uiState.value.copy(
            imageUploadState = ImageUploadState.Uploading(0)
        )
        
        viewModelScope.launch(remoteLoginCoroutineContext) {
            repository.uploadImage(file)
                .collect { result ->
                    result.mapCatching { requireNotNull(it) }
                        .fold(
                            onSuccess = { url ->
                                _uiState.value = _uiState.value.copy(
                                    uploadedImageUrl = url,
                                    imageUploadState = ImageUploadState.Success(url)
                                )
                                
                                _effect.send(ReportEffect.ShowToast("图片上传成功"))
                            },
                            onFailure = { e ->
                                val errorMessage = e.toUserFriendlyMessage()

                                _uiState.value = _uiState.value.copy(
                                    imageUploadState = ImageUploadState.Error(errorMessage)
                                )

                                _effect.send(ReportEffect.ShowToast("图片上传失败：$errorMessage"))
                            }
                        )
                }
        }
    }
    
    /**
     * 清除上传的图片
     */
    private fun clearUploadedImage() {
        _uiState.value = _uiState.value.copy(
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

        _uiState.value = _uiState.value.copy(
            reason = reason,
            submitState = SubmitState.Submitting
        )

        viewModelScope.launch(remoteLoginCoroutineContext) {
            // 使用新状态中的图片URL
            val picture = _uiState.value.uploadedImageUrl.orEmpty()
            repository.report(infoId, reason, picture)
                .collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            // 举报成功
                            _uiState.value = _uiState.value.copy(
                                submitState = SubmitState.Success
                            )

                            _effect.send(ReportEffect.ShowToast("举报已提交，感谢您的反馈"))
                            _effect.send(ReportEffect.DismissDialog)
                        }
                        is ApiResult.ApiError -> {
                            // 检查是否为字段验证错误 (data 是 Map)
                            val errorMessage = if (result.data is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                val fieldErrors = result.data as Map<String, String>
                                // 优先处理内容错误，其次处理图片错误，最后使用通用错误信息
                                fieldErrors["content"] ?: fieldErrors["picture"] ?: result.message
                            } else {
                                // 通用业务错误
                                result.message
                            }

                            _uiState.value = _uiState.value.copy(
                                submitState = SubmitState.Error(errorMessage)
                            )

                            _effect.send(ReportEffect.ShowToast("举报提交失败：$errorMessage"))
                        }
                        is ApiResult.NetworkError -> {
                            // 网络错误
                            val errorMessage = result.getErrorMessage("网络连接失败")
                            _uiState.value = _uiState.value.copy(
                                submitState = SubmitState.Error(errorMessage)
                            )

                            _effect.send(ReportEffect.ShowToast("举报提交失败：$errorMessage"))
                        }
                        is ApiResult.UnknownError -> {
                            // 未知错误
                            val errorMessage = result.getErrorMessage("未知错误")
                            _uiState.value = _uiState.value.copy(
                                submitState = SubmitState.Error(errorMessage)
                            )

                            _effect.send(ReportEffect.ShowToast("举报提交失败：$errorMessage"))
                        }
                    }
                }
        }
    }

    private fun convertUriToFileAndUpload(uri: Uri) {
        // 立即更新选择的URI
        _uiState.value = _uiState.value.copy(selectedImageUri = uri)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = convertUriToFile(uri)
                if (file != null) {
                    uploadImage(file)
                } else {
                    _effect.send(ReportEffect.ShowToast("图片处理失败"))

                    // 重置状态
                    _uiState.value = _uiState.value.copy(
                        imageUploadState = ImageUploadState.Error("图片处理失败")
                    )
                }
            } catch (e: Exception) {
                AppLogger.w("ReportViewModel", e)
                _effect.send(ReportEffect.ShowToast("图片处理失败"))
                
                // 重置状态
                _uiState.value = _uiState.value.copy(
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

    @AssistedFactory
    interface Factory {
        fun create(infoId: String): ReportViewModel
    }
}