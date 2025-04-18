package com.jiyingcao.a51fengliu.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.jiyingcao.a51fengliu.databinding.FragmentRegisterBinding
import com.jiyingcao.a51fengliu.ui.base.BaseFragment

class RegisterFragment : BaseFragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleBar.titleBarBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 实现注册逻辑
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}