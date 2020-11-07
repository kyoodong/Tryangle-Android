package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class FreeSpaceAboveHeadGuide(val yDiff: Int, component: ObjectComponent)
    : Guide(9, "머리 위에 여백이 있는것이 좋습니다", component) {

    override fun guide() {
        TODO("Not yet implemented")
    }
}