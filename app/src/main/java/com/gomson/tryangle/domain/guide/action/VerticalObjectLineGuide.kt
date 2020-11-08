package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout

open class VerticalObjectLineGuide(
    guidId: Int,
    message: String,
    val line: Line,
    component: ObjectComponent
): Guide(guidId, message, component) {

    private var guideLine: Line? = null

    override fun guide(layerLayout: LayerLayout) {
        val objectComponent = component as ObjectComponent
        val width = objectComponent.mask[0].size
        val height = objectComponent.mask.size
        guideLine = line.convertTo(width, height, layerLayout.width, layerLayout.height)
        layerLayout.lineList.add(guideLine!!)
        super.guide(layerLayout)
    }

    override fun clearGuide(layerLayout: LayerLayout) {
        layerLayout.areaList.remove(guideLine)
        guideLine = null
        super.clearGuide(layerLayout)
    }

    override fun isMatch(roi: Roi): Boolean {
        val unitWidth = roi.getWidth() / 3
        return roi.left + unitWidth < line.startPoint.x && line.startPoint.x < roi.right - unitWidth
    }
}