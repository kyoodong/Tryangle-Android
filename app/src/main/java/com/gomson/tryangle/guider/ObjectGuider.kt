package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.action.GoldenAreaGuide
import com.gomson.tryangle.domain.guide.action.MiddleObjectLineGuide
import kotlin.math.abs

open class ObjectGuider(
    private var imageWidth: Int,
    private var imageHeight: Int
): Guider() {

    override fun guide(component: Component) {
        val component = component as ObjectComponent
        val imageHeight = this.imageHeight ?: return
        val imageWidth = this.imageWidth ?: return

        val leftSide = imageWidth / 3
        val rightSide = imageWidth / 3 * 2
        val middleSide = imageWidth / 2
        val error = imageWidth / 2

        // @TODO y 도 비교해야함
        val leftDiff = abs(leftSide - component.centerPoint.x)
        val rightDiff = abs(rightSide - component.centerPoint.x)
        val middleDiff = abs(middleSide - component.centerPoint.x)
        val guideList = component.guideList
        guideList.clear()

        // @TODO 황금 비율 영역
//        golden_ratio_area_list = self.get_golden_ratio_area()
//        # for golden_ratio_area in golden_ratio_area_list:
//        #     cv2.rectangle(image, (golden_ratio_area[0], golden_ratio_area[1]), (golden_ratio_area[2], golden_ratio_area[3]), (255, 0, 0))

        if (leftDiff < rightDiff) {
            if (leftDiff < middleDiff) {
                // 왼쪽에 치우친 경우
                if (leftDiff > error)
                    guideList.add(
                        GoldenAreaGuide(
                            Pair(
                                Point(leftSide - 10, 0),
                                Point(leftSide + 10, imageHeight)
                            ),
                            component
                        )
                    )
            } else {
                // 중앙에 있는 경우
                if (middleDiff > error)
                    guideList.add(
                        MiddleObjectLineGuide(
                            Line(
                                Point(middleSide, 0),
                                Point(middleSide, imageHeight),
                                Guide.GREEN
                            ), component
                        )
                    )
            }
        } else {
            if (rightDiff < middleDiff) {
                if (rightDiff > error)
                    GoldenAreaGuide(
                        Pair(
                            Point(rightSide - 10, 0),
                            Point(rightSide + 10, imageHeight)
                        ), component
                    )
            } else {
                if (middleDiff > error)
                    guideList.add(
                        MiddleObjectLineGuide(
                            Line(
                                Point(middleSide, 0),
                                Point(middleSide, imageHeight),
                                Guide.GREEN
                            ), component
                        )
                    )
            }
        }

        component.guideList = guideList
    }
}