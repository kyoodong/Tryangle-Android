package com.gomson.tryangle.domain

import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.Layer

data class Line(
    val startPoint: Point,
    val endPoint: Point,
    val color: Int = Guide.GREEN
): Layer() {
    override fun convertTo(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Line {
        return Line(
            Point(
                (startPoint.x.toFloat() / originalWidth * targetWidth).toInt(),
                (startPoint.y.toFloat() / originalHeight * targetHeight).toInt()
            ),
            Point(
                (endPoint.x.toFloat() / originalWidth * targetWidth).toInt(),
                (endPoint.y.toFloat() / originalHeight * targetHeight).toInt()
            ),
            color
        )
    }

    fun isClose(line: Line): Boolean {
        return startPoint.isClose(line.startPoint) && endPoint.isClose(line.endPoint)
    }
}