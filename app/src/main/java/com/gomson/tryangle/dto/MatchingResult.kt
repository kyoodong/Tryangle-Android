package com.gomson.tryangle.dto

import org.opencv.core.Point

data class MatchingResult(
    val matchRatio: Int,
    // 좌상단 -> 우상단 -> 우하단 -> 좌하단
    val pointX1: Float,
    val pointY1: Float,
    val pointX2: Float,
    val pointY2: Float,
    val pointX3: Float,
    val pointY3: Float,
    val pointX4: Float,
    val pointY4: Float
) {
}