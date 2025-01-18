package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.data.MockTokenManager
import com.jiyingcao.a51fengliu.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.jiyingcao.a51fengliu.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFlowCollectors()
        setupClickListeners()

        if (MockTokenManager.isLoggedIn) {
            viewModel.fetchProfile()
        } else {
            showLoginState()
        }
    }

    private fun setupFlowCollectors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profile.collectLatest { profile ->
                profile?.let {
                    with(binding) {
                        tvUsername.text = it.name
                        tvEmail.text = it.email
                        // Set other profile fields
                    }
                    showProfileState()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.isVisible = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            // Handle login click
            MockTokenManager.isLoggedIn = true
            viewModel.fetchProfile()
        }

        binding.btnRegister.setOnClickListener {
            // Handle register click
        }

        binding.btnLogout.setOnClickListener {
            MockTokenManager.isLoggedIn = false
            showLoginState()
        }
    }

    private fun showLoginState() {
        with(binding) {
            groupProfile.isVisible = false
            groupAuth.isVisible = true
        }
    }

    private fun showProfileState() {
        with(binding) {
            groupProfile.isVisible = true
            groupAuth.isVisible = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment 变为可见时的逻辑
    }

    override fun onPause() {
        super.onPause()
        // Fragment 变为不可见时的逻辑
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}