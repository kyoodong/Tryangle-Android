package com.gomson.tryangle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.util.Log
import androidx.core.graphics.ColorUtils
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random

class ImageSegmentationModelExecutor(
    context: Context,
    private var useGPU: Boolean = false
) {
    private var gpuDelegate: GpuDelegate? = null

    private val segmentationMasks: ByteBuffer
    private val interpreter: Interpreter

    private var fullTimeExecutionTime = 0L
    private var preprocessTime = 0L
    private var imageSegmentationTime = 0L
    private var maskFlatteningTime = 0L

    private var numberThreads = 4

    init {
        interpreter = getInterpreter(context, imageSegmentationModel, useGPU)
        segmentationMasks = ByteBuffer.allocateDirect(1 * imageSize * imageSize * NUM_CLASSES * 4)
        segmentationMasks.order(ByteOrder.nativeOrder())
    }

    private fun getRandomRGBInt(random: Random) = (255 * random.nextFloat()).toInt()

    companion object {
        private const val TAG = "ImageSegmentationMExec"
        private const val imageSegmentationModel = "deeplabv3_257_mv_gpu.tflite"
        private const val imageSize = 257
        const val NUM_CLASSES = 21
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }

    val segmentColors = IntArray(NUM_CLASSES)
    val labelsArrays = arrayOf(
        "background", "aeroplane", "bicycle", "bird", "boat", "bottle", "bus",
        "car", "cat", "chair", "cow", "dining table", "dog", "horse", "motorbike",
        "person", "potted plant", "sheep", "sofa", "train", "tv"
    )

    init {
        val random = Random(System.currentTimeMillis())
        segmentColors[0] = Color.TRANSPARENT
        for (i in 1 until NUM_CLASSES) {
            segmentColors[i] = Color.argb(
                (128),
                getRandomRGBInt(random),
                getRandomRGBInt(random),
                getRandomRGBInt(random)
            )
        }
    }

    fun execute(data: Bitmap): ModelExecutionResult {
        try {
            fullTimeExecutionTime = SystemClock.uptimeMillis()

            preprocessTime = SystemClock.uptimeMillis()
            val scaledBitmap = Bitmap.createScaledBitmap(
                data,
                imageSize,
                imageSize,
                true
            )

            // Image to ByteBuffer
            val contentArray = bitmapToByteBuffer(scaledBitmap, imageSize, imageSize, IMAGE_MEAN, IMAGE_STD)
            preprocessTime = SystemClock.uptimeMillis() - preprocessTime

            imageSegmentationTime = SystemClock.uptimeMillis()
            interpreter.run(contentArray, segmentationMasks)
            imageSegmentationTime = SystemClock.uptimeMillis() - imageSegmentationTime
            Log.d(TAG, "이미지 세그멘테이션 시간 : $imageSegmentationTime")

            maskFlatteningTime = SystemClock.uptimeMillis()
            val (maskImageApplied, maskOnly, itemsFound) = convertByteBufferMaskToBitmap(
                segmentationMasks, imageSize, imageSize, scaledBitmap,
                segmentColors
            )

            maskFlatteningTime = SystemClock.uptimeMillis() - maskFlatteningTime
            Log.d(TAG, "Time to flatten the mask result $maskFlatteningTime")

            fullTimeExecutionTime = SystemClock.uptimeMillis() - fullTimeExecutionTime
            Log.d(TAG, "총 시간 $fullTimeExecutionTime")

            return ModelExecutionResult(
                maskImageApplied,
                scaledBitmap,
                maskOnly,
                formatExecutionLog(),
                itemsFound
            )
        } catch (e: Exception) {
            val emptyBitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.RGB_565)
            return if (e.message == null) {
                ModelExecutionResult(
                    emptyBitmap, emptyBitmap, emptyBitmap,
                    "",
                    HashSet(0)
                )
            } else {
                ModelExecutionResult(
                    emptyBitmap, emptyBitmap, emptyBitmap,
                    e.message!!,
                    HashSet(0)
                )
            }
        }
    }

    private fun formatExecutionLog(): String {
        val sb = StringBuilder()
        sb.append("Input Image Size: $imageSize x $imageSize\n")
        sb.append("GPU enabled: $useGPU\n")
        sb.append("Number of threads: $numberThreads\n")
        sb.append("Pre-process execution time: $preprocessTime ms\n")
        sb.append("Model execution time: $imageSegmentationTime ms\n")
        sb.append("Mask flatten time: $maskFlatteningTime ms\n")
        sb.append("Full execution time: $fullTimeExecutionTime ms\n")
        return sb.toString()
    }

    private fun convertByteBufferMaskToBitmap(
        inputBuffer: ByteBuffer,
        imageWidth: Int,
        imageHeight: Int,
        backgroundImage: Bitmap,
        colors: IntArray
    ): Triple<Bitmap, Bitmap, Set<Int>> {
        val conf = Bitmap.Config.ARGB_8888
        val maskBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)
        val resultBitmap = Bitmap.createBitmap(imageWidth, imageHeight, conf)
        val scaledBackgroundImage = Bitmap.createScaledBitmap(
            backgroundImage,
            imageWidth,
            imageHeight,
            true
        )
        val segmentBits = Array(imageWidth) { IntArray(imageHeight) }
        val itemsFound = HashSet<Int>()
        inputBuffer.rewind()

        for (y in 0 until imageHeight) {
            for (x in 0 until imageWidth) {
                var maxVal = 0f
                segmentBits[x][y] = 0

                for (c in 0 until NUM_CLASSES) {
                    val value = inputBuffer.getFloat((y * imageWidth * NUM_CLASSES + x * NUM_CLASSES + c) * 4)
                    if (c == 0 || value > maxVal) {
                        maxVal = value
                        segmentBits[x][y] = c
                    }
                }

                itemsFound.add(segmentBits[x][y])
                val newPixelColor = ColorUtils.compositeColors(
                    colors[segmentBits[x][y]],
                    scaledBackgroundImage.getPixel(x, y)
                )
                resultBitmap.setPixel(x, y, newPixelColor)
                maskBitmap.setPixel(x, y, colors[segmentBits[x][y]])
            }
        }

        return Triple(resultBitmap, maskBitmap, itemsFound)
    }

    private fun bitmapToByteBuffer(
        bitmapIn: Bitmap,
        width: Int,
        height: Int,
        mean: Float = 0.0f,
        std: Float = 255.0f
    ): ByteBuffer {
        val bitmapOut = ByteBuffer.allocateDirect(1 * imageSize * imageSize * 3 * 4)
        bitmapOut.order(ByteOrder.nativeOrder())
        bitmapOut.rewind()

        val intValues = IntArray(imageSize * imageSize)
        bitmapIn.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)
        var pixel = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = intValues[pixel++]

                bitmapOut.putFloat(((value shr 16 and 0xFF) - mean) / std)
                bitmapOut.putFloat(((value shr 8 and 0xFF) - mean) / std)
                bitmapOut.putFloat(((value and 0xFF) - mean) / std)
            }
        }

        bitmapOut.rewind()
        return bitmapOut
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean = false
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()
        tfliteOptions.setNumThreads(numberThreads)

        gpuDelegate = null
        if (useGpu) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)
        }

        return Interpreter(loadModelFile(context, modelName), tfliteOptions)
    }

    @Throws(IOException::class)
    private fun loadModelFile(
        context: Context,
        modelFile: String
    ): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }
}