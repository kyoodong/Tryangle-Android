package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent

class BottomToeGuide(area: Area, component: ObjectComponent):
    AreaGuide(2, "발 끝 라인을 아래 영역에 넣어보세요", area, component) {

    override fun isMatch(componentRoi: Roi, guideTime: Long): Boolean {
        return area.getRoi().getIou(componentRoi) > 0.1
    }
}