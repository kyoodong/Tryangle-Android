package com.gomson.tryangle

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.gomson.tryangle.domain.LineComponent
import com.gomson.tryangle.domain.ObjectComponent
import com.gomson.tryangle.domain.PersonComponent
import com.gomson.tryangle.dto.MatchingResult
import com.gomson.tryangle.guider.LineGuider
import com.gomson.tryangle.guider.ObjectGuider
import com.gomson.tryangle.guider.PoseGuider
import com.gomson.tryangle.network.ImageService
import com.gomson.tryangle.pose.PoseClassifier
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.tensorflow.lite.examples.posenet.lib.Posenet

private const val TAG = "ImageAnalyzer"

class ImageAnalyzer(
    context: Context,
    val layerBitmapUpdateListener: OnLayerBitmapUpdateListener?
): ImageAnalysis.Analyzer, AutoCloseable {

    private var rotation: Int = 0
    private var needToRequestSegmentation = true
    private var objectComponents: ArrayList<ObjectComponent> = ArrayList()
    private val hough = Hough()
    private var effectiveLines = ArrayList<LineComponent>()
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var bitmap: Bitmap
    private lateinit var layerBitmap: Bitmap
    private val converter: YuvToRgbConverter = YuvToRgbConverter(context)
    private val imageService = ImageService(context)
    private val posenet = Posenet(context)
    private val poseClassifier = PoseClassifier()

    private lateinit var poseGuider: PoseGuider
    private lateinit var objectGuider: ObjectGuider
    private lateinit var lineGuider: LineGuider

    init {
        // 토큰 발급
        if (!imageService.hasValidToken()) {
            imageService.issueToken(null)
        }
    }

    external fun MatchFeature(matAddrInput1: Long, matAddrInput2: Long, ratioInRoi: Int): MatchingResult

    override fun analyze(imageProxy: ImageProxy) {
        rotation = imageProxy.imageInfo.rotationDegrees
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
            )
        }

        // yuv -> RGB
        imageProxy.use { converter.yuvToRgb(imageProxy.image!!, bitmapBuffer) }

        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())

        bitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height, matrix, true)

        poseGuider = PoseGuider(bitmap.width, bitmap.height)
        objectGuider = ObjectGuider(bitmap.width, bitmap.height)
        lineGuider = LineGuider()

        // 세그멘테이션을 요청할 필요가 있다면
        if (needToRequestSegmentation) {
            val segmentationResponse = imageService.imageSegmentation(bitmap)
            if (segmentationResponse.isSuccessful) {
                Log.i(TAG, "image Segmentation 성공")
                if (segmentationResponse.body() == null) {
                    imageProxy.close()
                    return
                }

                val objectComponentList = segmentationResponse.body()!!

                // 오브젝트 컴포넌트가 없는 경우 재요청
                if (objectComponentList.isEmpty()) {
                    Log.d(TAG, "Empty objectComponentList")
                }

                objectComponents.clear()

                for (i in 0 until objectComponentList.size) {
                    val objectComponent = objectComponentList[i]
                    val layer = Layer(objectComponent.maskList, objectComponent.roiList)
                    if (layer.layeredImage != null) {
                        val roiImage = Bitmap.createBitmap(bitmap,
                            objectComponent.roiList[1], objectComponent.roiList[0],
                            objectComponent.roiList[3] - objectComponent.roiList[1],
                            objectComponent.roiList[2] - objectComponent.roiList[0])

                        objectComponent.layer = layer
                        objectComponent.roiImage = roiImage

                        if (objectComponent.clazz == ObjectComponent.PERSON) {
                            val scaledBitmap = Bitmap.createScaledBitmap(roiImage, MODEL_WIDTH, MODEL_HEIGHT, true)
                            val person = posenet.estimateSinglePose(scaledBitmap)
                            val poseClass = poseClassifier.classify(person.keyPoints.toTypedArray())
                            val personComponent = PersonComponent(
                                objectComponent.id,
                                objectComponent.componentId,
                                objectComponent.clazz,
                                objectComponent.centerPointX,
                                objectComponent.centerPointY,
                                objectComponent.area,
                                objectComponent.mask,
                                objectComponent.roi,
                                objectComponent.roiImage,
                                objectComponent.layer,
                                person,
                                poseClass
                            )
                            objectComponents.add(personComponent)
                            poseGuider.guide(personComponent)
                        } else {
                            objectComponents.add(objectComponent)
                        }

                        objectGuider.guide(objectComponent)
                        Log.d(TAG, "레이아웃 이미지 노출")
                    } else {
                        Log.d(TAG, "너무 작은 오브젝트")
                    }
                }

                needToRequestSegmentation = objectComponents.isEmpty()

                if (objectComponents.isNotEmpty()) {
                    effectiveLines.clear()
                    val newLines = hough.findHoughLine(bitmap)
                    if (newLines != null) {
                        effectiveLines.addAll(newLines)

                        for (effectiveLine in effectiveLines) {
                            lineGuider.guide(effectiveLine)
                        }
                    }
                }
            } else {
                Log.i(TAG, "image Segmentation 서버 에러 ${segmentationResponse.code()}")
            }
        }

        // 원본 카메라 이미지
        val originalImage = Mat()
        Utils.bitmapToMat(bitmap, originalImage)

        var t = System.currentTimeMillis()
        var totalCount = 0
        var num = 0
        var averageCount = 0

        // 오브젝트별 이미지
        for (objectComponentImage in objectComponents) {
            val objectImage = Mat()
            Utils.bitmapToMat(objectComponentImage.roiImage, objectImage)

            val matchingResult = MatchFeature(objectImage.nativeObjAddr, originalImage.nativeObjAddr,
                objectComponentImage.layer.ratioInRoi)
            totalCount += matchingResult.matchRatio
            num++

            if (matchingResult.matchRatio > 30) {
                val width =
                    (maxOf(matchingResult.pointX1, maxOf(matchingResult.pointX2, maxOf(matchingResult.pointX3, matchingResult.pointX4))) -
                            minOf(matchingResult.pointX1, minOf(matchingResult.pointX2, minOf(matchingResult.pointX3, matchingResult.pointX4)))).toInt()
                val height =
                    (maxOf(matchingResult.pointY1, maxOf(matchingResult.pointY2, maxOf(matchingResult.pointY3, matchingResult.pointY4))) -
                            minOf(matchingResult.pointY1, minOf(matchingResult.pointY2, minOf(matchingResult.pointY3, matchingResult.pointY4)))).toInt()

                val rect = Rect(matchingResult.pointX1.toInt(), matchingResult.pointY1.toInt(), matchingResult.pointX1.toInt() + width, matchingResult.pointY1.toInt() + height)
                if (!::layerBitmap.isInitialized) {
                    layerBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                }
                val canvas = Canvas(layerBitmap)
                canvas.drawColor(Color.argb(0, 0, 0, 0), BlendMode.CLEAR)
                canvas.drawBitmap(objectComponentImage.layer.layeredImage!!, null, rect, null)

                layerBitmapUpdateListener?.onUpdate(layerBitmap)
            }
            Log.d(TAG, "MatchRatio = ${matchingResult.matchRatio}")
        }

        if (num > 0) {
            averageCount = totalCount / num

            if (averageCount < 20) {
                needToRequestSegmentation = true
            }
        }
        Log.d(TAG, "match time = ${System.currentTimeMillis() - t}")

//                // 추천 이미지 요청
//                imageService.recommendImage(bitmapBuffer, object : Callback<GuideImageListDTO> {
//                    override fun onFailure(call: Call<GuideImageListDTO>, t: Throwable) {
//                        Log.d(MainActivity.TAG, "실패")
//                        t.printStackTrace()
//                    }
//
//                    override fun onResponse(
//                        call: Call<GuideImageListDTO>,
//                        response: Response<GuideImageListDTO>
//                    ) {
//                        val guideImageListDto = response.body() ?: return
//                        guideImageListView.getAdapter().resetImageUrlList()
//                        guideImageListView.getAdapter()
//                            .addImageUrlList(guideImageListDto.guideImageList)
//                        guideImageListDto.guideDTO.componentList
//                        Log.d(TAG, "성공")
//                    }
//                })


        imageProxy.close()
    }

    override fun close() {
        posenet.close()
    }

    interface OnLayerBitmapUpdateListener {
        fun onUpdate(layerBitmap: Bitmap)
    }
}