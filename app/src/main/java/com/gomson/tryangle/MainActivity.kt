package com.gomson.tryangle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
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

//    /**
//     * 카메라를 여는 함수
//     */
//    fun openCamera() {
//        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        try {
//            // 카메라가 여러개 있을 수 있고, 각 카메라의 ID 를 추출
//            val cameraIdList = cameraManager.cameraIdList
//            val cameraId = cameraIdList[0]
//
//            // 카메라가 지닌 특징을 추출
//            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
//            val configurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                ?: return
//
//            val sizesForStream = configurationMap.getOutputSizes(SurfaceTexture::class.java)
//
//            // @TODO: 최적의 해상도를 찾는 작업 필요
//            val previewSize = sizesForStream[0]
//
//            // 권한이 없는 경우
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.CAMERA
//                ) != PackageManager.PERMISSION_GRANTED) {
//                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
//                finish()
//                return
//            }
//
//            // 카메라 열기
//            cameraManager.openCamera(cameraId, object: CameraDevice.StateCallback() {
//                override fun onOpened(camera: CameraDevice) {
//                    Log.i(TAG, "Camera onOpened")
//
//                    // 카메라 Preview 노출
//                    showCameraPreview(camera, previewSize)
//                }
//
//                override fun onDisconnected(camera: CameraDevice) {
//                    Log.i(TAG, "Camera onDisconnected")
//                    camera.close()
//                }
//
//                override fun onError(camera: CameraDevice, errorCode: Int) {
//                    Log.i(TAG, "Camera onError $errorCode")
//                    camera.close()
//                }
//            }, null)
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
//    }
//
//    /**
//     * 카메라 preview 를 화면에 보여주는 함수
//     * @param cameraDevice 카메라 디바이스 객체
//     * @param previewSize preview 이미지 사이즈
//     */
//    fun showCameraPreview(cameraDevice: CameraDevice, previewSize: Size) {
//        try {
//            val texture = textureView.surfaceTexture
//                ?: return
//
//            // 버퍼 사이즈
//            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
//            val surface = Surface(texture)
//
//            // 카메라 캡쳐 설정 (Camera API2 에서는 다양한 세팅 커스터마이징을 지원)
//            // Preview 요구 및 자동(기본) 설정 사용
//            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//            captureRequestBuilder.addTarget(surface)
//            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
//
//            val surfaceList = ArrayList<Surface>()
//            surfaceList.add(surface)
//
//            // 카메라 하드웨어로부터 Java와 통신을 하게 해주는 세션 생성
//            cameraDevice.createCaptureSession(surfaceList, object: CameraCaptureSession.StateCallback() {
//                override fun onConfigureFailed(session: CameraCaptureSession) {
//                    Log.i(TAG, "onConfigureFailed")
//                }
//
//                override fun onConfigured(session: CameraCaptureSession) {
//                    Log.i(TAG, "onConfigured")
//                    updatePreview(session, captureRequestBuilder)
//                }
//            }, null)
//        }
//        catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    /**
//     * Preview 영상 업데이트 하는 함수
//     * @param session 카메라 세션
//     * @param captureRequestBuilder 카메라에 요구할 작업이 담긴 빌더 (여기서는 Preview 를 요구하는 내용이 들어있음)
//     */
//    private fun updatePreview(session: CameraCaptureSession, captureRequestBuilder: CaptureRequest.Builder) {
////        val thread = HandlerThread("CameraPreview")
////        thread.start()
////        val backgroundHandler = Handler(thread.looper)
//
//        try {
//            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
//    }

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