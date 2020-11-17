package com.gomson.tryangle.domain

import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.Convertable
import org.tensorflow.lite.examples.posenet.lib.Position

data class Area(
    val leftTop: Point,
    val rightBottom: Point,
    val color: Int = Guide.GREEN,
    var text: String? = null
): Convertable {
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
            color,
            text
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

    fun include(point: Point): Boolean {
        return point.x >= leftTop.x && point.x <= rightBottom.x &&
                point.y >= leftTop.y && point.y <= rightBottom.y
    }
}