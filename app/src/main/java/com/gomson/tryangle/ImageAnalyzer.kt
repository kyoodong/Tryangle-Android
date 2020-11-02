package com.gomson.tryangle

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.component.*
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.dto.MatchingResult
import com.gomson.tryangle.domain.guide.ObjectGuide
import com.gomson.tryangle.guider.LineGuider
import com.gomson.tryangle.guider.ObjectGuider
import com.gomson.tryangle.guider.PoseGuider
import com.gomson.tryangle.network.ImageService
import com.gomson.tryangle.pose.PoseClassifier
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.tensorflow.lite.examples.posenet.lib.Posenet
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ImageAnalyzer"

class ImageAnalyzer(
    private val context: Context,
    private val analyzeListener: OnAnalyzeListener?
): ImageAnalysis.Analyzer, AutoCloseable {

    private var rotation: Int = 0
    private var needToRequestSegmentation = true
    private var components = ComponentList()
    private val hough = Hough()
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
    private var guidingComponent: Component? = null

    var width = 0
    var height = 0

    init {
        // 토큰 발급
        if (!imageService.hasValidToken()) {
            imageService.issueToken(null)
        }
    }

    external fun MatchFeature(matAddrInput1: Long, matAddrInput2: Long, ratioInRoi: Int): MatchingResult?

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


        // 릴리즈용
//        bitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0,
//            bitmapBuffer.width, bitmapBuffer.height, matrix, true)

        // 개발용
        val option = BitmapFactory.Options()
        option.inScaled = false
        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.test2, option)

        width = bitmap.width
        height = bitmap.height

        poseGuider = PoseGuider(bitmap.width, bitmap.height)
        objectGuider = ObjectGuider(bitmap.width, bitmap.height)
        lineGuider = LineGuider()

        // 세그멘테이션을 요청할 필요가 있다면
        if (needToRequestSegmentation) {
            Log.i(TAG, "Image Segmentation 요청")
            val recommendImageResponse = imageService.recommendImage(bitmap)
                ?: return

            if (recommendImageResponse.isSuccessful) {
                Log.i(TAG, "image Segmentation 성공")
                val body = recommendImageResponse.body()
                if (body == null) {
                    imageProxy.close()
                    return
                }

                this.analyzeListener?.onUpdateRecommendedImage(body.guideImageList)

                val objectComponents = ArrayList<ObjectComponent>()
                for (component in body.guideDTO.objectComponentList) {
                    objectComponents.add(component)
                }

                for (component in body.guideDTO.personComponentList) {
                    objectComponents.add(component)
                }

                // 오브젝트 컴포넌트가 없는 경우 재요청
                if (objectComponents.isEmpty()) {
                    Log.d(TAG, "Empty objectComponentList")
                }

                this.components.clear()

                for (objectComponent in objectComponents) {
                    val layer = Layer(objectComponent.mask, objectComponent.roi)
                    objectComponent.centerPoint = layer.getCenterPoint()
                    objectComponent.area = layer.getArea()

                    val roiImage = Bitmap.createBitmap(bitmap,
                        objectComponent.roi.left, objectComponent.roi.top,
                        objectComponent.roi.getWidth(),
                        objectComponent.roi.getHeight())

                    objectComponent.layer = layer
                    objectComponent.roiImage = roiImage

                    if (layer.layeredImage != null) {
                        if (objectComponent.clazz == ObjectComponent.PERSON) {
                            val gamma = 30
                            val roiX = max(objectComponent.roi.left - gamma, 0)
                            val roiY = max(objectComponent.roi.top - gamma, 0)
                            val roiImage = Bitmap.createBitmap(bitmap,
                                roiX,
                                roiY,
                                min(objectComponent.roi.getWidth() + gamma * 2, bitmap.width - roiX),
                                min(objectComponent.roi.getHeight() + gamma * 2, bitmap.height - roiY))

                            val scaledBitmap = Bitmap.createScaledBitmap(roiImage, MODEL_WIDTH, MODEL_HEIGHT, true)
                            val person = posenet.estimateSinglePose(scaledBitmap)
                            val poseClass = poseClassifier.classify(person.keyPoints.toTypedArray())
                            val personComponent =
                                PersonComponent(
                                    objectComponent.id,
                                    objectComponent.componentId,
                                    objectComponent.guideList,
                                    objectComponent.clazz,
                                    objectComponent.centerPoint,
                                    objectComponent.area,
                                    objectComponent.maskStr,
                                    objectComponent.roiStr,
                                    objectComponent.roiImage,
                                    objectComponent.layer,
                                    person,
                                    poseClass
                                )

                            this.components.add(personComponent)
                        } else {
                            this.components.add(objectComponent)
                        }

                        Log.d(TAG, "레이아웃 이미지 노출")
                    } else {
                        Log.d(TAG, "너무 작은 오브젝트")
                    }
                }

                needToRequestSegmentation = this.components.isEmpty()

                if (this.components.isNotEmpty()) {
                    val newLines = hough.findHoughLine(bitmap)
                    if (newLines != null) {
                        components.addAll(newLines)

                        for (component in components) {
                            if (component !is LineComponent) {
                                continue
                            }

                            val lineComponent = component as LineComponent
                            this.components.add(lineComponent)
                        }
                    }

                    analyzeListener?.onUpdateComponents(components)
                }
            } else {
                Log.i(TAG, "image Segmentation 서버 에러 ${recommendImageResponse.code()}")
            }
        }

        // 원본 카메라 이미지
        val originalImage = Mat()
        Utils.bitmapToMat(bitmap, originalImage)

        var totalCount = 0
        var num = 0
        var averageCount = 0

        // 오브젝트별 이미지
        for (component in components) {
            if (component !is ObjectComponent)
                continue

            val objectComponent = component as ObjectComponent
            val objectImage = Mat()
            Utils.bitmapToMat(objectComponent.roiImage, objectImage)

            val matchingResult = MatchFeature(objectImage.nativeObjAddr, originalImage.nativeObjAddr,
                objectComponent.layer.ratioInRoi) ?: continue

            totalCount += matchingResult.matchRatio
            num++

            if (matchingResult.matchRatio > 30) {
                val maxX = maxOf(matchingResult.pointX1, maxOf(matchingResult.pointX2, maxOf(matchingResult.pointX3, matchingResult.pointX4))).toInt()
                val minX = minOf(matchingResult.pointX1, minOf(matchingResult.pointX2, minOf(matchingResult.pointX3, matchingResult.pointX4))).toInt()
                val maxY = maxOf(matchingResult.pointY1, maxOf(matchingResult.pointY2, maxOf(matchingResult.pointY3, matchingResult.pointY4))).toInt()
                val minY = minOf(matchingResult.pointY1, minOf(matchingResult.pointY2, minOf(matchingResult.pointY3, matchingResult.pointY4))).toInt()

                val width = (maxX - minX).toInt()
                val height = (maxY - minY).toInt()

                val rect = Rect(matchingResult.pointX1.toInt(), matchingResult.pointY1.toInt(), matchingResult.pointX1.toInt() + width, matchingResult.pointY1.toInt() + height)
                val center = Point(minX + width / 2, minY + height / 2)

                // 객체가 너무 많이 움직인 경우
//                if (center.isFar(objectComponent.centerPoint)) {
//                    needToRequestSegmentation = true
//                    Log.d(TAG, "객체가 많이 움직여서 reload")
//                }

//                if (!::layerBitmap.isInitialized) {
//                    layerBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//                }
//                val canvas = Canvas(layerBitmap)
//                canvas.drawColor(Color.argb(0, 0, 0, 0), BlendMode.CLEAR)
//                canvas.drawBitmap(objectComponent.layer.layeredImage!!, null, rect, null)

//                if (guidingComponent != null) {
//                    if (guidingComponent is ObjectComponent && objectComponent.componentId == guidingComponent!!.componentId) {
//                        // 가이드 내에서 도달해야하는 목표지점
//                        val targetPoint = objGuide.targetComponent.centerPoint + objGuide.diffPoint
//                        if (targetPoint.isClose(center)) {
//                            Log.i(TAG, "가이드 목표 도달!")
//                            analyzeListener?.onMatchGuide(objGuide, null)
//                        }
//                    }
//                }

//                analyzeListener?.onUpdateLayerImage(layerBitmap)
            }
        }

        if (num > 0) {
            averageCount = totalCount / num

            if (averageCount < 20) {
                needToRequestSegmentation = true
                Log.d(TAG, "객체 발견하지 못하여 Reload")
            }
        } else {
            needToRequestSegmentation = true
            Log.d(TAG, "num == 0")
        }

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

    interface OnAnalyzeListener {
        fun onUpdateLayerImage(layerBitmap: Bitmap)
        fun onUpdateComponents(components: ArrayList<Component>)
        fun onMatchGuide(guide: Guide, newMainGuide: Guide?)
        fun onUpdateRecommendedImage(imageList: List<String>)
    }
}