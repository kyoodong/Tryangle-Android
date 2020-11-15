package com.gomson.tryangle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.location.Location
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.gomson.tryangle.domain.Point
import com.gomson.tryangle.domain.Roi
import com.gomson.tryangle.domain.Spot
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ComponentList
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.`object`.ObjectGuide
import com.gomson.tryangle.domain.guide.`object`.PoseGuide
import com.gomson.tryangle.dto.GuideImageListDTO
import com.gomson.tryangle.dto.MatchingResult
import com.gomson.tryangle.guider.LineGuider
import com.gomson.tryangle.network.ImageService
import com.gomson.tryangle.pose.PoseClassifier
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.tensorflow.lite.examples.posenet.lib.Posenet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ImageAnalyzer"
private const val WAIT_SEGMENT_TIME = 600

class ImageAnalyzer(
    private val context: Context,
    private val analyzeListener: OnAnalyzeListener?
): ImageAnalysis.Analyzer, AutoCloseable {

    private var rotation: Int = 0
    private var needToRequestSegmentation = true
    private var waitSegmentStartTime: Long = 0
    private var components = ComponentList()
    private val hough = Hough()
    private lateinit var bitmapBuffer: Bitmap
    lateinit var bitmap: Bitmap
    private lateinit var prevBitmap: Bitmap
    private lateinit var lastCapturedBitmap: Bitmap
    private val converter: YuvToRgbConverter = YuvToRgbConverter(context)
    private val imageService = ImageService(context)
    private val posenet = Posenet(context)
    private val poseClassifier = PoseClassifier()
    private val lineGuider = LineGuider()
    private var guidingComponent: Component? = null
    private var targetComponent: Component? = null
    private var guidingGuide: Guide? = null
    private var failToDetectObjectStartTime: Long = 0
    private var ratio: Float = 1f
    var latestLocation: Location? = null
    private var hasSpotImages = false
    private var isProcessingSegmentation = false

    var width = 0
    var height = 0
    private var lastAnalyzeTime = System.currentTimeMillis()

    init {
        // 토큰 발급
        if (!imageService.hasValidToken()) {
            imageService.issueToken(null)
        }
    }

    external fun MatchFeature(matAddrInput1: Long, matAddrInput2: Long, ratioInRoi: Int): MatchingResult?
    external fun ImageRetrieval(matAddrInput: Long): Array<String>

    override fun analyze(imageProxy: ImageProxy) {
        val curTime = System.currentTimeMillis()
        if (curTime - lastAnalyzeTime < 100) {
            Thread.sleep(50)
            imageProxy.close()
            return
        }

        lastAnalyzeTime = System.currentTimeMillis()

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
        bitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0,
            bitmapBuffer.width, bitmapBuffer.height, matrix, true)

        // 개발용
//        val option = BitmapFactory.Options()
//        option.inScaled = false
//        bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.test5, option)

        width = bitmap.width
        height = (bitmap.width * ratio).toInt()
        bitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.height - height) / 2, width, height)

        width = 640
        height = (width * ratio).toInt()
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        // 세그멘테이션을 요청할 필요가 있다면
        if (needToRequestSegmentation) {
            requestSegmentation()
        } else {
            // 오브젝트별 이미지
            if (guidingComponent != null) {
                if (guidingComponent is ObjectComponent) {
                    traceGuidingObjectComponent()
                }
            }

            traceImage()
        }

        imageProxy.close()
    }

    private fun traceImage() {
        val curImage = Mat()
        val prevImage = Mat()
        Utils.bitmapToMat(bitmap, curImage)
        Utils.bitmapToMat(lastCapturedBitmap, prevImage)

        val matchingResult = MatchFeature(curImage.nativeObjAddr, prevImage.nativeObjAddr, 100)
        if (matchingResult != null && matchingResult.matchRatio > 10) {
            failToDetectObjectStartTime = 0L
        } else if (failToDetectObjectStartTime == 0L) {
            failToDetectObjectStartTime = System.currentTimeMillis()
            Log.d(TAG, "카메라에 큰 변화를 감지")
        } else if (System.currentTimeMillis() - failToDetectObjectStartTime < WAIT_SEGMENT_TIME) {
            needToRequestSegmentation = true
            failToDetectObjectStartTime = 0
            val canvas = Canvas(lastCapturedBitmap)
            canvas.drawColor(Color.rgb(255, 255, 255))
            Log.d(TAG, "카메라에 큰 변화로 인한 세그멘테이션 재요청")
        }
    }

    private fun traceGuidingObjectComponent() {
        // 원본 카메라 이미지
        val originalImage = Mat()
        Utils.bitmapToMat(bitmap, originalImage)

        val guidingComponent = guidingComponent as ObjectComponent
        val objectImage = Mat()
        Utils.bitmapToMat(guidingComponent.croppedImage, objectImage)

        val matchingResult = MatchFeature(objectImage.nativeObjAddr, originalImage.nativeObjAddr,
            guidingComponent.layer.ratioInRoi)
        if (matchingResult != null) {
            if (matchingResult.matchRatio > 30) {
                val maxX = maxOf(matchingResult.pointX1, maxOf(matchingResult.pointX2, maxOf(matchingResult.pointX3, matchingResult.pointX4))).toInt()
                val minX = minOf(matchingResult.pointX1, minOf(matchingResult.pointX2, minOf(matchingResult.pointX3, matchingResult.pointX4))).toInt()
                val maxY = maxOf(matchingResult.pointY1, maxOf(matchingResult.pointY2, maxOf(matchingResult.pointY3, matchingResult.pointY4))).toInt()
                val minY = minOf(matchingResult.pointY1, minOf(matchingResult.pointY2, minOf(matchingResult.pointY3, matchingResult.pointY4))).toInt()

                val width = (maxX - minX)
                val height = (maxY - minY)

                val center = Point(minX + width / 2, minY + height / 2)
                val leftTop = Point(minX, minY)

                guidingComponent.roi = Roi(minX, maxX, minY, maxY)

                // 가이드 내에서 도달해야하는 목표지점
                val guide = guidingGuide
                if (guide == null) {
                    if (targetComponent != null) {
                        val targetComponent = targetComponent as ObjectComponent

                        // 객체간에 충분히 가까워 진 경우
                        val targetPoint = targetComponent.centerPoint
                        val curRoi = Roi(minX, maxX, minY, maxY)
                        val iou = if (targetComponent.roi < curRoi) {
                            targetComponent.roi.getIou(curRoi)
                        } else {
                            curRoi.getIou(targetComponent.roi)
                        }
                        if (targetPoint.isRoughClose(center) || iou > 0.75) {
                            analyzeListener?.onMatchComponent()
                        }
                    }
                } else if (guide is ObjectGuide) {
                    if (guide.isMatch(Roi(minX, maxX, minY, maxY))) {
                        Log.i(TAG, "가이드 목표 도달!")
                        analyzeListener?.onMatchGuide()
                    }
                } else if (guide is PoseGuide && guidingComponent is PersonComponent) {
                    val alpha = 30
                    val croppedX = max(minX - alpha, 0)
                    val croppedY = max(minY - alpha, 0)
                    val croppedWidth = min(width + alpha * 2, this.width - croppedX)
                    val croppedHeight = min(height + alpha * 2, this.height - croppedY)

                    val croppedImage = Bitmap.createBitmap(bitmap, croppedX, croppedY, croppedWidth, croppedHeight)
                    val rescaledImage = Bitmap.createScaledBitmap(croppedImage, MODEL_WIDTH, MODEL_HEIGHT, true)
                    guidingComponent.person = posenet.estimateSinglePose(rescaledImage)

                    if (guide.isMatch(guidingComponent)) {
                        Log.i(TAG, "가이드 목표 도달!")
                        analyzeListener?.onMatchGuide()
                    }
                }
                analyzeListener?.onUpdateGuidingComponentPosition(width, height, leftTop)
            }
        }
    }

    private fun requestSegmentation() {
        if (isProcessingSegmentation)
            return

        if (waitSegmentStartTime == 0L) {
            waitSegmentStartTime = System.currentTimeMillis()
            prevBitmap = bitmap.copy(bitmap.config, true)
            return
        }

        val curImage = Mat()
        val prevImage = Mat()
        Utils.bitmapToMat(bitmap, curImage)
        Utils.bitmapToMat(prevBitmap, prevImage)

        val matchingResult = MatchFeature(curImage.nativeObjAddr, prevImage.nativeObjAddr, 100)

        // 현재 프레임과 직전 프레임과의 싱크가 30% 이상 맞지 않으면 다시 시간 카운팅
        if (matchingResult == null || matchingResult.matchRatio < 30) {
            waitSegmentStartTime = 0
            Log.d(TAG, "직전프레임과의 싱크가 30%이상 맞지 않음")
            return
        }

        // WAIT_SEGMENT_TIME 동안 이 상태가 유지되었다면 세그먼테이션 재요청
        if (System.currentTimeMillis() - waitSegmentStartTime < WAIT_SEGMENT_TIME) {
            return
        }

        if (::lastCapturedBitmap.isInitialized) {
            Utils.bitmapToMat(bitmap, curImage)
            Utils.bitmapToMat(lastCapturedBitmap, prevImage)
            val matchingResult = MatchFeature(curImage.nativeObjAddr, prevImage.nativeObjAddr, 100)

            // 이 전에 요청했던 이미지와 50% 이상 싱크가 일치한다면 요청하지 않음
            if (matchingResult != null && matchingResult.matchRatio > 30) {
                waitSegmentStartTime = 0
                return
            }
        }

        lastCapturedBitmap = bitmap.copy(bitmap.config, true)
        Log.i(TAG, "Image Segmentation 요청")

        val result = ImageRetrieval(curImage.nativeObjAddr)

        isProcessingSegmentation = true
        imageService.recommendImage(bitmap, object: retrofit2.Callback<GuideImageListDTO> {
            val bitmap = lastCapturedBitmap.copy(lastCapturedBitmap.config, true)

            override fun onResponse(
                call: Call<GuideImageListDTO>,
                response: Response<GuideImageListDTO>
            ) {
                isProcessingSegmentation = false
                if (response.isSuccessful) {
                    Log.i(TAG, "image Segmentation 성공")
                    val body = response.body()
                        ?: return

                    if (body.guideDTO.objectComponentList.isEmpty()
                        && body.guideDTO.personComponentList.isEmpty()) {
                        Log.d(TAG, "Empty objectComponentList")
                        val canvas = Canvas(lastCapturedBitmap)
                        canvas.drawColor(Color.rgb(255, 255, 255))
                        return
                    }

                    if (body.guideImageList.isEmpty()) {
                        Log.d(TAG, "Empty guideImageList")
                        val canvas = Canvas(lastCapturedBitmap)
                        canvas.drawColor(Color.rgb(255, 255, 255))
                        return
                    }

                    body.guideDTO.deployMask()
                    body.guideDTO.initPerson()
                    this@ImageAnalyzer.analyzeListener?.onUpdateRecommendedImage(body.guideImageList)

                    val objectComponents = ArrayList<ObjectComponent>()
                    for (component in body.guideDTO.objectComponentList) {
                        objectComponents.add(component)
                    }

                    for (component in body.guideDTO.personComponentList) {
                        objectComponents.add(component)
                    }

                    needToRequestSegmentation = false
                    this@ImageAnalyzer.components.clear()

                    for (objectComponent in objectComponents) {
                        objectComponent.refreshLayer(bitmap, Guide.PINK, Guide.TRANS_PINK)

                        if (objectComponent.layer.layeredImage != null) {
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
                                        objectComponent.mask,
                                        objectComponent.roiStr,
                                        objectComponent.croppedImage,
                                        objectComponent.layer,
                                        person,
                                        poseClass
                                    )

                                this@ImageAnalyzer.components.add(personComponent)
                            } else {
                                this@ImageAnalyzer.components.add(objectComponent)
                            }

                            Log.d(TAG, "레이아웃 이미지 노출")
                        } else {
                            Log.d(TAG, "너무 작은 오브젝트")
                        }
                    }

                    needToRequestSegmentation = false
                    lastCapturedBitmap = bitmap.copy(bitmap.config, true)

                    if (this@ImageAnalyzer.components.isNotEmpty()) {
                        val newLines = hough.findHoughLine(bitmap)
                        if (newLines != null) {
                            for (lineComponent in newLines) {
                                lineGuider.initGuideList(lineComponent)
                            }
                            components.addAll(newLines)
                        }

                        analyzeListener?.onUpdateComponents(components)
                    }
                } else {
                    val canvas = Canvas(lastCapturedBitmap)
                    canvas.drawColor(Color.rgb(255, 255, 255))
                    Log.i(TAG, "image Segmentation 서버 에러 ${response.code()}")
                }
            }

            override fun onFailure(call: Call<GuideImageListDTO>, t: Throwable) {
                isProcessingSegmentation = false
                val canvas = Canvas(lastCapturedBitmap)
                canvas.drawColor(Color.rgb(255, 255, 255))
                Log.i(TAG, "image Segmentation 서버 에러 ${t.message}")
                t.printStackTrace()
            }
        })

        if (latestLocation != null && !hasSpotImages) {
            val latestLocation = latestLocation
                ?: return

            hasSpotImages = true
            Log.i(TAG, "Spot 요청")
            imageService.getSpotByLocation(bitmap, latestLocation.latitude, latestLocation.longitude, object:
                Callback<List<Spot>> {
                override fun onResponse(
                    call: Call<List<Spot>>,
                    response: Response<List<Spot>>
                ) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Spot 로딩 성공")
                        val spotList = response.body()
                            ?: return

                        hasSpotImages = true
                        this@ImageAnalyzer.analyzeListener?.onUpdateSpot(spotList as ArrayList<Spot>)
                    } else {
                        hasSpotImages = false
                        Log.i(TAG, "Spot 로딩 실패 ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Spot>>, t: Throwable) {
                    Log.i(TAG, "Spot 로딩 실패 ${t.message}")
                    hasSpotImages = false
                }
            })
        }

        waitSegmentStartTime = 0
    }

    override fun close() {
        posenet.close()
    }

    fun setGuide(guidingComponent: Component?, targetComponent: Component?, guide: Guide?) {
        this.guidingComponent = guidingComponent
        this.targetComponent = targetComponent
        this.guidingGuide = guide
    }

    fun setRatio(ratio: Float) {
        this.ratio = ratio
    }

    interface OnAnalyzeListener {
        fun onUpdateComponents(components: ArrayList<Component>)
        fun onMatchGuide()
        fun onUpdateRecommendedImage(imageList: ArrayList<String>)
        fun onUpdateGuidingComponentPosition(width: Int, height: Int, leftTopPoint: Point)
        fun onMatchComponent()
        fun onUpdateSpot(spotList: ArrayList<Spot>)
        fun onUpdateRecommendedCacheImage(imageList: ArrayList<String>)
    }
}