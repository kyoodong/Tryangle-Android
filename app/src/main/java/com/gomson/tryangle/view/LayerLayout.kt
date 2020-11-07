package com.gomson.tryangle.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.component.ObjectComponent

class LayerLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    :FrameLayout(context, attrs, defStyleAttr) {

    val lineList = ArrayList<Line>()
    val areaList = ArrayList<Area>()
    val paint = Paint()


    fun removeAllViewsWithout(view: View) {
        removeAllViews()
        addView(view)
    }

    fun createImageView(component: ObjectComponent): ImageView {
        val imageView = ImageView(context)
        val layoutWidth = width
        val layoutHeight = height
        val width = component.roi.getWidth() * layoutWidth / 640
        val height = component.roi.getHeight() * layoutHeight / 640

        imageView.x = (component.roi.getCenterPoint().x.toFloat() * layoutWidth / 640) - width / 2
        imageView.y = (component.roi.getCenterPoint().y.toFloat() * layoutHeight / 640) - height / 2
        imageView.layoutParams = ViewGroup.LayoutParams(width, height)
        val bitmap = Bitmap.createBitmap(component.roi.getWidth(), component.roi.getHeight(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val layerImage = component.layer.layeredImage
            ?: return imageView

        canvas.drawBitmap(layerImage, null, Rect(0, 0, bitmap.width, bitmap.height), Paint())
        imageView.setImageBitmap(bitmap)
        return imageView
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val canvas = canvas
            ?: return

        for (line in lineList) {
            paint.color = line.color
            canvas.drawLine(line.startPoint.x.toFloat(), line.startPoint.y.toFloat(),
                line.endPoint.x.toFloat(), line.endPoint.y.toFloat(), paint)
        }

        for (area in areaList) {
            paint.color = area.color
            canvas.drawRect(
                area.leftTop.x.toFloat(),
                area.leftTop.y.toFloat(),
                area.rightBottom.x.toFloat(),
                area.rightBottom.y.toFloat(),
                paint)
        }
    }
}