package com.gomson.tryangle.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class LayerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    :FrameLayout(context, attrs, defStyleAttr) {


    fun removeAllViewsWithout(view: View) {
        var index = -1
        for (i in 0 until childCount) {
            if (getChildAt(i) == view) {
                index = i
                break
            }
        }

        removeViews(0, index)
        removeViews(1, childCount - index - 1)
    }
}