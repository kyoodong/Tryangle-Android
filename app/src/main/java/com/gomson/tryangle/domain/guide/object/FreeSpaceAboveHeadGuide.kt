package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.guider.get
import com.gomson.tryangle.guider.has
import com.gomson.tryangle.view.LayerLayout
import org.tensorflow.lite.examples.posenet.lib.BodyPart

class FreeSpaceAboveHeadGuide(
    private val area: Area,
    private val component: PersonComponent)
    : PoseGuide(9, "머리 위에 여백이 있는것이 좋습니다", component) {

    var imageWidth = 0
    var imageHeight = 0

    override fun guide(layerLayout: LayerLayout) {
        imageWidth = component.mask[0].size
        imageHeight = component.mask.size

        area.text = "머리 금지 영역"
        layerLayout.areaList.add(area.convertTo(imageWidth, imageHeight, layerLayout.width, layerLayout.height))
        super.guide(layerLayout)
    }

    override fun isMatch(component: PersonComponent): Boolean {
        val rightEar = component.person.get(BodyPart.RIGHT_EAR)
        val leftEar = component.person.get(BodyPart.LEFT_EAR)
        val rightEye = component.person.get(BodyPart.RIGHT_EYE)
        val leftEye = component.person.get(BodyPart.LEFT_EYE)
        val nose = component.person.get(BodyPart.NOSE)

        if (rightEar != null && component.person.has(BodyPart.RIGHT_EAR)
            && area.include(Point(rightEar.position.x, rightEar.position.y - 20))) {
            return false
        }

        if (leftEar != null && component.person.has(BodyPart.LEFT_EAR)
            && area.include(Point(leftEar.position.x, leftEar.position.y - 20))) {
            return false
        }

        if (rightEye != null && component.person.has(BodyPart.RIGHT_EYE)
            && area.include(Point(rightEye.position.x, rightEye.position.y - 20))) {
            return false
        }

        if (leftEye != null && component.person.has(BodyPart.LEFT_EYE)
            && area.include(Point(leftEye.position.x, leftEye.position.y - 20))) {
            return false
        }

        if (nose != null && component.person.has(BodyPart.NOSE)
            && area.include(Point(nose.position.x, nose.position.y - 20))) {
            return false
        }

        return true
    }
}