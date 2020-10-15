package com.gomson.tryangle

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView

/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitTextureView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            Log.d("measure wh", width.toString()+" "+(height).toString())
            Log.d("measure w rh", width.toString()+" "+(height * ratioWidth / ratioHeight).toString())
            Log.d("measure rw rh", ratioWidth.toString()+" "+ratioHeight)
            if (width <= height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
//                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }
}

