package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gomson.tryangle.dto.GuideImageListDTO
import com.gomson.tryangle.network.ImageService
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.FastFeatureDetector
import org.opencv.features2d.Feature2D
import org.opencv.features2d.FlannBasedMatcher
import org.opencv.imgproc.Imgproc
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // 마지막에 추천 이미지를 받은 시간
    var last_time = 0L

    // 카메라
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var bitmapBuffer: Bitmap

    private lateinit var imageService: ImageService
    private lateinit var baseLoaderCallback: BaseLoaderCallback
    private lateinit var handler: Handler
    private var isOpenCvLoaded = false

    private val featureDetector: Feature2D
    private val keypoint1: MatOfKeyPoint
    private val descriptor1: Mat
    private val mask1: Mat
    private val keypoint2: MatOfKeyPoint
    private val descriptor2: Mat
    private val mask2: Mat
    private val flann: FlannBasedMatcher
    private val matches: ArrayList<MatOfDMatch>

    external fun MatchFeature(matAddrInput1: Long, matAddrInput2: Long): Int

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")

        featureDetector = FastFeatureDetector.create()
        keypoint1 = MatOfKeyPoint()
        descriptor1 = Mat()
        mask1 = Mat()
        keypoint2 = MatOfKeyPoint()
        descriptor2 = Mat()
        mask2 = Mat()
        flann = FlannBasedMatcher.create()
        matches = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageService = ImageService(baseContext)

        // 토큰 발급
        if (!imageService.hasValidToken()) {
            imageService.issueToken(null)
        }

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

            val converter = YuvToRgbConverter(this)

            imageAnalysis!!.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->

                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    bitmapBuffer = Bitmap.createBitmap(
                        imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
                    )
                }

                // yuv -> RGB
                imageProxy.use { converter.yuvToRgb(imageProxy.image!!, bitmapBuffer) }

                // Grayscale 변환
                val gray = Mat()
                Utils.bitmapToMat(bitmapBuffer, gray)
                var t = System.currentTimeMillis()
                val count = MatchFeature(gray.nativeObjAddr, gray.nativeObjAddr)

                Log.d(TAG, "Count = ${count}")
                Log.d(TAG, "match time = ${System.currentTimeMillis() - t}")

//                featureDetector.detect(gray, keypoint1)
//
//                featureDetector.detectAndCompute(gray, mask1, keypoint1, descriptor1)
//                featureDetector.detectAndCompute(gray, mask2, keypoint2, descriptor2)
//                Log.d(TAG, "detect time = ${System.currentTimeMillis() - t}")
//
//                if (keypoint1.toList().size >= 2 && keypoint2.toList().size >= 2) {
//                    t = System.currentTimeMillis()
//                    flann.knnMatch(descriptor1, descriptor2, matches, 2)
//                }

//                handler.post {
//                    imageView.setImageBitmap(grayBitmap)
//                }

                // 20초에 한 번 재탐색
                val now = SystemClock.uptimeMillis()

                if (now - last_time < 20000) {
                    imageProxy.close()
                    return@Analyzer
                }

                last_time = now

                // 추천 이미지 요청
                imageService.recommendImage(bitmapBuffer, object : Callback<GuideImageListDTO> {
                    override fun onFailure(call: Call<GuideImageListDTO>, t: Throwable) {
                        Log.d(MainActivity.TAG, "실패")
                        t.printStackTrace()
                    }

                    override fun onResponse(
                        call: Call<GuideImageListDTO>,
                        response: Response<GuideImageListDTO>
                    ) {
                        val guideImageListDto = response.body() ?: return
                        guideImageListView.getAdapter().resetImageUrlList()
                        guideImageListView.getAdapter()
                            .addImageUrlList(guideImageListDto.guideImageList)
                        guideImageListDto.guideDTO.componentList
                        Log.d(TAG, "성공")
                    }
                })
                imageProxy.close()
            })
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


}