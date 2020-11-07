package com.gomson.tryangle.domain.guide.action

import android.graphics.Color
import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

class BottomToeGuide(val yDiff: Int, component: ObjectComponent):
    Guide(2, "발 끝을 맞추어 찍으면 다리가 길게 보입니다", component) {

    private var originLine: Line? = null
    private var guideLine: Line? = null

    override fun guide(layerLayout: LayerLayout) {
        val objectComponent = component as ObjectComponent

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