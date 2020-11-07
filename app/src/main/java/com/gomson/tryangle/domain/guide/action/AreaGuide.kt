package com.gomson.tryangle.domain.guide.action

import android.graphics.Color
import com.gomson.tryangle.domain.Area
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

open class AreaGuide(
    guidId: Int,
    message: String,
    val area: Pair<Point, Point>,
    component: ObjectComponent
): Guide(guidId, message, component) {

    private var guideArea: Area? = null

    override fun guide(layerLayout: LayerLayout) {
        guideArea = Area(area.first, area.second, Color.GREEN)
        layerLayout.areaList.add(guideArea!!)
        super.guide(layerLayout)
    }

    override fun clearGuide(layerLayout: LayerLayout) {
        layerLayout.areaList.remove(guideArea)
        guideArea = null
        super.clearGuide(layerLayout)
    }
}