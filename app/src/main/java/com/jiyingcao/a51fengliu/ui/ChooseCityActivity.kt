package com.jiyingcao.a51fengliu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiyingcao.a51fengliu.databinding.TitleBarRecyclerViewBinding
import com.jiyingcao.a51fengliu.ui.adapter.CityAdapter
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.administrativeDivisions
import com.jiyingcao.a51fengliu.util.showToast

class ChooseCityActivity: BaseActivity() {
    private lateinit var binding: TitleBarRecyclerViewBinding

    private lateinit var cityAdapter: CityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TitleBarRecyclerViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cityAdapter = CityAdapter().apply {
            setOnItemClickListener { _, _, position ->
                this.getItem(position)?.let {
                    showToast("code = ${it.key}")
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("CITY_CODE", it.key)
                    })
                    finish()
                }
            }

            submitList(administrativeDivisions.entries.toList())
        }
        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = cityAdapter
        }
    }

    companion object {
        private const val TAG = "ChooseCityActivity"

        @JvmStatic
        fun createIntent(context: Context) =
            Intent(context, ChooseCityActivity::class.java).apply {
                // putExtra("ITEM_DATA", itemData)
            }

        @JvmStatic
        fun start(context: Context) {
            context.startActivity(createIntent(context))
        }
    }
}