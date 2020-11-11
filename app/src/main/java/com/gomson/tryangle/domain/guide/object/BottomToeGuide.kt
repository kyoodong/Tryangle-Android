package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent

class BottomToeGuide(area: Area, component: ObjectComponent):
    AreaGuide(2, "발 끝을 맞추어 찍으면 다리가 길게 보입니다", area, component) {

    override fun isMatch(componentRoi: Roi): Boolean {
        return area.getRoi().getIou(componentRoi) > 0.1
    }
}