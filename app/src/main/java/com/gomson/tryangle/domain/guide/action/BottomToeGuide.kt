package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class BottomToeGuide(val diffY: Int, component: ObjectComponent):
    Guide(2, "발 끝을 맞추어 찍으면 다리가 길게 보입니다", component) {

    override fun guide() {

    }
}