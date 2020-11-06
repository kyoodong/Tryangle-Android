package com.gomson.tryangle.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class LayerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    :FrameLayout(context, attrs, defStyleAttr) {


    fun removeAllViewsWithout(view: View) {
        removeAllViews()
        addView(view)
    }
}