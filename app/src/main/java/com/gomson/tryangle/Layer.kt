package com.gomson.tryangle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.nfc.Tag
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class Layer(
    val mask: ArrayList<ArrayList<Int>>,
    private val roi: ArrayList<Int>) {

    private val width = mask[0].size
    private val height = mask.size
    private val roiWidth = roi[3] - roi[1]
    private val roiHeight = roi[2] - roi[0]
    private val visit = Array(height) { BooleanArray(width) }
    private val directions = arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1))
    private val directions8 = arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, 1),
        intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(-1, -1), intArrayOf(0, -1))
    private var cumulativeX = 0
    private var cumulativeY = 0
    private var pixelCount = 0
    private val queue: Queue<Pair<Int, Int>> = LinkedList()

    var ratioInRoi = 0
    var centerPoint = Pair(0, 0)
    val layeredImage: Bitmap?


    private fun bfs(y: Int, x: Int) {
        queue.add(Pair(y, x))

        while (queue.isNotEmpty()) {
            val front = queue.poll() ?: break
            cumulativeY += front.first
            cumulativeX += front.second
            pixelCount++

            for (i in directions.indices) {
                val direction = directions[i]
                val curY = front.first + direction[0]
                val curX = front.second + direction[1]

                if (curY < 0 || curX < 0 || curY >= height || curX >= width || visit[curY][curX])
                    continue

                when (mask[curY][curX]) {
                    0 -> {
                        for (j in directions8.indices) {
                            val ny = curY + directions8[j][0]
                            val nx = curX + directions8[j][1]

                            if (ny < 0 || nx < 0 || ny >= height || nx >= width)
                                continue

                            mask[ny][nx] = 2
                        }
                        mask[curY][curX] = 2
                    }
                    1 -> {
                        visit[curY][curX] = true
                        queue.add(Pair(curY, curX))
                    }
                }
            }
        }
    }

    init {
        for (y in roi[0] until roi[2]) {
            for (x in roi[1] until roi[3]) {
                if (mask[y][x] == 1 && !visit[y][x]) {
                    bfs(y, x)
                }
            }
        }

        val ratio = pixelCount.toFloat() / (width * height)
        ratioInRoi = (pixelCount.toFloat() / (roiWidth * roiHeight) * 100).toInt()
        if (ratio > 0.02) {
            centerPoint = Pair(cumulativeY / pixelCount, cumulativeX / pixelCount)

            val pixels = IntArray(roiWidth * roiHeight)
            var index = 0
            for (y in roi[0] until roi[2]) {
                for (x in roi[1] until roi[3]) {
                    if (mask[y][x] == 0) {
                        pixels[index] = Color.argb(0, 0, 0, 0)
                    } else if (mask[y][x] == 1) {
                        pixels[index] = Color.argb(50, 127, 127, 127)
                    } else {
                        pixels[index] = Color.argb(255 ,127, 127, 127)
                    }
                    index++
                }
            }
            layeredImage = Bitmap.createBitmap(pixels, roiWidth, roiHeight, Bitmap.Config.ARGB_8888)
        } else {
            layeredImage = null
        }
    }
}