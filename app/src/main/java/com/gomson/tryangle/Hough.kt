package com.gomson.tryangle

import android.graphics.Bitmap
import com.gomson.tryangle.domain.Line
import org.opencv.android.Utils
import org.opencv.core.Mat

class Hough {

    private external fun find_hough_line(image: Long): Array<Line>?

    fun findHoughLine(image: Bitmap): Array<Line>? {
        val imageMat = Mat()
        Utils.bitmapToMat(image, imageMat)
        return find_hough_line(imageMat.nativeObjAddr)
    }
}