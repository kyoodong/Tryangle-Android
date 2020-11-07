package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class CutNeckGuide(val yDiff: Int, component: ObjectComponent)
    : Guide(8, "목이 잘리지 않게 어깨까지 찍어보세요", component) {

    override fun guide() {
        TODO("Not yet implemented")
    }
}