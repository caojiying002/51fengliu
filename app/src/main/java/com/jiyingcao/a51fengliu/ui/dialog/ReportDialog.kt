package com.jiyingcao.a51fengliu.ui.dialog

import android.app.Dialog
import android.graphics.Color
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
import com.jiyingcao.a51fengliu.databinding.DialogReportBinding
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.ReportEffect
import com.jiyingcao.a51fengliu.viewmodel.ReportIntent
import com.jiyingcao.a51fengliu.viewmodel.ReportViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable
import com.jiyingcao.a51fengliu.viewmodel.ImageUploadState
import com.jiyingcao.a51fengliu.viewmodel.SubmitState
import com.jiyingcao.a51fengliu.util.ImageLoader

class ReportDialog : DialogFragment() {
    private var _binding: DialogReportBinding? = null
    private val binding get() = _binding!!
    
    private var selectedImageUri: Uri? = null
    private var recordTitle: String = ""
    private lateinit var viewModel: ReportViewModel
    
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            viewModel.processIntent(ReportIntent.UploadImage(uri))
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
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
        
        viewModel = ViewModelProvider(
            this,
            ReportViewModel.Factory(infoId)
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
            // 使用新状态的imageUploadState进行检查
            val currentState = viewModel.state2.value.imageUploadState
            if (currentState is ImageUploadState.Uploading) {
                return@setOnClickListener
            }
            openPhotoPicker()
        }
        
        // Delete image
        binding.ivDeleteImage.setOnClickListener {
            viewModel.processIntent(ReportIntent.ClearUploadedImage)
        }
        
        // Confirm report button
        binding.btnConfirmReport.setOnClickListener {
            val reason = binding.etReportReason.text.toString().trim()
            viewModel.processIntent(ReportIntent.SubmitReport(reason))
        }
    }
    
    private fun observeViewModel() {
        // Observe new state changes
        lifecycleScope.launch {
            viewModel.state2.collect { state ->
                // 处理图片上传状态
                when (state.imageUploadState) {
                    is ImageUploadState.Idle -> {
                        // 闲置状态，重置UI
                        if (state.selectedImageUri == null) {
                            resetUploadUI()
                        }
                    }
                    is ImageUploadState.Uploading -> {
                        showLoadingState()
                    }
                    is ImageUploadState.Success -> {
                        state.uploadedImageUrl?.let { url ->
                            displayUploadedImage(url)
                        }
                    }
                    is ImageUploadState.Error -> {
                        // 错误会通过effects展示，如果没有已上传图片就重置UI
                        if (state.uploadedImageUrl == null) {
                            resetUploadUI()
                        }
                    }
                }

                // 处理提交状态
                when (state.submitState) {
                    is SubmitState.Idle -> {
                        updateReportButtonState(true)
                    }
                    is SubmitState.Submitting -> {
                        updateReportButtonState(false, "举报中...")
                    }
                    is SubmitState.Success -> {
                        updateReportButtonState(true)
                        // 关闭Dialog由 [ReportEffect.DismissDialog] 处理
                    }
                    is SubmitState.Error -> {
                        updateReportButtonState(true)
                    }
                }
            }
        }
        
        // Observe side effects
        lifecycleScope.launch {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is ReportEffect.ShowToast -> {
                        requireContext().showToast(effect.message)
                    }
                    is ReportEffect.DismissDialog -> {
                        dismiss()
                    }
                }
            }
        }
    }
    
    private fun openPhotoPicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    
    private fun showLoadingState() {
        binding.tvUploadImage.text = "上传中..."
        binding.tvUploadImage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
    }
    
    private fun displayUploadedImage(relativeUrl: String) {
        // Show the uploaded image layout
        binding.layoutUploadedImage.visibility = View.VISIBLE
        binding.tvUploadImage.visibility = View.GONE
        
        // Load the image with ImageLoader
        // TODO 能否使用本地文件显示
        ImageLoader.loadCenterCrop(
            imageView = binding.ivUploadedImage,
            url = relativeUrl
        )
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
    
    private fun updateReportButtonState(enabled: Boolean, text: String? = null) {
        binding.btnConfirmReport.apply {
            isEnabled = enabled
            if (text != null) {
                this.text = text
            } else {
                this.text = getString(R.string.confirm_report)
            }
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