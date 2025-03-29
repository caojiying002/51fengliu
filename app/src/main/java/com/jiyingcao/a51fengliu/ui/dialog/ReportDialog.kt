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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.databinding.DialogReportBinding
import com.jiyingcao.a51fengliu.glide.BASE_IMAGE_URL
import com.jiyingcao.a51fengliu.glide.GlideApp
import com.jiyingcao.a51fengliu.repository.RecordRepository
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.ReportState
import com.jiyingcao.a51fengliu.viewmodel.ReportViewModel
import com.jiyingcao.a51fengliu.viewmodel.ReportViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ReportDialog : DialogFragment() {
    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!
    
    private var selectedImageUri: Uri? = null
    private var recordTitle: String = ""
    private lateinit var viewModel: ReportViewModel
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            convertUriToFileAndUpload(uri)
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
        
        // Setup ViewModel
        this.recordTitle = arguments?.getString(ARG_RECORD_TITLE) ?: ""
        val infoId = arguments?.getString(ARG_RECORD_ID) ?: ""
        val repository = RecordRepository.getInstance(RetrofitClient.apiService)
        
        viewModel = ViewModelProvider(
            this,
            ReportViewModelFactory(repository, infoId)
        )[ReportViewModel::class.java]
        
        // Set dialog width to match parent with margins
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        
        setupUI()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.etReportTitle.setText(recordTitle)
    }
    
    private fun setupClickListeners() {
        // Close button
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        
        // Image upload
        binding.tvUploadImage.setOnClickListener {
            if (viewModel.isUploading.value) {
                return@setOnClickListener
            }
            openPhotoPicker()
        }
        
        // Delete image
        binding.ivDeleteImage.setOnClickListener {
            viewModel.clearUploadedImage()
            resetUploadUI()
        }
        
        // Confirm report button
        binding.btnConfirmReport.setOnClickListener {
            val reason = binding.etReportReason.text.toString().trim()
            viewModel.submitReport(reason)
        }
    }
    
    private fun observeViewModel() {
        // Observe state changes
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ReportState.Initial -> {
                        // Initial state, nothing to do
                    }
                    is ReportState.UploadingImage -> {
                        showLoadingState()
                    }
                    is ReportState.ImageUploaded -> {
                        displayUploadedImage(state.relativeUrl)
                    }
                    is ReportState.SubmittingReport -> {
                        // Show loading state for report submission if needed
                    }
                    is ReportState.ReportSubmitted -> {
                        // Will be handled by effect
                    }
                    is ReportState.Error -> {
                        // Error will be shown as toast via effects
                        if (viewModel.uploadedImageUrl.value == null) {
                            resetUploadUI()
                        }
                    }
                }
            }
        }
        
        // Observe side effects
        lifecycleScope.launch {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is com.jiyingcao.a51fengliu.viewmodel.ReportEffect.ShowToast -> {
                        requireContext().showToast(effect.message)
                    }
                    is com.jiyingcao.a51fengliu.viewmodel.ReportEffect.DismissDialog -> {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private fun openPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    
    private fun convertUriToFileAndUpload(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = convertUriToFile(uri)
                if (file != null) {
                    viewModel.uploadImage(file)
                } else {
                    requireContext().showToast("图片处理失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireContext().showToast("图片处理失败")
            }
        }
    }
    
    private fun showLoadingState() {
        binding.tvUploadImage.text = "上传中..."
        binding.tvUploadImage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
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
    
    companion object {
        const val TAG = "ReportDialog"
        private const val ARG_RECORD_TITLE = "title"
        private const val ARG_RECORD_ID = "id"

        fun newInstance(title: String, id: String): ReportDialog {
            return ReportDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_RECORD_TITLE, title)
                    putString(ARG_RECORD_ID, id)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}