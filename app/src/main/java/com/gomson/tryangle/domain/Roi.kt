package com.gomson.tryangle.domain

import android.graphics.Rect

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
}