package com.gomson.tryangle.guider

import com.gomson.tryangle.domain.Guide

class Guides {

    companion object {
        fun compositeGuide(dst: Array<ArrayList<Guide>>, vararg guidesArray: Array<ArrayList<Guide>>) {
            for (guides in guidesArray) {
                for (i in guides.indices) {
                    dst[i].addAll(guides[i])
                }
            }
        }
    }
}