package com.gomson.tryangle

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.camera.view.PreviewView


class GridLinesView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var grid = true
    var paint:Paint = Paint()

    override fun onDraw(canvas: Canvas) {
        Log.e("onDraw",grid.toString())
        if (grid) {
            //  Find Screen size first
            val metrics: DisplayMetrics = Resources.getSystem().getDisplayMetrics()
            val screenWidth = metrics.widthPixels
//            val screenHeight = (metrics.heightPixels * 0.9).toInt()
            val screenHeight = height

            //  Set paint option
            paint.setAntiAlias(true)
            paint.setStrokeWidth(3F)
            paint.setStyle(Paint.Style.STROKE)
            paint.setColor(Color.argb(255, 255, 255, 255))
            canvas.drawLine((screenWidth / 3 * 2).toFloat(), 0F,
                (screenWidth / 3 * 2).toFloat(), screenHeight.toFloat(), paint)
            canvas.drawLine((screenWidth / 3).toFloat(), 0F, (screenWidth / 3).toFloat(),
                screenHeight.toFloat(), paint)
            canvas.drawLine(0F,
                (screenHeight / 3 * 2).toFloat(), screenWidth.toFloat(),
                (screenHeight / 3 * 2).toFloat(), paint)
            canvas.drawLine(0F, (screenHeight / 3).toFloat(),
                screenWidth.toFloat(), (screenHeight / 3).toFloat(), paint)
        }
    }
}