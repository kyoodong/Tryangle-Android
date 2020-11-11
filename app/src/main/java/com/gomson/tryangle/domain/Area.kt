package com.gomson.tryangle.domain

import com.gomson.tryangle.domain.guide.Layer

data class Area(
    val leftTop: Point,
    val rightBottom: Point,
    val color: Int = 0
): Layer() {
    override fun convertTo(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Area {
        return Area(
            Point(
                (leftTop.x.toFloat() / originalWidth * targetWidth).toInt(),
                (leftTop.y.toFloat() / originalHeight * targetHeight).toInt()
            ),
            Point(
                (rightBottom.x.toFloat() / originalWidth * targetWidth).toInt(),
                (rightBottom.y.toFloat() / originalHeight * targetHeight).toInt()
            ),
            color
        )
    }

    fun getWidth(): Int {
        return rightBottom.x - leftTop.x
    }

    fun getHeight(): Int {
        return rightBottom.y - leftTop.y
    }

    fun getRoi(): Roi {
        return Roi(leftTop.x, rightBottom.x, leftTop.y, rightBottom.y)
    }
}