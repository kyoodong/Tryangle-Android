package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.guider.has
import org.tensorflow.lite.examples.posenet.lib.BodyPart

class CutKneeGuide(component: PersonComponent)
    : PoseGuide(7, "무릎이 잘리지 않게 허벅지까지만 찍어보세요", component) {

    override fun isMatch(component: PersonComponent, guideTime: Long): Boolean {
        val diffTime = (System.currentTimeMillis() - guideTime) / 1000
        if (diffTime > 3)
            return true

        return (component.person.has(BodyPart.LEFT_HIP) || component.person.has(BodyPart.RIGHT_HIP))
    }
}