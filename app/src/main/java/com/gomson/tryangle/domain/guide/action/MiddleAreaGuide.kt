package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

class MiddleAreaGuide(
    val middleArea: Pair<Point, Point>, component: ObjectComponent
): Guide(4, "대상을 중앙에 두어 좌우 대칭을 맞추어 찍어 보세요", component) {

    override fun guide(layerLayout: LayerLayout) {
        super.guide(layerLayout)
    }

    override fun clearGuide(layerLayout: LayerLayout) {
        super.clearGuide(layerLayout)
    }
}