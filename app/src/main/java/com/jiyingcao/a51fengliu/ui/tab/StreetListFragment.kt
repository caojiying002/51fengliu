package com.jiyingcao.a51fengliu.ui.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jiyingcao.a51fengliu.R
import com.jiyingcao.a51fengliu.util.AppLogger

class StreetListFragment : Fragment() {

    private lateinit var sort: String
    private var index: Int = 0
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sort = arguments?.getString(ARG_SORT) ?: "publish"
        index = arguments?.getInt(ARG_INDEX) ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_street_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textView = view.findViewById(R.id.textView)
        
        val displayText = "StreetListFragment\nSort: $sort\nIndex: $index"
        textView.text = displayText
        
        AppLogger.d(TAG, "StreetListFragment created with sort: $sort, index: $index")
    }

    override fun onResume() {
        super.onResume()
        AppLogger.d(TAG, "StreetListFragment ($sort) is now visible")
    }

    override fun onPause() {
        super.onPause()
        AppLogger.d(TAG, "StreetListFragment ($sort) is now invisible")
    }

    companion object {
        private const val ARG_SORT = "sort"
        private const val ARG_INDEX = "index"
        private const val TAG = "StreetListFragment"
        
        fun newInstance(sort: String, index: Int): StreetListFragment {
            return StreetListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SORT, sort)
                    putInt(ARG_INDEX, index)
                }
            }
        }
    }
}