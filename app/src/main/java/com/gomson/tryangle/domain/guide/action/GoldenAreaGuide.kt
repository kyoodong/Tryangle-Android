package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class GoldenAreaGuide(
    val goldenArea: Pair<Point, Point>,
    component: ObjectComponent
): Guide(5, "대상을 황금 영역에 두고 찍어 보세요", component) {

    override fun guide() {

    }
}