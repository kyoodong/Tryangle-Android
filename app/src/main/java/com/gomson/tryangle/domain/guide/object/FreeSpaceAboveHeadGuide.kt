package com.gomson.tryangle.domain.guide.`object`

import com.gomson.tryangle.domain.component.ObjectComponent

class FreeSpaceAboveHeadGuide(yDiff: Int, component: ObjectComponent)
    : YDiffGuide(9, "머리 위에 여백이 있는것이 좋습니다", yDiff, component)