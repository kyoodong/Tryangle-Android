package com.gomson.tryangle

import android.Manifest
import android.animation.AnimatorInflater
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.LineGuide
import com.gomson.tryangle.domain.guide.ObjectGuide
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup_more.view.*
import kotlinx.android.synthetic.main.popup_ratio.view.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


enum class RatioMode constructor(val width: Int, val height: Int) {
    RATIO_3_4(3, 4),
    RATIO_1_1(1, 1),
    RATIO_9_16(9, 16),
//    RATIO_FULL(0,0)
}

enum class TimerMode constructor(val milliseconds: Int, val btnImg: Int) {
    TIMER_OFF(0, R.id.off)
}

class MainActivity : AppCompatActivity(), ImageAnalyzer.OnAnalyzeListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // 카메라
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var baseLoaderCallback: BaseLoaderCallback
    private lateinit var handler: Handler
    private var isOpenCvLoaded = false

    private lateinit var camera: Camera
    private lateinit var converter: YuvToRgbConverter
    private lateinit var layerBitmap: Bitmap
    private lateinit var guideBitmap: Bitmap
    private var guideClusters: Array<ArrayList<Guide>>? = null

    private var components = ArrayList<Component>()
    private var mainGuide: Guide? = null
    private lateinit var imageAnalyzer: ImageAnalyzer

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var preview: Preview? = null


    val TAG = "MainActivity"

    private var COLOR_WHITE = 0
    private var COLOR_LIGHTGRAY = 0
    private var COLOR_LIGHTMINT = 0

    lateinit var popupMoreView: PopupWindow
    lateinit var popupRatioView: PopupWindow

    var currentRatio = RatioMode.RATIO_3_4
    var isFlash = false
    var isGrid = false
    private val recommendedImageUrlList = ArrayList<String>()


    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
    }

    val ratioPopupViewClickListener = View.OnClickListener { view ->
        var clickRatio = RatioMode.RATIO_3_4
        previewLayout.layoutParams =
            (previewLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                when (view.id) {
                    R.id.ratio3_4 -> {
                        clickRatio = RatioMode.RATIO_3_4
                        topToTop = ConstraintSet.PARENT_ID
                        height = 0
                        ratioBtn.setBackgroundResource(R.drawable.ratio3_4)
                        previewLayout.requestLayout()
                    }
                    R.id.ratio1_1 -> {
                        clickRatio = RatioMode.RATIO_1_1
                        height = previewLayout.width
                        topToTop = topLayout.id
                        ratioBtn.setBackgroundResource(R.drawable.ratio1_1)
                        previewLayout.requestLayout()
                    }
                    R.id.ratio9_16 -> {
                        clickRatio = RatioMode.RATIO_9_16
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                        ratioBtn.setBackgroundResource(R.drawable.ratio9_16)
                        previewLayout.requestLayout()
                    }
                }
            }
        if (clickRatio != currentRatio) {
            currentRatio = clickRatio
            imageCapture = getImageCapture(clickRatio.height, clickRatio.width)
            bindCameraConfiguration()
        }
        popupRatioView.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        converter = YuvToRgbConverter(this)

        // 카메라 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        COLOR_WHITE = ContextCompat.getColor(this, R.color.colorWhite)
        COLOR_LIGHTGRAY = ContextCompat.getColor(this, R.color.colorLightgray)
        COLOR_LIGHTMINT = ContextCompat.getColor(this, R.color.colorLightMint)

        layoutInflater.inflate(R.layout.popup_more, null).let {
            popupMoreView = PopupWindow(
                it,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            it.flashLayout.setOnClickListener {
                if (camera.cameraInfo.hasFlashUnit()) {
                    isFlash = !isFlash
                    camera.cameraControl.enableTorch(isFlash)
                    popupMoreView.contentView.flash.isSelected = isFlash
                }
            }
            it.timerLayout.setOnClickListener {

            }
            it.gridLayout.setOnClickListener {
                isGrid = !isGrid
                gridLinesView.visibility = if (isGrid) {
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }
                popupMoreView.contentView.grid.isSelected = isGrid
            }
            it.settingLayout.setOnClickListener {
                val nextIntent = Intent(this, PreferenceActivity::class.java)
                startActivity(nextIntent)
            }
        }
        handler = Handler(mainLooper)
        baseLoaderCallback = object : BaseLoaderCallback(baseContext) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> Log.i(TAG, "OpenCV loaded successfully")
                    else -> super.onManagerConnected(status)
                }
            }
        }

//        captureButton.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        layoutInflater.inflate(R.layout.popup_ratio, null).let {
            it.ratio1_1.setOnClickListener(ratioPopupViewClickListener)
            it.ratio3_4.setOnClickListener(ratioPopupViewClickListener)
            it.ratio9_16.setOnClickListener(ratioPopupViewClickListener)
            popupRatioView = PopupWindow(
                it,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        ratioBtn.setOnClickListener {
            popupRatioView.contentView.ratio9_16.setColorFilter(
                if (currentRatio == RatioMode.RATIO_9_16) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            popupRatioView.contentView.ratio3_4.setColorFilter(
                if (currentRatio == RatioMode.RATIO_3_4) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            popupRatioView.contentView.ratio1_1.setColorFilter(
                if (currentRatio == RatioMode.RATIO_1_1) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )


            popupRatioView.animationStyle = -1
            popupRatioView.showAsDropDown(topLayout, 0, 0)
        }
        moreBtn.setOnClickListener {
            popupMoreView.showAsDropDown(topLayout, 0, 0)
        }
        reverseBtn.setOnClickListener {
            cameraSelector = if (CameraSelector.DEFAULT_BACK_CAMERA == cameraSelector)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            bindCameraConfiguration()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            Log.d(
                TAG,
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            );
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION,
                this,
                baseLoaderCallback
            );
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            isOpenCvLoaded = true;
        }
    }

    /**
     * 필요한 권한을 로드하는 함수
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 사진을 저장할 위치를 리턴하는 함수
     */
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun getImageCapture(heightRatio: Int, widthRatio: Int): ImageCapture {
        val imageCapture = ImageCapture.Builder()
            .apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                setTargetAspectRatioCustom(Rational(widthRatio, heightRatio))
            }
            .build()
        preview = Preview.Builder()
            .setTargetAspectRatioCustom(Rational(widthRatio, heightRatio))
            .build()
        return imageCapture
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()

            bindCameraConfiguration()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 카메라 서비스 설정값을 지정하는 함수
     */
    private fun bindCameraConfiguration() {
        val preview = Preview.Builder()
            .build()

        // 카메라 뒷면 선택
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // 기본 카메라 비율을 16:9로 설정
        if (imageCapture == null)
            imageCapture = getImageCapture(16, 9)

        // 이미지 분석 모듈
        if (imageAnalysis == null) {
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer = ImageAnalyzer(baseContext, this)
            imageAnalysis!!.setAnalyzer(cameraExecutor, imageAnalyzer)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalysis
            )
            preview.setSurfaceProvider(previewView.createSurfaceProvider())
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    MediaScannerConnection.scanFile(
                        baseContext, arrayOf(photoFile.toString()), arrayOf(photoFile.name), null
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "카메라 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onUpdateLayerImage(layerBitmap: Bitmap) {
        this.layerBitmap = layerBitmap

        runOnUiThread {
            this.layerBitmap = layerBitmap
            layerImageView.setImageBitmap(layerBitmap)
        }
    }

    override fun onUpdateComponents(components: ArrayList<Component>) {
        Log.i(TAG, "컴포넌트 업데이트")
        this.components.clear()
        this.components.addAll(components)
        this.components.sortByDescending {
            it.priority
        }

        val objectComponents = ArrayList<ObjectComponent>()
        for (component in this.components) {
            if (component is ObjectComponent) {
                objectComponents.add(component)
            }
        }

        // 여러 객체가 있을 때 객체를 선택하도록 함
        if (objectComponents.size > 1) {
            guideTextView.text = getString(R.string.select_main_object)

            val layoutWidth = previewLayout.width
            val layoutHeight = previewLayout.height

            for (component in objectComponents) {
                val imageView = ImageView(baseContext)
                imageView.x = component.centerPoint.x.toFloat() * layoutWidth / 640
                imageView.y = component.centerPoint.y.toFloat() * layoutHeight / 640
                imageView.layoutParams = FrameLayout.LayoutParams(50, 50)
                val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.argb(255, 0, 0, 0))
                imageView.setImageBitmap(bitmap)
                imageView.setOnClickListener {
//                        it.
                }
                previewLayout.addView(imageView)
            }
        }
    }

//    private fun displayGuide() {
//        val guide = mainGuide ?: return
//        val canvas = Canvas(guideBitmap)
//        canvas.drawColor(Color.argb(0, 0, 0, 0), BlendMode.CLEAR)
//
//        if (guide is LineGuide) {
//            val lineGuide = guide as LineGuide
//            val paint = Paint()
//            paint.color = Color.rgb(255, 0, 0)
//            paint.isAntiAlias = true
//            paint.strokeWidth = 5f
//            canvas.drawLine(lineGuide.startPoint.x.toFloat(),
//                lineGuide.startPoint.y.toFloat(),
//                lineGuide.endPoint.x.toFloat(),
//                lineGuide.endPoint.y.toFloat(), paint)
//        }
//
//        else if (guide is ObjectGuide) {
//            val objectGuide = guide as ObjectGuide
//            val layerImage = objectGuide.targetComponent.layer.layeredImage ?: return
//            val roi = objectGuide.targetComponent.roi + objectGuide.diffPoint
//            canvas.drawBitmap(layerImage, null, roi.toRect(), null)
//        }
//
//        runOnUiThread {
//            guideTextView.text = GUIDE_MSG_LIST[guide.guideId]
//            guideImageView.setImageBitmap(guideBitmap)
//        }
//    }

//    override fun onGuideUpdate(guides: Array<ArrayList<Guide>>, mainGuide: Guide) {
//        this.guideClusters = guides
//        this.mainGuide = mainGuide
//
//        if (!::guideBitmap.isInitialized) {
//            guideBitmap = Bitmap.createBitmap(
//                imageAnalyzer.width,
//                imageAnalyzer.height,
//                Bitmap.Config.ARGB_8888)
//        }
//
//        displayGuide()
//        Log.i(TAG, "가이드 업데이트")
//    }

    override fun onUpdateRecommendedImage(imageList: List<String>) {
        Log.i(TAG, "추천 이미지 ${imageList.size} 개 도착!")
        this.recommendedImageUrlList.clear()
        this.recommendedImageUrlList.addAll(imageList)
    }

    override fun onMatchGuide(guide: Guide, newMainGuide: Guide?) {
        Log.i(TAG, "가이드에 맞음!")
//        if (guide.targetComponent is ObjectComponent) {
//            val component = guide.targetComponent as ObjectComponent
//            val layoutParams = thumbUp.layoutParams as ConstraintLayout.LayoutParams
//            layoutParams.leftMargin = component.centerPoint.x
//            layoutParams.topMargin = component.centerPoint.y
//            thumbUp.visibility = View.VISIBLE
//
//            // 좋아요 아이콘 잠깐 보여주기
//            AnimatorInflater.loadAnimator(baseContext, R.animator.thumb_up)
//                .apply {
//                    setTarget(thumbUp)
//                    start()
//                }
//        }
    }
}
