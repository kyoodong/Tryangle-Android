package com.gomson.tryangle.domain

import android.graphics.Bitmap
import android.widget.ImageView
import com.gomson.tryangle.Layer

class ObjectComponentImage: ObjectComponent {

    val roiImage: Bitmap
    val layer: Layer

    constructor(objectComponent: ObjectComponent, layer: Layer, roiImage: Bitmap)
            : super(objectComponent.id,
        objectComponent.componentId,
        objectComponent.clazz,
        objectComponent.centerPointX,
        objectComponent.centerPointY,
        objectComponent.area,
        objectComponent.mask,
        objectComponent.roi) {
        this.roiImage = roiImage
        this.layer = layer
    }
}