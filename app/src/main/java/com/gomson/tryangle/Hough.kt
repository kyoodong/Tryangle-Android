package com.gomson.tryangle

import android.graphics.Bitmap
import com.gomson.tryangle.domain.LineComponent
import org.opencv.android.Utils
import org.opencv.core.Mat

class Hough {

    private external fun find_hough_line(image: Long): Array<LineComponent>?

    fun findHoughLine(image: Bitmap): Array<LineComponent>? {
        val imageMat = Mat()
        Utils.bitmapToMat(image, imageMat)
        return find_hough_line(imageMat.nativeObjAddr, test.nativeObjAddr, test2.nativeObjAddr)
    }
}