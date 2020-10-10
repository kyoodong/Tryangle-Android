package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.*
import org.opencv.core.Point
import kotlin.math.abs

class LineGuider: Guider() {

    override fun guide(component: Component): Array<ArrayList<Guide>> {
        for (guide in guides) {
            guide.clear()
        }

        val component = component as LineComponent
        val upperThreshold = 25
        val lowerThreshold = 5

        val yDiff = abs(component.startY - component.endY)
        val xDiff = abs(component.startX - component.endX)

        // 수평선 양 끝 점의 차이가 lower_threshold 초과이면 가이드 멘트
        if (yDiff in lowerThreshold until upperThreshold) {
            guides[0].add(LineGuide(component.id, 0,
                Point(component.startX.toDouble(), component.startY.toDouble()),
                Point(component.endX.toDouble(), component.endY.toDouble())
            ))
        }

        // 수직선
        if (xDiff in lowerThreshold until upperThreshold) {
            guides[1].add(LineGuide(component.id, 1,
                Point(component.startX.toDouble(), component.startY.toDouble()),
                Point(component.endX.toDouble(), component.endY.toDouble())))
        }

        return guides
    }
}