package com.gomson.tryangle.domain.guide.action

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide

class CutKneeGuide(yDiff: Int, component: ObjectComponent)
    : YDiffGuide(7, "무릎이 잘리지 않게 허벅지까지만 찍어보세요", yDiff, component)