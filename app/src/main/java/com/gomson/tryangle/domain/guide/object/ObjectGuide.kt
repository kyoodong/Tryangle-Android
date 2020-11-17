package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

open abstract class ObjectGuide(
    guideId: Int,
    message: String,
    component: ObjectComponent
): Guide(guideId, message, component) {

    abstract fun isMatch(roi: Roi, guideTime: Long): Boolean

}