package com.gomson.tryangle

import android.graphics.Bitmap
import android.graphics.Color
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import java.util.*
import kotlin.collections.ArrayList

class Layer(
    val mask: ArrayList<ArrayList<Int>>,
    roi: Roi) {

    private val width = mask[0].size
    private val height = mask.size
    private val visit = Array(height) { BooleanArray(width) }
    private val directions = arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1))
    private val directions8 = arrayOf(intArrayOf(-1, 0), intArrayOf(-1, 1), intArrayOf(0, 1),
        intArrayOf(1, 1), intArrayOf(1, 0), intArrayOf(1, -1), intArrayOf(-1, -1), intArrayOf(0, -1))
    private var cumulativeX = 0
    private var cumulativeY = 0
    private var pixelCount = 0
    private val queue: Queue<Pair<Int, Int>> = LinkedList()
    private var centerPoint = Point(0, 0)

    var ratioInRoi = 0
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
        for (y in roi.top until roi.bottom) {
            for (x in roi.left until roi.right) {
                if (mask[y][x] == 1 && !visit[y][x]) {
                    bfs(y, x)
                }
            }
        }

        ratioInRoi = (pixelCount.toFloat() / (roi.getWidth() * roi.getHeight()) * 100).toInt()
        if (getArea() > 0.01) {
            centerPoint = Point(cumulativeX / pixelCount, cumulativeY / pixelCount)

            val pixels = IntArray(roi.getWidth() * roi.getHeight())
            var index = 0
            for (y in roi.top until roi.bottom) {
                for (x in roi.left until roi.right) {
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
            layeredImage = Bitmap.createBitmap(pixels, roi.getWidth(), roi.getHeight(), Bitmap.Config.ARGB_8888)
        } else {
            layeredImage = null
        }
    }

    fun getCenterPoint(): Point {
        return centerPoint
    }

    fun getArea(): Float {
        return pixelCount.toFloat() / (width * height)
    }
}