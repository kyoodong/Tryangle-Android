package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.ObjectGuide
import kotlin.math.abs

open class ObjectGuider(
    private var imageWidth: Int,
    private var imageHeight: Int
): Guider() {

    override fun guide(component: Component): Array<ArrayList<Guide>> {
        for (guide in guides) {
            guide.clear()
        }

        val component = component as ObjectComponent
        val imageHeight = this.imageHeight ?: return guides
        val imageWidth = this.imageWidth ?: return guides

        val leftSide = imageWidth / 3
        val rightSide = imageWidth / 3 * 2
        val middleSide = imageWidth / 2
        val error = imageWidth / 2

        // @TODO y 도 비교해야함
        val leftDiff = abs(leftSide - component.centerPoint.x)
        val rightDiff = abs(rightSide - component.centerPoint.x)
        val middleDiff = abs(middleSide - component.centerPoint.x)

        // @TODO 황금 비율 영역
//        golden_ratio_area_list = self.get_golden_ratio_area()
//        # for golden_ratio_area in golden_ratio_area_list:
//        #     cv2.rectangle(image, (golden_ratio_area[0], golden_ratio_area[1]), (golden_ratio_area[2], golden_ratio_area[3]), (255, 0, 0))

        if (leftDiff < rightDiff) {
            if (leftDiff < middleDiff) {
                // 왼쪽에 치우친 경우
                if (leftDiff > error)
                    guides[5].add(
                        ObjectGuide(
                            component,
                            5,
                            Point(leftSide - component.centerPoint.x, 0)
                        )
                    )
            } else {
                // 중앙에 있는 경우
                if (middleDiff > error)
                    guides[4].add(
                        ObjectGuide(
                            component,
                            4,
                            Point(middleSide - component.centerPoint.x, 0)
                        )
                    )
            }
        } else {
            if (rightDiff < middleDiff) {
                if (rightDiff > error)
                    guides[5].add(
                        ObjectGuide(
                            component,
                            5,
                            Point(rightSide - component.centerPoint.x, 0)
                        )
                    )
            } else {
                if (middleDiff > error)
                    ObjectGuide(
                        component,
                        4,
                        Point(middleSide - component.centerPoint.x, 0)
                    )
            }
        }
        return guides
    }
}