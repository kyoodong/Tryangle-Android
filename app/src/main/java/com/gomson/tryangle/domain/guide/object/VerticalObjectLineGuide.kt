package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Line
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.view.LayerLayout
import kotlin.math.abs

open class VerticalObjectLineGuide(
    guidId: Int,
    message: String,
    val line: Line,
    component: ObjectComponent
): ObjectGuide(guidId, message, component) {

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
        layerLayout.lineList.remove(guideLine)
        guideLine = null
        super.clearGuide(layerLayout)
    }

    override fun isMatch(roi: Roi, guideTime: Long): Pair<Boolean, Double> {
        val unitWidth = roi.getWidth() / 3
        val isMatch = roi.left + unitWidth < line.startPoint.x && line.startPoint.x < roi.right - unitWidth
        val total = unitWidth
        val percent = when {
            isMatch -> {
                1.0
            }
            total <= 0 -> {
                1.0
            }
            line.startPoint.x < roi.left + unitWidth -> {
                val distance = abs(line.startPoint.x - (roi.left + unitWidth))
                ((total - distance) / total).toDouble()
            }
            else -> {
                val distance = abs(line.startPoint.x - (roi.right - unitWidth))
                ((total - distance) / total).toDouble()
            }
        }
        return Pair(isMatch, percent)
    }
}