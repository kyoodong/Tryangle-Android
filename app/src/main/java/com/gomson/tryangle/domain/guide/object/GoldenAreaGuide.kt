package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent

class GoldenAreaGuide(
    goldenArea: Pair<Point, Point>,
    component: ObjectComponent
): AreaGuide(5, "대상을 황금 영역에 두고 찍어 보세요", goldenArea, component)