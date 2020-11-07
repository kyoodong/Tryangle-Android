package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

class MiddleAreaGuide(
    middleArea: Pair<Point, Point>, component: ObjectComponent
): AreaGuide(4, "대상을 중앙에 두어 좌우 대칭을 맞추어 찍어 보세요", middleArea, component)