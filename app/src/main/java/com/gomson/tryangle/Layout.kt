package com.gomson.tryangle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.*

class Layout(
    val mask: ArrayList<ArrayList<Int>>) {

    private val width = mask[0].size
    private val height = mask.size
    private val visit = Array(height) { BooleanArray(width) }
    private val directions = arrayOf(intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(0, -1))
    private var cumulativeX = 0
    private var cumulativeY = 0
    var pixelCount = 0
    var centerPoint = Pair(0, 0)
    val layeredImage: Bitmap


    private fun bfs(y: Int, x: Int) {
        val queue: Queue<Pair<Int, Int>> = LinkedList()
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

                if (mask[curY][curX] == 0) {
                    mask[curY][curX] = 2
                    continue
                }

                visit[curY][curX] = true
                queue.add(Pair(curY, curX))
            }
        }
    }

    init {
        for (y in mask.indices) {
            for (x in mask[y].indices) {
                if (mask[y][x] == 1 && !visit[y][x]) {
//                    bfs(y, x)
                }
            }
        }

//        val ratio = pixelCount.toFloat() / (width * height)
//        if (ratio > 0.1) {
//            centerPoint = Pair(cumulativeY / pixelCount, cumulativeX / pixelCount)

            val pixels = IntArray(width * height)
            var index = 0
            for (y in mask.indices) {
                val sb = StringBuffer()
                for (x in mask[y].indices) {
                    if (mask[y][x] == 0) {
                        pixels[index] = Color.rgb(0, 0, 0)
                    } else if (mask[y][x] == 1) {
                        pixels[index] = Color.rgb(127, 127, 127)
                    } else {
                        pixels[index] = Color.rgb(255, 255, 255)
                    }
                    sb.append(mask[y][x])
                    index++
                }
                Log.d("dd", sb.toString())
            }
            layeredImage = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
//        } else {
//            layeredImage = Bitmap.createBitmap(
//                width, height,
//                Bitmap.Config.ARGB_8888
//            )
//        }
    }
}