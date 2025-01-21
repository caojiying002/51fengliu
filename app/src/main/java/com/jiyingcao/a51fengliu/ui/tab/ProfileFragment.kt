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
    }

    private fun setupFlowCollectors() {

    }

    private fun setupClickListeners() {

    }

    private fun showLoginState() {

    }

    private fun showProfileState() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}