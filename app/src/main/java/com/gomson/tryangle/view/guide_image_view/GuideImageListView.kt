package com.gomson.tryangle.view.guide_image_view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gomson.tryangle.R
import kotlinx.android.synthetic.main.view_guide_image_list.view.*

class GuideImageListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0)
    :LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val adapter: GuideImageAdapter = GuideImageAdapter(context)

    init {
        inflate(context, R.layout.view_guide_image_list, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    fun getAdapter(): GuideImageAdapter {
        return adapter
    }
}