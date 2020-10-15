package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.LineGuide
import com.gomson.tryangle.domain.guide.ObjectGuide
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    private lateinit var converter: YuvToRgbConverter
    private lateinit var layerBitmap: Bitmap
    private lateinit var guideBitmap: Bitmap
    private var guideClusters: Array<ArrayList<Guide>>? = null

    private var components = ArrayList<Component>()
    private var mainGuide: Guide? = null
    private lateinit var imageAnalyzer: ImageAnalyzer

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        converter = YuvToRgbConverter(this)

        // 카메라 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        handler = Handler(mainLooper)
        baseLoaderCallback = object: BaseLoaderCallback(baseContext) {
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

        ratio_1_1.setOnClickListener {
            imageCapture = getImageCapture(1, 1)
            bindCameraConfiguration()
        }

        ratio_16_9.setOnClickListener {
            imageCapture = getImageCapture(16, 9)
            bindCameraConfiguration()
        }

        ratio_4_3.setOnClickListener {
            imageCapture = getImageCapture(4, 3)
            bindCameraConfiguration()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            isOpenCvLoaded = true;
        }
    }

    private fun getImageCapture(heightRatio: Int, widthRatio: Int): ImageCapture {
        val imageCapture = ImageCapture.Builder()
            .apply {
                setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                setTargetAspectRatioCustom(Rational(widthRatio, heightRatio))
            }
            .build()
        return imageCapture
    }

    /**
     * 필요한 권한을 로드하는 함수
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
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

    /**
     * 카메라 서비스 시작하는 메소드
     */
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

        guideTextView.text = GUIDE_MSG_LIST[guide.guideId]

        runOnUiThread {
            guideImageView.setImageBitmap(guideBitmap)
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
}