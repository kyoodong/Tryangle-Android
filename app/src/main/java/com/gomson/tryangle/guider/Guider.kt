package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Component
import com.gomson.tryangle.domain.Guide

abstract class Guider {

    protected val guides = Array(20) {i -> ArrayList<Guide>()}

    abstract fun guide(component: Component) : Array<ArrayList<Guide>>
}