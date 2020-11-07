package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class CutAnkleGuide(val yDiff: Int, component: ObjectComponent)
    : Guide(3, "발목에서 잘리면 사진이 불안정해 보입니다. 발 끝을 맞추어 찍어 보세요", component) {



    override fun guide() {

    }
}