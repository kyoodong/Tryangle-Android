package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.guider.hasHead
import com.gomson.tryangle.guider.hasUpperBody

class CutNeckGuide(component: PersonComponent)
    : PoseGuide(8, "목이 잘리지 않게 어깨까지 찍어보세요", component) {

    override fun isMatch(component: PersonComponent): Boolean {
        return component.person.hasHead() && component.person.hasUpperBody()
    }
}