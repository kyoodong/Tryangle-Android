package com.gomson.tryangle.domain.guide.`object`

import android.graphics.Color
import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout
import kotlin.math.abs

open class YDiffGuide(guideId: Int, message: String, val yDiff: Int, val component: ObjectComponent):
    ObjectGuide(guideId, message, component) {

    private var originLine: Line? = null
    protected var guideLine: Line? = null

    var imageWidth = 0
    var imageHeight = 0
    var layoutWidth = 0
    var layoutHeight = 0

    override fun guide(layerLayout: LayerLayout) {
        val objectComponent = component
        imageWidth = objectComponent.mask[0].size
        imageHeight = objectComponent.mask.size
        layoutWidth = layerLayout.width
        layoutHeight = layerLayout.height

        // 원래 이미지 발 끝에 라인 그리기
        originLine = Line(
            Point(objectComponent.roi.left, objectComponent.roi.bottom),
            Point(objectComponent.roi.right, objectComponent.roi.bottom),
            Color.GRAY
        ).convertTo(imageWidth, imageHeight, layerLayout.width, layerLayout.height)
        layerLayout.lineList.add(originLine!!)

        // 수정된 위치의 발 끝 라인 그리기
        guideLine = Line(
            Point(objectComponent.roi.left, objectComponent.roi.bottom + yDiff),
            Point(objectComponent.roi.right, objectComponent.roi.bottom + yDiff),
            GREEN
        ).convertTo(imageWidth, imageHeight, layerLayout.width, layerLayout.height)
        layerLayout.lineList.add(guideLine!!)

        super.guide(layerLayout)
    }

    override fun clearGuide(layerLayout: LayerLayout) {
        layerLayout.lineList.remove(originLine)
        layerLayout.lineList.remove(guideLine)

        originLine = null
        guideLine = null

        super.clearGuide(layerLayout)
    }

    override fun isMatch(roi: Roi, guideTime: Long): Boolean {
        if (guideLine == null) {
            return true
        }

        val convertedRoi = roi.convertTo(imageWidth, imageHeight, layoutWidth, layoutHeight)
        val diff = 3
        return abs(convertedRoi.bottom - guideLine!!.startPoint.y) < diff
    }
}