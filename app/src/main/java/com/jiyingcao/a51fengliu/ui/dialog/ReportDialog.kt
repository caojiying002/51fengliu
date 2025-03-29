package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.databinding.DialogReportBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.util.showToast
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ReportDialog : DialogFragment() {
    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!
    
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var reportTitle: String = ""
    private var isUploading = false
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            uploadImage(uri)
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReportBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set dialog width to match parent with margins
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        setupUI()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // Set report title if provided
        if (reportTitle.isNotEmpty()) {
            binding.etReportTitle.setText(reportTitle)
        }
    }
    
    private fun setupClickListeners() {
        // Close button
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        
        // Image upload
        binding.tvUploadImage.setOnClickListener {
            if (!isUploading) {
                openPhotoPicker()
            }
        }
        
        // Delete image
        binding.ivDeleteImage.setOnClickListener {
            resetUploadUI()
        }
        
        // Confirm report button
        binding.btnConfirmReport.setOnClickListener {
            val reason = binding.etReportReason.text.toString().trim()
            
            if (reason.isEmpty()) {
                requireContext().showToast("请输入举报原因")
                return@setOnClickListener
            }
            
            // TODO: Implement report submission when API is ready
            requireContext().showToast("举报已提交，感谢您的反馈")
            dismiss()
        }
    }
    
    private fun openPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    
    private fun uploadImage(uri: Uri) {
        isUploading = true
        binding.tvUploadImage.isEnabled = false
        
        // Show loading state
        showLoadingState()
        
        // Convert URI to File
        lifecycleScope.launch {
            try {
                val file = convertUriToFile(uri)
                if (file != null) {
                    uploadFileToServer(file)
                } else {
                    requireContext().showToast("图片处理失败")
                    resetUploadUI()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("图片上传失败")
                resetUploadUI()
            }
        }
    }
    
    private fun showLoadingState() {
        binding.tvUploadImage.text = "上传中..."
        binding.tvUploadImage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
    
    private suspend fun uploadFileToServer(file: File) {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        try {
            val response = RetrofitClient.apiService.postUpload(body)
            if (response.isSuccessful()) {
                response.data?.let { url ->
                    uploadedImageUrl = url
                    displayUploadedImage(url)
                }
            } else {
                requireContext().showToast("上传失败: ${response.msg}")
                resetUploadUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            requireContext().showToast("上传失败，请稍后重试")
            resetUploadUI()
        } finally {
            isUploading = false
            binding.tvUploadImage.isEnabled = true
        }
    }
    
    private fun displayUploadedImage(relativeUrl: String) {
        val fullUrl = BASE_IMAGE_URL + relativeUrl
        
        // Show the uploaded image layout
        binding.layoutUploadedImage.visibility = View.VISIBLE
        binding.tvUploadImage.visibility = View.GONE
        
        // Load the image with Glide
        GlideApp.with(requireContext())
            .load(fullUrl)
            .centerCrop()
            .into(binding.ivUploadedImage)
    }
    
    private fun resetUploadUI() {
        // Reset data
        selectedImageUri = null
        uploadedImageUrl = null
        isUploading = false
        
        // Reset UI
        binding.tvUploadImage.apply {
            text = "上传图片"
            setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_upload_photo, 0, 0)
            visibility = View.VISIBLE
            isEnabled = true
        }
        binding.layoutUploadedImage.visibility = View.GONE
    }
    
    private fun convertUriToFile(uri: Uri): File? {
        val context = requireContext()
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
    
    fun setReportTitle(title: String) {
        this.reportTitle = title
    }
    
    companion object {
        const val TAG = "ReportDialog"
        
        fun newInstance(reportTitle: String): ReportDialog {
            return ReportDialog().apply {
                setReportTitle(reportTitle)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}