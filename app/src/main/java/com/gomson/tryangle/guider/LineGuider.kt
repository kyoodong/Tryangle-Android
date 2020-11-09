package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.LineComponent
import com.gomson.tryangle.domain.guide.LineGuide

class LineGuider: Guider() {

    override fun initGuideList(component: Component) {
        val component = component as LineComponent
        val upperThreshold = 25
        val lowerThreshold = 5

        val diff = component.start.diff(component.end)
        val guideList = component.guideList
        guideList.clear()

        //@TODO start, end 그대로 넣으면 안되긴함
        // 수평선 양 끝 점의 차이가 lower_threshold 초과이면 가이드 멘트
        if (diff.y in lowerThreshold until upperThreshold) {
            val lineGuide = LineGuide(
                0,
                "수평선을 맞추어 찍어 보세요",
                component,
                component.start,
                component.end
            )
            guideList.add(lineGuide)
        }

        // 수직선
        if (diff.x in lowerThreshold until upperThreshold) {
            val lineGuide = LineGuide(
                1,
                "수직선을 맞추어 찍어 보세요",
                component,
                component.start,
                component.end
            )
            guideList.add(lineGuide)
        }
    }
}