package com.gomson.tryangle.domain.guide

import com.gomson.tryangle.view.LayerLayout

class LayerLayoutGuideManager(
    private val layerLayout: LayerLayout
) {
    private var prevGuide: Guide? = null

    fun guide(guide: Guide?) {
        prevGuide?.clearGuide(layerLayout)
        guide?.guide(layerLayout)
        prevGuide = guide
    }
}