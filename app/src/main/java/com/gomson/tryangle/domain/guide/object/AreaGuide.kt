package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.view.LayerLayout

open class AreaGuide(
    guidId: Int,
    message: String,
    val area: Pair<Point, Point>,
    component: ObjectComponent
): ObjectGuide(guidId, message, component) {

    private var guideArea: Area? = null

    override fun guide(layerLayout: LayerLayout) {
        val objectComponent = component as ObjectComponent
        val width = objectComponent.mask[0].size
        val height = objectComponent.mask.size
        guideArea = Area(area.first, area.second, GREEN)
            .convertTo(width, height, layerLayout.width, layerLayout.height)
        layerLayout.areaList.add(guideArea!!)
        super.guide(layerLayout)
    }

    override fun clearGuide(layerLayout: LayerLayout) {
        layerLayout.areaList.remove(guideArea)
        guideArea = null
        super.clearGuide(layerLayout)
    }

    override fun isMatch(componentRoi: Roi): Boolean {
        val diff = 0.05
        val areaRoi = Roi(area.first.x, area.second.x, area.first.y, area.second.y)
        val iou = areaRoi.getIou(componentRoi)
        return iou > (1 - diff) && iou < (1 + diff)
    }
}