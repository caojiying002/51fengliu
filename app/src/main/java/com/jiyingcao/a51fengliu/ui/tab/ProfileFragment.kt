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
import com.jiyingcao.a51fengliu.App
import com.jiyingcao.a51fengliu.api.RetrofitClient
import com.jiyingcao.a51fengliu.api.response.Profile
import com.jiyingcao.a51fengliu.repository.UserRepository
import com.jiyingcao.a51fengliu.viewmodel.ProfileIntent
import com.jiyingcao.a51fengliu.viewmodel.ProfileState
import com.jiyingcao.a51fengliu.viewmodel.ProfileViewModel
import com.jiyingcao.a51fengliu.viewmodel.ProfileViewModelFactory
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.data.TokenManager
import com.jiyingcao.a51fengliu.navigation.LoginInterceptor
import com.jiyingcao.a51fengliu.ui.FavoriteActivity
import com.jiyingcao.a51fengliu.ui.auth.AuthActivity
import com.jiyingcao.a51fengliu.ui.PostInfoActivity
import com.jiyingcao.a51fengliu.ui.dialog.LoadingDialog
import com.jiyingcao.a51fengliu.ui.dialog.ConfirmDialog
import com.jiyingcao.a51fengliu.util.dataStore
import com.jiyingcao.a51fengliu.util.showToast
import com.jiyingcao.a51fengliu.viewmodel.LogoutEffect
import kotlinx.coroutines.flow.collectLatest

class ProfileFragment : Fragment() {
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

    private var loadingDialog: LoadingDialog? = null

    private lateinit var loginInterceptor: LoginInterceptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViewModel()
        // 初始化登录拦截器
        loginInterceptor = LoginInterceptor().apply {
            register(this@ProfileFragment)
        }
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

    override fun onStart() {
        super.onStart()
        viewModel.setUIVisibility(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setUIVisibility(false)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(
                UserRepository.getInstance(RetrofitClient.apiService),
                TokenManager.getInstance(App.INSTANCE.dataStore)
            )
        )[ProfileViewModel::class.java]
    }

    private fun setupFlowCollectors() {
        // 观察登录状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoggedIn.collect { isLoggedIn ->
                when (isLoggedIn) {
                    true -> showLoggedInUI()
                    false -> showLoggedOutUI()
                    null -> { /* 初始状态，可以显示加载中 */ }
                }
            }
        }

        // 观察个人信息状态
        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.state.collect { state ->
                when (state) {
                    is ProfileState.Init -> { /* 初始状态，可以显示加载中 */ }
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

        lifecycleScope.launch {
            viewModel.effect.collectLatest { effect ->
                when (effect) {
                    is LogoutEffect.ShowLoadingDialog -> {
                        showLoadingDialog {
                            viewModel.processIntent(ProfileIntent.CancelLogout)
                        }
                    }
                    is LogoutEffect.DismissLoadingDialog -> { dismissLoadingDialog() }
                    is LogoutEffect.ShowToast -> { context?.showToast(effect.message) }
                }
            }
        }
    }

    private fun setupViews() {
        profileInfo = binding.profileInfo.root
        profileError = binding.profileError.root
        profileLoading = binding.profileLoading.root
    }

    private fun setupClickListeners() {
        binding.profileError.clickRetry.setOnClickListener {
            viewModel.processIntent(ProfileIntent.LoadProfile)    // 如果有不同的UI样式，需要定义新Intent类型 ProfileIntent.Retry
        }
        binding.profileInfo.refreshPrompt.setOnClickListener {
            viewModel.processIntent(ProfileIntent.LoadProfile)  // 如果有不同的UI样式，需要定义新Intent类型 ProfileIntent.Refresh
        }
        binding.profileNotLogin.clickLogin.setOnClickListener {
            startActivity(AuthActivity.createIntent(requireContext()))
        }
        binding.tvLogout.setOnClickListener {
            ConfirmDialog.newInstance(
                message = getString(R.string.logout_confirmation_message),
                positiveButtonText = getString(R.string.quit),
                negativeButtonText = getString(R.string.cancel)
            ).setOnConfirmDialogListener(object : ConfirmDialog.OnConfirmDialogListener {
                override fun onConfirm() {
                    viewModel.processIntent(ProfileIntent.Logout)
                }
            }).show(parentFragmentManager, ConfirmDialog.TAG)
        }
        binding.tvPostInfo.setOnClickListener {
            loginInterceptor.execute {
                startActivity(PostInfoActivity.createIntent(requireContext()))
            }
        }
        binding.tvMyFavorite.setOnClickListener {
            loginInterceptor.execute {
                startActivity(FavoriteActivity.createIntent(requireContext()))
            }
        }
    }

    private fun showLoggedOutUI() {
        binding.profileNotLogin.root.isVisible = true
        binding.profileInfoContainer.isVisible = false
        binding.tvLogout.isVisible = false

        // 虽然隐藏了，最好也清除一下之前的个人信息显示
        binding.profileInfo.apply {
            usernameText.text = ""
            tvPoints.text = "0"
            tvMessages.text = "0"
            membershipStatus.text = getString(R.string.normal_member)
        }
    }

    private fun showLoggedInUI() {
        binding.profileInfoContainer.isVisible = true
        binding.profileNotLogin.root.isVisible = false
        binding.tvLogout.isVisible = true
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
                membershipStatus.text = getString(R.string.vip_member)
                membershipStatus.setTextColor(resources.getColor(R.color.primary, context?.theme))

                permanentMemberGroup.isVisible = true
                normalMemberGroup.isVisible = false

                // TODO 根据expiredAt显示会员到期时间，永久会员显示“永久”
                permanentStatus.text = getString(R.string.permanent)
            } else {
                titleText.text = getString(R.string.normal_welcome)
                membershipStatus.text = getString(R.string.normal_member)
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

    override fun onDestroy() {
        super.onDestroy()
        dismissLoadingDialog()
    }

    private fun showLoadingDialog(onCancelListener: (() -> Unit)? = null) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog().apply {
                setOnCancelListener(onCancelListener)
            }
        }
        if (loadingDialog?.isVisible != true) {
            loadingDialog?.showNow(parentFragmentManager, LoadingDialog.TAG)
        }
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismissAllowingStateLoss()
    }
}