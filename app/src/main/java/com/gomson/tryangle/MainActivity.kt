package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
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

    val imageService = NetworkManager.retrofit.create(ImageService::class.java)
    var last_time = 0L

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 카메라 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        captureButton.setOnClickListener { takePhoto() }

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

        // TextureView 세팅
//        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener {
//            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//                Log.i(TAG, "onSurfaceTextureSizeChanged (height, width) ($height, $width)")
//            }
//
//            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//                val now = SystemClock.uptimeMillis()
//
//                if (now - last_time < 20000) {
//                    return
//                }
//
//                last_time = now
//                val bitmap = textureView.bitmap ?: return
//                val baos = ByteArrayOutputStream()
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//                val byteArray = baos.toByteArray()
//                val requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), byteArray)
//                val body = MultipartBody.Part.createFormData("file", "${SystemClock.uptimeMillis()}.jpeg", requestBody)
//                val call = imageService.imageSegmentation(body)
//                call.enqueue(object : Callback<Map<String, Any>> {
//                    override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
//                        Log.d(TAG, "실패")
//                        t.printStackTrace()
//                    }
//
//                    override fun onResponse(
//                        call: Call<Map<String, Any>>,
//                        response: Response<Map<String, Any>>
//                    ) {
//                        Log.d(TAG, "성공")
//                    }
//                })
//            }
//
//            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                Log.i(TAG, "onSurfaceTextureDestroyed")
//                return false
//            }
//
//            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//                Log.i(TAG, "onSurfaceTextureAvailable (height, width) ($height, $width)")
//                openCamera()
//            }
//        }
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
        val preview = Preview.Builder()
            .build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        if (imageCapture == null)
            imageCapture = getImageCapture(16, 9)

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
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