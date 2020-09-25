package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup_more.view.*
import kotlinx.android.synthetic.main.popup_ratio.view.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class RatioMode constructor(val width: Int, val height: Int){
    RATIO_3_4(3,4),
    RATIO_1_1(1,1),
    RATIO_9_16(9,16),
//    RATIO_FULL(0,0)
}

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    val imageService = NetworkManager.retrofit.create(ImageService::class.java)
    var last_time = 0L

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraProvider: ProcessCameraProvider
    val TAG = "MainActivity"

    lateinit var popupMoreView: PopupWindow
    lateinit var popupRatioView: PopupWindow
    var currentRatio = RatioMode.RATIO_1_1

    val ratioPopupViewClickListener = View.OnClickListener{ view->
        var clickRatio = RatioMode.RATIO_3_4
        previewView.layoutParams  = (previewView.layoutParams as ConstraintLayout.LayoutParams).apply {
            when (view.id) {
                R.id.ratio3_4 -> {
                    clickRatio = RatioMode.RATIO_3_4
                    topToTop = ConstraintSet.PARENT_ID
                    height = 0
                    previewView.requestLayout()
                }
                R.id.ratio1_1 -> {
                    clickRatio = RatioMode.RATIO_1_1
                    height = previewView.width
                    topToTop = topLayout.id
                    previewView.requestLayout()
                }
                R.id.ratio9_16 -> {
                    clickRatio = RatioMode.RATIO_9_16
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    previewView.requestLayout()
                }
            }
        }

        if(clickRatio != currentRatio) {
            currentRatio = clickRatio
            imageCapture = getImageCapture(clickRatio.height, clickRatio.width)
            bindCameraConfiguration()
        }
        popupRatioView.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 카메라 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        layoutInflater.inflate(R.layout.popup_more, null).let {
            popupMoreView = PopupWindow(it, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
            it.flashLayout.setOnClickListener {

            }
            it.timerLayout.setOnClickListener {

            }
            it.gridLayout.setOnClickListener {

            }
            it.settingLayout.setOnClickListener {

            }
        }

        layoutInflater.inflate(R.layout.popup_ratio, null).let{
            it.ratio1_1.setOnClickListener(ratioPopupViewClickListener)
            it.ratio3_4.setOnClickListener(ratioPopupViewClickListener)
            it.ratio9_16.setOnClickListener(ratioPopupViewClickListener)
            it.ratioFull.setOnClickListener(ratioPopupViewClickListener)
            popupRatioView = PopupWindow(it, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        }

        captureButton.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        ratioBtn.setOnClickListener {
            popupRatioView.animationStyle=-1
            popupRatioView.showAsDropDown(topLayout, 0, 0)
        }
        moreBtn.setOnClickListener{
            popupMoreView.showAsDropDown(topLayout,0,0)
        }
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()

            bindCameraConfiguration()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraConfiguration() {
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        if (imageCapture == null)
            imageCapture = getImageCapture(16, 9)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
            preview?.setSurfaceProvider(previewView.createSurfaceProvider())
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
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                    MediaScannerConnection.scanFile(
                        baseContext, arrayOf(photoFile.toString()), arrayOf(photoFile.name), null)
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