package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.LineComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.LineGuide

class LineGuider: Guider() {

    override fun guide(component: Component): Array<ArrayList<Guide>> {
        for (guide in guides) {
            guide.clear()
        }

        val component = component as LineComponent
        val upperThreshold = 25
        val lowerThreshold = 5

        val diff = component.start.diff(component.end)

        //@TODO start, end 그대로 넣으면 안되긴함
        // 수평선 양 끝 점의 차이가 lower_threshold 초과이면 가이드 멘트
        if (diff.y in lowerThreshold until upperThreshold) {
            guides[0].add(
                LineGuide(
                    component, 0,
                    component.start,
                    component.end
                )
            )
        }

        // 수직선
        if (diff.x in lowerThreshold until upperThreshold) {
            guides[1].add(
                LineGuide(
                    component, 1,
                    component.start,
                    component.end
                )
            )
        }

        return guides
    }
}