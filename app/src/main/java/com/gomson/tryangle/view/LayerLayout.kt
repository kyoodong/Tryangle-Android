package com.gomson.tryangle.view

import android.content.Context
import android.graphics.*
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


    override fun removeAllViews() {
        lineList.clear()
        areaList.clear()

        super.removeAllViews()
    }

    fun removeAllViewsWithout(view: View) {
        super.removeAllViews()
        addView(view)
    }

    fun createImageView(component: ObjectComponent, color: Boolean = false): ImageView {
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

        val layerImage = (if (color) component.layer.colorLayeredImage else component.layer.layeredImage)
            ?: return imageView

        canvas.drawBitmap(layerImage, null, Rect(0, 0, bitmap.width, bitmap.height), Paint())
        imageView.setImageBitmap(bitmap)
        return imageView
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)

        val canvas = canvas
            ?: return

        paint.strokeWidth = 6f
        for (line in lineList) {
            paint.color = line.color
            canvas.drawLine(line.startPoint.x.toFloat(), line.startPoint.y.toFloat(),
                line.endPoint.x.toFloat(), line.endPoint.y.toFloat(), paint)
        }

        paint.textAlign = Paint.Align.CENTER
        for (area in areaList) {
            paint.color = area.color
            canvas.drawRect(
                area.leftTop.x.toFloat(),
                area.leftTop.y.toFloat(),
                area.rightBottom.x.toFloat(),
                area.rightBottom.y.toFloat(),
                paint)

            val text = area.text
            if (text != null) {
                paint.color = Color.WHITE
                paint.textSize = 5f
                canvas.drawText(text,
                    (area.leftTop.x + area.getWidth() / 2).toFloat(),
                    (area.leftTop.y + area.getHeight() / 2).toFloat(),
                    paint
                )
            }
        }
    }
}