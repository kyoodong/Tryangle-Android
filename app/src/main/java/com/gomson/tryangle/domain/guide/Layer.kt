package com.gomson.tryangle.domain.guide

abstract class Layer {

    abstract fun convertTo(originalWidth: Int, originalHeight: Int, targetWidth: Int, targetHeight: Int): Layer
}