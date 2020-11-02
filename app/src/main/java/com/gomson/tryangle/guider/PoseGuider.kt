package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.pose.PoseClass
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.ObjectGuide
import com.gomson.tryangle.domain.component.PersonComponent
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.KeyPoint
import org.tensorflow.lite.examples.posenet.lib.Person

private const val FOOT_LOWER_THRESHOLD = 10
private const val POSE_THRESHOLD = 0.02

private fun Person.has(bodyPart: BodyPart): Boolean {
    for (kp in keyPoints) {
        if (kp.bodyPart == bodyPart) {
            return kp.score > POSE_THRESHOLD
        }
    }
    return false
}

private fun Person.get(bodyPart: BodyPart): KeyPoint? {
    for (kp in keyPoints) {
        if (kp.bodyPart == bodyPart) {
            return kp
        }
    }
    return null
}

private fun Person.hasHead(): Boolean {
    return has(BodyPart.LEFT_EAR) &&
            has(BodyPart.RIGHT_EAR) &&
            has(BodyPart.LEFT_EYE) &&
            has(BodyPart.RIGHT_EYE) &&
            has(BodyPart.NOSE)
}

private fun Person.hasUpperBody(): Boolean {
    return has(BodyPart.LEFT_SHOULDER) &&
            has(BodyPart.RIGHT_SHOULDER)
}

private fun Person.hasLowerBody(): Boolean {
    return has(BodyPart.LEFT_HIP) &&
           has(BodyPart.RIGHT_HIP) &&
           has(BodyPart.LEFT_KNEE) &&
           has(BodyPart.RIGHT_KNEE) &&
           has(BodyPart.LEFT_ANKLE) &&
           has(BodyPart.RIGHT_ANKLE)
}

private fun Person.hasFullBody(): Boolean {
    return hasLowerBody() && hasUpperBody()
}

class PoseGuider(
    private var imageWidth: Int,
    private var imageHeight: Int
) : ObjectGuider(imageWidth, imageHeight) {

    override fun guide(component: Component) {
        val component = component as PersonComponent
        val imageHeight = this.imageHeight ?: return
        val imageWidth = this.imageWidth ?: return

        val guideList = component.guideList

        // @TODO: 서 있는 케이스 추가해야함. 상반신만 있어도 서 있을 수 있음
        // 서 있는 경우
        if (component.pose == PoseClass.STAND) {
            val gamma = 5

            // 사람이 사진 밑쪽에 위치한 경우
            if (component.roi.bottom + gamma > imageHeight) {
                // 발목이 잘린 경우
                // 무릎은 있으나 발목, 발꿈치 등이 모두 없는 경우
                if (component.person.has(BodyPart.LEFT_KNEE) &&
                    component.person.has(BodyPart.RIGHT_KNEE) &&
                    !component.person.has(BodyPart.LEFT_ANKLE) &&
                    !component.person.has(BodyPart.RIGHT_ANKLE)) {
                    val personHeight = component.roi.bottom - component.roi.top
                    val diff = -personHeight * 10 / 170
                    guideList.add(
                        ObjectGuide(
                            3,
                            Point(0, diff)
                        )
                    )
                }

                // 엉덩이는 있지만 무릎에서 잘린 경우
                if ((component.person.has(BodyPart.LEFT_HIP) || component.person.has(BodyPart.RIGHT_HIP)) &&
                        !component.person.has(BodyPart.LEFT_KNEE) &&
                        !component.person.has(BodyPart.LEFT_ANKLE) &&
                        !component.person.has(BodyPart.RIGHT_KNEE) &&
                        !component.person.has(BodyPart.RIGHT_ANKLE)) {
                    val personHeight = component.roi.getHeight()
                    val diff = personHeight * 20 / 170
                    guideList.add(
                        ObjectGuide(
                            7,
                            Point(0, diff)
                        )
                    )
                }

                // 머리만 덜렁 있는 사진
                if (component.person.hasHead() && !component.person.hasUpperBody() && !component.person.hasLowerBody()) {
                    val personHeight = component.roi.getHeight()
                    val diff = -personHeight * 20 / 170
                    guideList.add(
                        ObjectGuide(
                            8,
                            Point(0, diff)
                        )
                    )
                }
            }

            // 발 끝을 맞추도록 유도
            if (component.person.hasFullBody() &&
                imageHeight > component.roi.bottom + FOOT_LOWER_THRESHOLD) {
                val diff = imageHeight - component.roi.bottom + FOOT_LOWER_THRESHOLD
                guideList.add(
                    ObjectGuide(
                        2,
                        Point(0, diff)
                    )
                )
            }

            // 사람이 사진의 윗쪽에 위치한 경우
            if (component.roi.top < gamma) {
                if (component.person.hasHead()) {
                    val top = imageHeight / 3
                    val diff = top - component.roi.top
                    guideList.add(
                        ObjectGuide(
                            9,
                            Point(0, diff)
                        )
                    )
                }
            }
        }

        component.guideList = guideList
    }
}