package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.ObjectGuide
import kotlin.math.abs

open class GuideImageObjectGuider(
    private var imageWidth: Int,
    private var imageHeight: Int
): Guider() {

    override fun guide(component: Component) {
        val component = component as ObjectComponent
        val imageHeight = this.imageHeight ?: return
        val imageWidth = this.imageWidth ?: return
        val middleSide = imageWidth / 2
        val error = imageWidth / 2

        val guideList = component.guideList
        guideList.clear()
        val middleDiff = abs(middleSide - component.centerPoint.x)
        val goldenAreaList = getGoldenRatioArea(imageWidth, imageHeight)
        for (i in goldenAreaList.indices) {
            val iou = getIou(goldenAreaList[i], component.mask)
            if (iou > 0.7) {
                guideList.add(ObjectGuide(5, Point(i, -1)))
            }
        }

        // 중앙에 잘 위치한 경우
        if (middleDiff <= error) {
            guideList.add(ObjectGuide(4, null))
        }
        component.guideList = guideList
    }

    private fun getGoldenRatioArea(imageWidth: Int, imageHeight: Int): ArrayList<Pair<Point, Point>> {
        val unitX = imageWidth / 8
        val unitY = imageHeight / 8

        val areaList = ArrayList<Pair<Point, Point>>()

        // 좌상단
        areaList.add(
            Pair(
                Point(unitY * 2, unitX * 2),
                Point(unitY * 3, unitX * 3)
            )
        )

        // 우상단
        areaList.add(
            Pair(
                Point(unitY * 2, unitX * 5),
                Point(unitY * 3, unitX * 6)
            )
        )

        // 좌하단
        areaList.add(
            Pair(
                Point(unitY * 5, unitX * 2),
                Point(unitY * 6, unitX * 3)
            )
        )

        // 우하단
        areaList.add(
            Pair(
                Point(unitY * 5, unitX * 5),
                Point(unitY * 6, unitX * 6)
            )
        )

        // 정중앙
        areaList.add(
            Pair(
                Point(unitY * 3, unitX * 3),
                Point(unitY * 5, unitX * 5)
            )
        )

        return areaList
    }

    private fun getIou(area: Pair<Point, Point>, mask: ArrayList<ArrayList<Int>>): Float {
        val width = area.second.x - area.first.x
        val height = area.second.y - area.first.y
        var count = 0
        for (y in area.first.y .. area.second.y) {
            for (x in area.first.x .. area.second.x) {
                if (mask[y][x] > 0) {
                    count += 1
                }
            }
        }
        return count / (width * height).toFloat()
    }
}