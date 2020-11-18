package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.guider.has
import org.tensorflow.lite.examples.posenet.lib.BodyPart

class CutAnkleGuide(component: PersonComponent)
    : PoseGuide(3, "발목에서 잘리면 사진이 불안정해 보입니다. 발 끝을 맞추어 찍어 보세요", component) {
    override fun isMatch(component: PersonComponent, guideTime: Long): Boolean {
        val diffTime = (System.currentTimeMillis() - guideTime) / 1000
        if (diffTime > 3)
            return true

        return component.person.has(BodyPart.LEFT_KNEE) &&
                component.person.has(BodyPart.RIGHT_KNEE) &&
                component.person.has(BodyPart.LEFT_ANKLE) &&
                component.person.has(BodyPart.RIGHT_ANKLE)
    }
}