package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jiyingcao.a51fengliu.databinding.FragmentMerchantBinding

class MerchantFragment : Fragment() {
    private var _binding: FragmentMerchantBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMerchantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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