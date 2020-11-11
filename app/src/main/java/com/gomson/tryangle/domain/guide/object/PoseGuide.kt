package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.domain.guide.Guide

open abstract class PoseGuide(
    guideId: Int,
    message: String,
    component: PersonComponent
): Guide(guideId, message, component) {

    abstract fun isMatch(component: PersonComponent): Boolean

}