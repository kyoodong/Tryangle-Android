package com.gomson.tryangle.photo

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View

class CropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var image:Drawable
    private var scaleGestureDetector:ScaleGestureDetector
    var mScaleFactor = 1f

    init{
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun setImage(image:Drawable){
        this.image = image
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.save()
//        canvas?.translate(mPosX, mPosY);
        canvas?.scale(mScaleFactor, mScaleFactor);
        if (canvas != null) {
            image.draw(canvas)
        }
        canvas?.restore()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        return true
    }


    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f))
            invalidate()
            return true
        }
    }
}

