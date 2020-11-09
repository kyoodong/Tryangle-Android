package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.component.ObjectComponent

class MiddleObjectLineGuide(
    middleLine: Line,
    component: ObjectComponent
): VerticalObjectLineGuide(4, "대상을 중앙에 두어 좌우 대칭을 맞추어 찍어보세요", middleLine, component)