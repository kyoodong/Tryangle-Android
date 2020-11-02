package com.gomson.tryangle

import com.gomson.tryangle.domain.component.ObjectComponent
import kotlin.math.abs

class ComponentMatcher {

    fun match(component: ObjectComponent, guideImageComponentList: ArrayList<ObjectComponent>): ObjectComponent? {
        val index = findClosestComponent(component, guideImageComponentList)

        if (index < 0)
            return null

        val result = guideImageComponentList[index]
        guideImageComponentList.remove(result)
        return result
    }

    private fun findClosestComponent(component: ObjectComponent, componentList: ArrayList<ObjectComponent>): Int {
        var distance = Int.MAX_VALUE
        var index = -1
        for (i in componentList.indices) {
            if (component.clazz != componentList[i].clazz)
                continue

            val diff = component.centerPoint - componentList[i].centerPoint
            val curDistance = abs(diff.x) + abs(diff.y)
            if (curDistance < distance) {
                distance = curDistance
                index = i
            }
        }
        return index
    }
}