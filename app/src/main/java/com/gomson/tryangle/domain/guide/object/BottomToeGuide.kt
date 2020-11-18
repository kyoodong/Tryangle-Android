package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent

class BottomToeGuide(area: Area, component: ObjectComponent):
    AreaGuide(2, "발 끝 라인을 아래 영역에 넣어보세요", area, component) {

    override fun isMatch(componentRoi: Roi, guideTime: Long): Pair<Boolean, Double> {
        val iou = area.getRoi().getIou(componentRoi)
        val diff = 0.1
        val isMatch = iou > diff
        val total = iou - diff
        val percent = if (isMatch || total <= 0.toDouble()) {
            1.0
        } else {
            iou / total
        }
        return Pair(isMatch, percent)
    }
}