package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jiyingcao.a51fengliu.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.repository.UserRepository
import com.jiyingcao.a51fengliu.util.FirstResumeLifecycleObserver
import com.jiyingcao.a51fengliu.viewmodel.ProfileIntent
import com.jiyingcao.a51fengliu.viewmodel.ProfileState
import com.jiyingcao.a51fengliu.viewmodel.ProfileViewModel
import com.jiyingcao.a51fengliu.viewmodel.ProfileViewModelFactory
import com.jiyingcao.a51fengliu.R

class ProfileFragment : Fragment(),
    FirstResumeLifecycleObserver.FirstResumeListener {
    private var _binding: FragmentProfileBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    /** 等价于binding.profileInfo.root */
    private lateinit var profileInfo: View
    /** 等价于binding.profileError.root */
    private lateinit var profileError: View
    /** 等价于binding.profileLoading.root */
    private lateinit var profileLoading: View

    private lateinit var viewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 添加第一次 onResume 事件监听器
        lifecycle.addObserver(FirstResumeLifecycleObserver(this))
        setupViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupFlowCollectors()
        setupClickListeners()
    }

    override fun onFirstResume(isRecreate: Boolean) {
        // 重新创建时不加载数据
        if (!isRecreate) {
            // 第一次 onResume 事件发生时加载数据
            viewModel.processIntent(ProfileIntent.LoadProfile)
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                UserRepository.getInstance(RetrofitClient.apiService)
            )
        )[ProfileViewModel::class.java]
    }

    private fun setupFlowCollectors() {
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                when (state) {
                    is ProfileState.Init -> {
                        // 初始化应该显示什么内容，还是什么都不显示？
                    }
                    is ProfileState.Loading -> {
                        showLoadingView()
                    }
                    is ProfileState.Success -> {
                        showContentView()
                        updateUI(state.profile)
                    }
                    is ProfileState.Error -> {
                        showErrorView(state.message)
                    }
                }
            }
//            }
        }
    }

    private fun setupViews() {
        profileInfo = binding.profileInfo.root
        profileError = binding.profileError.root
        profileLoading = binding.profileLoading.root
    }

    private fun setupClickListeners() {
        binding.profileError.clickRetry.setOnClickListener {
            viewModel.processIntent(ProfileIntent.Retry)
        }
        binding.profileInfo.refreshPrompt.setOnClickListener {
            viewModel.processIntent(ProfileIntent.Refresh)
        }
    }

    private fun showLoginState() {

    }

    private fun showProfileState() {

    }

    private fun showLoadingView() {
        profileLoading.visibility = View.VISIBLE

        // 这里必须用INVISIBLE而不是GONE，因为profile_info决定了profile_loading和profile_error的高度
        profileInfo.visibility = View.INVISIBLE
        // profile_loading和profile_error用INVISIBLE和GONE都可以
        profileError.visibility = View.GONE
    }

    private fun showErrorView(message: String) {
        profileError.visibility = View.VISIBLE
        binding.profileError.tvError.text = message

        // 这里必须用INVISIBLE而不是GONE，因为profile_info决定了profile_loading和profile_error的高度
        profileInfo.visibility = View.INVISIBLE
        // profile_loading和profile_error用INVISIBLE和GONE都可以
        profileLoading.visibility = View.GONE
    }

    private fun showContentView() {
        profileInfo.visibility = View.VISIBLE

        // profile_loading和profile_error用INVISIBLE和GONE都可以
        profileLoading.visibility = View.GONE
        profileError.visibility = View.GONE
    }

    private fun updateUI(profile: Profile) {
        binding.profileInfo.apply {
            usernameText.text = profile.name
            tvPoints.text = profile.score
            tvMessages.text = profile.publishedInfoCount

            if (profile.isVip == true) {
                titleText.text = getString(R.string.vip_welcome)
                membershipStatus.text = "VIP会员"
                membershipStatus.setTextColor(resources.getColor(R.color.text_strong, context?.theme))

                permanentMemberGroup.isVisible = true
                normalMemberGroup.isVisible = false

                // TODO 根据expiredAt显示会员到期时间，永久会员显示“永久”
                permanentStatus.text = "永久"
            } else {
                titleText.text = getString(R.string.normal_welcome)
                membershipStatus.text = "普通会员"
                membershipStatus.setTextColor(resources.getColor(R.color.text_content, context?.theme))

                permanentMemberGroup.isVisible = false
                normalMemberGroup.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}