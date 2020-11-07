package com.gomson.tryangle.domain.guide.action

import android.graphics.Color
import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

open class YDiffGuide(guideId: Int, message: String, val yDiff: Int, val component: ObjectComponent):
    Guide(guideId, message, component) {

    protected var originLine: Line? = null
    protected var guideLine: Line? = null

    override fun guide(layerLayout: LayerLayout) {
        val objectComponent = component

        // 원래 이미지 발 끝에 라인 그리기
        originLine = Line(
            Point(objectComponent.roi.left, objectComponent.roi.bottom),
            Point(objectComponent.roi.right, objectComponent.roi.bottom),
            Color.GRAY
        )
        layerLayout.lineList.add(originLine!!)

        // 수정된 위치의 발 끝 라인 그리기
        guideLine = Line(
            Point(objectComponent.roi.left, objectComponent.roi.bottom + yDiff),
            Point(objectComponent.roi.right, objectComponent.roi.bottom + yDiff),
            Color.GREEN
        )
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
}