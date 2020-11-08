package com.gomson.tryangle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.gomson.tryangle.album.AlbumActivity
import com.gomson.tryangle.databinding.ActivityMainBinding
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.LineGuide
import com.gomson.tryangle.domain.guide.ObjectGuide
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

enum class RatioMode constructor(val width: Int, val height: Int) {
    RATIO_1_1(1, 1),
    RATIO_3_4(3, 4),
    RATIO_9_16(9, 16),
//    RATIO_FULL(0,0)
}

enum class TimerMode constructor(
    val milliseconds: Long,
    val btnImg: Int,
    val info: String,
    val text: String
) {
    TIMER_OFF(0, R.drawable.timer, "타이머 꺼짐", ""),
    TIMER_3S(3000, R.drawable.timer3s, "3초", "3"),
    TIMER_7S(7000, R.drawable.timer7s, "7초", "7"),
    TIMER_10S(10000, R.drawable.timer10s, "10초", "10"),
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

    var currentRatio = RatioMode.RATIO_1_1
    var isFlash = false
    var isGrid = false
    var currentTimerModeIndex = 0
    private lateinit var binding: ActivityMainBinding
//    val currentTimer ;

    val timer = Timer()

    // 마지막에 추천 이미지를 받은 시간
    var last_time = 0


    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
    }

    val ratioPopupViewClickListener = View.OnClickListener { view ->
        var clickRatio = RatioMode.RATIO_3_4
        binding.previewLayout.layoutParams =
            (binding.previewLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                when (view.id) {
                    R.id.ratio3_4 -> {
                        clickRatio = RatioMode.RATIO_3_4
                    }
                    R.id.ratio1_1 -> {
                        clickRatio = RatioMode.RATIO_1_1
                    }
                    R.id.ratio9_16 -> {
                        clickRatio = RatioMode.RATIO_9_16
                    }
                }
            }
        if (clickRatio != currentRatio) {
            setAspectRatioView(clickRatio)
            currentRatio = clickRatio
            imageCapture = getImageCapture(clickRatio.height, clickRatio.width)
            bindCameraConfiguration()
        }
        popupRatioView.dismiss()
    }

    /* 비율에 따른 뷰 설정*/
    private fun setAspectRatioView(ratio:RatioMode){
        binding.previewLayout.post(Runnable {
            (binding.previewLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                when (ratio) {
                    RatioMode.RATIO_1_1 -> {
                        height = binding.previewLayout.width
                        topToTop = binding.topLayout.id
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio1_1)
                    }
                    RatioMode.RATIO_3_4 -> {
                        topToTop = ConstraintSet.PARENT_ID
                        height = 0
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio3_4)
                    }
                    RatioMode.RATIO_9_16 -> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio9_16)
                    }
                }
                binding.previewLayout.postInvalidate()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
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
                currentTimerModeIndex++
                currentTimerModeIndex %= TimerMode.values().size
                val currentTimer = TimerMode.values()[currentTimerModeIndex]
                it.timer.setImageResource(currentTimer.btnImg)
                it.timerTextView.text = currentTimer.info

                binding.timerTextView.text = currentTimer.text
            }
            it.gridLayout.setOnClickListener {
                isGrid = !isGrid
                binding.gridLinesView.visibility = isGrid.visibleIf()
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

        binding.captureButton.setOnClickListener {
            countDownTimer(TimerMode.values()[currentTimerModeIndex])
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.ratioBtn.setOnClickListener {
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
            popupRatioView.showAsDropDown(binding.topLayout, 0, 0)
        }
        binding.moreBtn.setOnClickListener {
            popupMoreView.showAsDropDown(binding.topLayout, 0, 0)
        }
        binding.reverseBtn.setOnClickListener {
            cameraSelector = if (CameraSelector.DEFAULT_BACK_CAMERA == cameraSelector)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            bindCameraConfiguration()
        }

        binding.albumBtn.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
        }

        setAspectRatioView(currentRatio)
        bindCameraConfiguration()
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

        val mediaDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), resources.getString(R.string.app_name))
        if(!mediaDir.exists()){
            mediaDir.mkdir()
        }

//        val mediaDir = externalMediaDirs.firstOrNull()?.let {
//            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
//        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun getImageCapture(heightRatio: Int, widthRatio: Int): ImageCapture {
        val imageCapture = ImageCapture.Builder()
            .apply {
//                todo
//                setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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

        // 기본 카메라 비율을 16:9로 설정
        if (imageCapture == null)
            imageCapture = getImageCapture(currentRatio.height, currentRatio.width)

        // 이미지 분석 모듈
        if (imageAnalysis == null) {
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalyzer = ImageAnalyzer(baseContext, this)
//            imageAnalysis!!.setAnalyzer(cameraExecutor, imageAnalyzer)
        }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalysis
            )
            preview.setSurfaceProvider(binding.previewView.createSurfaceProvider())
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
            binding.layerImageView.setImageBitmap(layerBitmap)
            this.layerBitmap = layerBitmap
            binding.layerImageView.setImageBitmap(layerBitmap)
        }
    }

    override fun onUpdateComponents(components: ArrayList<Component>) {
        this.components.clear()
        this.components.addAll(components)
        this.components.sortByDescending {
            it.priority
        }
    }

    private fun displayGuide() {
        val guide = mainGuide ?: return
        val canvas = Canvas(guideBitmap)
        canvas.drawColor(Color.argb(0, 0, 0, 0), BlendMode.CLEAR)

        if (guide is LineGuide) {
            val lineGuide = guide as LineGuide
            val paint = Paint()
            paint.color = Color.rgb(255, 0, 0)
            paint.isAntiAlias = true
            paint.strokeWidth = 5f
            canvas.drawLine(lineGuide.startPoint.x.toFloat(),
                lineGuide.startPoint.y.toFloat(),
                lineGuide.endPoint.x.toFloat(),
                lineGuide.endPoint.y.toFloat(), paint)
        }

        else if (guide is ObjectGuide) {
            val objectGuide = guide as ObjectGuide
            val layerImage = objectGuide.component.layer.layeredImage ?: return
            val roi = objectGuide.component.roi + objectGuide.diffPoint
            canvas.drawBitmap(layerImage, null, roi.toRect(), null)
        }

        runOnUiThread {
            binding.guideTextView.text = GUIDE_MSG_LIST[guide.guideId]
            binding.guideImageView.setImageBitmap(guideBitmap)
        }
    }

    override fun onGuideUpdate(guides: Array<ArrayList<Guide>>, mainGuide: Guide) {
        this.guideClusters = guides
        this.mainGuide = mainGuide

        if (!::guideBitmap.isInitialized) {
            guideBitmap = Bitmap.createBitmap(
                imageAnalyzer.width,
                imageAnalyzer.height,
                Bitmap.Config.ARGB_8888)
        }

        displayGuide()
        Log.i(TAG, "가이드 업데이트")
    }

    override fun onMatchGuide(guide: Guide, newMainGuide: Guide?) {
        Log.i(TAG, "가이드에 맞음!")
    }

    fun countDownTimer(timerMode: TimerMode) {
        object : CountDownTimer(timerMode.milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timerTextView.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.timerTextView.text = timerMode.text
                takePhoto()
            }
        }.start()
    }
}
