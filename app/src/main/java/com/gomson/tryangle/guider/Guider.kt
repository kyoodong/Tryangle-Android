package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.guide.Guide

abstract class Guider {

    abstract fun initGuideList(component: Component)
}