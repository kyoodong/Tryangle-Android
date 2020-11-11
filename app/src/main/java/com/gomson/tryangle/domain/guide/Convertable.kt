package com.gomson.tryangle.domain.guide

interface Convertable {

    fun convertTo(originalWidth: Int, originalHeight: Int, targetWidth: Int, targetHeight: Int): Convertable
}