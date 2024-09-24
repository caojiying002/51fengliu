package com.jiyingcao.a51fengliu.ui.tab

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jiyingcao.a51fengliu.R

class DashboardFragment : Fragment() {
    private lateinit var contentView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentView = view.findViewById(R.id.contentView)
    }

    override fun onResume() {
        super.onResume()
        // Fragment 变为可见时的逻辑
    }

    override fun onPause() {
        super.onPause()
        // Fragment 变为不可见时的逻辑
    }

    @SuppressLint("SetTextI18n")
    private fun updateContent() {
        contentView.text = "Dashboard Fragment - Last updated: ${System.currentTimeMillis()}"
    }
}