package com.gomson.tryangle.dto

import com.gomson.tryangle.domain.component.ObjectComponent
import kotlin.experimental.and


data class ObjectComponentListDTO(
    val objectComponentList: List<ObjectComponent>,
    val maskStr: String
) {
    fun deployMask(mask: MaskList) {
        for (i in mask.indices) {
            val b: ByteArray = mask.get(i)
            for (component in objectComponentList) {
                component.mask.add(ByteArray(b.size))
            }
            for (j in b.indices) {
                var value = b[j]
                var c = 0
                while (value > 0) {
                    val component: ObjectComponent = getObjectComponentByComponentId(c.toLong())
                        ?: continue
                    if ((value and ((1 shl c).toByte())) > 0) {
                        component.mask[i][j] = 1
                        value = (value - ((1 shl c))).toByte()
                    }
                    else {
                        component.mask[i][j] = 0
                    }
                    c++
                }
            }
        }
    }

    private fun getObjectComponentByComponentId(componentId: Long): ObjectComponent? {
        for (component in objectComponentList) {
            if (component.componentId == componentId) return component
        }
        return null
    }
}