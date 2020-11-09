package com.gomson.tryangle.photo

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.gomson.tryangle.R
import com.gomson.tryangle.dpToPx

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
class ProgressWheelView : View {
    private val mCanvasClipBounds = Rect()
    private var mScrollingListener: ScrollingListener? = null
    private var mLastTouchedPosition = 0f
    private var mProgressLinePaint: Paint? = null
    private var mProgressMiddleLinePaint: Paint? = null
    private var mProgressLineWidth = 0
    private var mProgressLineHeight = 0
    private var mProgressLineMargin = 0
    private var mScrollStarted = false
    private var mTotalScrollDistance = 0f
    private var mMiddleLineColor = 0
    private var mProgressMiddleLineHeight=0

    val MIN_ANGLE = -45
    val MAX_ANGLE = 45


    @JvmOverloads
    constructor(
        context: Context?,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    fun setScrollingListener(scrollingListener: ScrollingListener?) {
        mScrollingListener = scrollingListener
    }

    fun setMiddleLineColor(@ColorInt middleLineColor: Int) {
        mMiddleLineColor = middleLineColor
        mProgressMiddleLinePaint!!.color = mMiddleLineColor
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mLastTouchedPosition = event.x
            MotionEvent.ACTION_UP -> if (mScrollingListener != null) {
                mScrollStarted = false
                mScrollingListener!!.onScrollEnd()
            }
            MotionEvent.ACTION_MOVE -> {
                val distance = event.x - mLastTouchedPosition
                if (distance != 0f) {
                    if (!mScrollStarted) {
                        mScrollStarted = true
                        if (mScrollingListener != null) {
                            mScrollingListener!!.onScrollStart()
                        }
                    }
                    onScrollEvent(event, distance)
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(mCanvasClipBounds)

        val linesCount = mCanvasClipBounds.width() / (mProgressLineWidth + mProgressLineMargin)
        val deltaX =
            mTotalScrollDistance % (mProgressLineMargin + mProgressLineWidth).toFloat()

        for (i in 0 until linesCount) {
            if (i < linesCount / 4) {
                mProgressLinePaint!!.alpha = (255 * (i / (linesCount / 4).toFloat())).toInt()
            } else if (i > linesCount * 3 / 4) {
                mProgressLinePaint!!.alpha =
                    (255 * ((linesCount - i) / (linesCount / 4).toFloat())).toInt()
            } else {
                mProgressLinePaint!!.alpha = 255
            }
            canvas.drawLine(
                -deltaX + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin),
                mCanvasClipBounds.centerY() - mProgressLineHeight / 4.0f,
                -deltaX + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin),
                mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f, mProgressLinePaint!!
            )
        }

        canvas.drawLine(
            mCanvasClipBounds.centerX().toFloat(),
            mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f,
            mCanvasClipBounds.centerX().toFloat(),
            mCanvasClipBounds.centerY() + mProgressLineHeight / 2.0f,
            mProgressMiddleLinePaint!!
        )
    }

    private fun onScrollEvent(event: MotionEvent, distance: Float) {
        mTotalScrollDistance -= distance
        postInvalidate()
        mLastTouchedPosition = event.x
        if (mScrollingListener != null) {
            mScrollingListener!!.onScroll(-distance, mTotalScrollDistance)
        }
    }

    private fun init() {
        mMiddleLineColor = ContextCompat.getColor(context, R.color.colorLightMint)
        dpToPx(context,1)
        mProgressLineWidth = dpToPx(context,2)
        mProgressLineHeight = dpToPx(context,20)
        mProgressMiddleLineHeight = dpToPx(context,30)
        mProgressLineMargin = dpToPx(context,8)
        mProgressLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mProgressLinePaint!!.style = Paint.Style.STROKE
        mProgressLinePaint!!.strokeWidth = mProgressLineWidth.toFloat()
        mProgressLinePaint!!.color =  ContextCompat.getColor(context, R.color.colorLightgray)
        mProgressMiddleLinePaint = Paint(mProgressLinePaint)
        mProgressMiddleLinePaint!!.color = mMiddleLineColor
        mProgressMiddleLinePaint!!.strokeCap = Paint.Cap.ROUND
        mProgressMiddleLinePaint!!.strokeWidth = dpToPx(context,1).toFloat()
    }

    interface ScrollingListener {
        fun onScrollStart()
        fun onScroll(delta: Float, totalDistance: Float)
        fun onScrollEnd()
    }
}