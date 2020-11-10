package com.gomson.tryangle.domain

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

data class Roi(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int
) {
    fun getWidth(): Int {
        return right - left
    }

    fun getHeight(): Int {
        return bottom - top
    }

    operator fun plus(point: Point): Roi {
        return Roi(left + point.x, right + point.x, top + point.y, bottom + point.y)
    }

    operator fun minus(point: Point): Roi {
        return Roi(left - point.x, right - point.x, top - point.y, bottom - point.y)
    }

    operator fun plus(roi: Roi): Roi {
        return Roi(left + roi.left, right + roi.right, top + roi.top, bottom + roi.bottom)
    }

    fun toRect(): Rect {
        return Rect(left, top, right, bottom)
    }

    fun getCenterPoint(): Point {
        return Point(getWidth() / 2 + left, getHeight() / 2 + top)
    }

    fun getIou(roi: Roi): Float {
        var lft = this
        var rht = roi

        if (rht.left < lft.left) {
            val tmp = lft
            lft = rht
            rht = tmp
        }

        if (lft.right <= rht.left)
            return 0f

        val minX = max(lft.left, rht.left)
        val maxX = min(lft.right, rht.right)

        if (lft.bottom <= rht.top)
            return 0f

        val minY = min(lft.bottom, rht.top)
        val maxY = min(lft.bottom, rht.bottom)
        val intersectWidth = maxX - minX
        val intersectHeight = maxY - minY
        val intersectArea = intersectWidth * intersectHeight
        val totalArea = min(getWidth() * getHeight(), roi.getWidth() * roi.getHeight())
        return intersectArea / totalArea.toFloat()
    }
}