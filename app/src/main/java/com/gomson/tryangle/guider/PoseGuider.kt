package com.gomson.tryangle.guider

import com.gomson.tryangle.MODEL_HEIGHT
import com.gomson.tryangle.MODEL_WIDTH
import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.`object`.*
import com.gomson.tryangle.pose.STAND
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.KeyPoint
import org.tensorflow.lite.examples.posenet.lib.Person
import org.tensorflow.lite.examples.posenet.lib.Position

private const val POSE_THRESHOLD = 0.4

fun Person.has(bodyPart: BodyPart): Boolean {
    for (kp in keyPoints) {
        if (kp.bodyPart == bodyPart) {
            return kp.score > POSE_THRESHOLD
        }
    }
    return false
}

fun Person.get(bodyPart: BodyPart): KeyPoint? {
    for (kp in keyPoints) {
        if (kp.bodyPart == bodyPart) {
            return kp
        }
    }
    return null
}

fun Person.hasHead(): Boolean {
    return has(BodyPart.NOSE)
            ||
            (has(BodyPart.LEFT_EAR) || has(BodyPart.RIGHT_EAR))
            &&
            (has(BodyPart.LEFT_EYE) || has(BodyPart.RIGHT_EYE))
}

fun Person.hasUpperBody(): Boolean {
    return has(BodyPart.LEFT_SHOULDER) ||
            has(BodyPart.RIGHT_SHOULDER)
}

fun Person.hasLowerBody(): Boolean {
    return (has(BodyPart.LEFT_HIP) || has(BodyPart.RIGHT_HIP))
            &&
            (has(BodyPart.LEFT_KNEE) || has(BodyPart.RIGHT_KNEE))
            &&
            (has(BodyPart.LEFT_ANKLE) || has(BodyPart.RIGHT_ANKLE))
}

fun Person.hasFullBody(): Boolean {
    return hasLowerBody() && hasUpperBody()
}

fun Person.convertTo(cropArea: Area): Person {
    val result = Person()
    result.score = score
    for (keyPoint in keyPoints) {
        val kp = KeyPoint()
        kp.score = keyPoint.score
        kp.bodyPart = keyPoint.bodyPart
        val cropX = keyPoint.position.x.toDouble() / MODEL_WIDTH * cropArea.getWidth()
        val cropY = keyPoint.position.y.toDouble() / MODEL_HEIGHT * cropArea.getHeight()
        val x = cropX + cropArea.leftTop.x
        val y = cropY + cropArea.leftTop.y
        kp.position = Position()
        kp.position.x = x.toInt()
        kp.position.y = y.toInt()
        result.keyPoints += kp
    }

    return result
}

class PoseGuider(
    private var imageWidth: Int,
    private var imageHeight: Int
) : ObjectGuider(imageWidth, imageHeight) {

    override fun initGuideList(component: Component) {
        val component = component as PersonComponent
        val imageHeight = this.imageHeight ?: return
        val imageWidth = this.imageWidth ?: return

        val guideList = component.guideList
        val gamma = imageHeight / 4

        // 사람이 사진 밑쪽에 위치한 경우
        if (component.roi.bottom + gamma > imageHeight) {
            // 발목이 잘린 경우
            // 무릎은 있으나 발목, 발꿈치 등이 모두 없는 경우
            if (component.person.has(BodyPart.LEFT_KNEE) &&
                component.person.has(BodyPart.RIGHT_KNEE) &&
                !component.person.has(BodyPart.LEFT_ANKLE) &&
                !component.person.has(BodyPart.RIGHT_ANKLE)) {
                guideList.add(CutAnkleGuide(component))
            }

            // 엉덩이는 있지만 무릎에서 잘린 경우
            if ((component.person.has(BodyPart.LEFT_HIP) || component.person.has(BodyPart.RIGHT_HIP)) &&
                    !component.person.has(BodyPart.LEFT_KNEE) &&
                    !component.person.has(BodyPart.LEFT_ANKLE) &&
                    !component.person.has(BodyPart.RIGHT_KNEE) &&
                    !component.person.has(BodyPart.RIGHT_ANKLE)) {
                guideList.add(CutKneeGuide(component))
            }

            // 머리만 덜렁 있는 사진
            if (component.person.hasHead() && !component.person.hasUpperBody() && !component.person.hasLowerBody()) {
                guideList.add(CutNeckGuide(component))
            }
        }

        // 사람이 사진의 윗쪽에 위치한 경우
        if (component.roi.top < gamma) {
            if (component.person.hasHead()) {
                val area = Area(
                    Point(0, 0),
                    Point(imageWidth, imageHeight / 4)
                )
                guideList.add(FreeSpaceAboveHeadGuide(area, component))
            }
        }

        val foot_lower_threshold = imageHeight / 10 * 9
        // 발 끝을 맞추도록 유도
        if (//component.person.hasLowerBody() &&
            component.roi.bottom < foot_lower_threshold) {
            val area = Area(
                Point(0, foot_lower_threshold),
                Point(imageWidth, imageHeight),
                Guide.GREEN,
                "발 끝 영역"
            )
            guideList.add(BottomToeGuide(area, component))
        }
    }
}