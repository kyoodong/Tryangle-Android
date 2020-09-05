package com.gomson.tryangle

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val CAMERA_PERMISSION_CODE = 100

    private var useGPU = true
    private lateinit var imageSegmentationModel: ImageSegmentationModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainScope = MainScope()

    private var lensFacing = CameraCharacteristics.LENS_FACING_BACK

    private lateinit var viewModel: MLExecutionViewModel
    private var t1: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }

        // TextureView 세팅
        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceTextureSizeChanged (height, width) ($height, $width)")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val now = SystemClock.uptimeMillis()
                if (now - t1 < 500)
                    return

                t1 = now
                val bitmap = textureView.bitmap
                    ?: return

                viewModel.onApplyModel(bitmap, imageSegmentationModel, inferenceThread)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                Log.i(TAG, "onSurfaceTextureDestroyed")
                return false
            }

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceTextureAvailable (height, width) ($height, $width)")
                openCamera()
            }
        }

        viewModel = ViewModelProvider.AndroidViewModelFactory(application).create(MLExecutionViewModel::class.java)
        viewModel.resultingBitmap.observe(
            this,
            Observer { resultImage ->
                if (resultImage != null) {
                    updateUIWithResults(resultImage)
                }
            }
        )

        imageSegmentationModel = ImageSegmentationModelExecutor(this, useGPU)
    }

    private fun updateUIWithResults(modelExecutionResult: ModelExecutionResult) {
        resultImageView.setImageBitmap(modelExecutionResult.bitmapResult)
        originalImageView.setImageBitmap(modelExecutionResult.bitmapOriginal)
        maskImageView.setImageBitmap(modelExecutionResult.bitmapMaskOnly)

        Log.i(TAG, modelExecutionResult.executionLog)
        val sb = StringBuilder()
        for (key in modelExecutionResult.itemsFound) {
            sb.append("${imageSegmentationModel.labelsArrays[key]} ")
        }
        Log.i(TAG, sb.toString())
    }

    /**
     * 카메라를 여는 함수
     */
    fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // 카메라가 여러개 있을 수 있고, 각 카메라의 ID 를 추출
            val cameraIdList = cameraManager.cameraIdList
            val cameraId = cameraIdList[0]

            // 카메라가 지닌 특징을 추출
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val configurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return

            val sizesForStream = configurationMap.getOutputSizes(SurfaceTexture::class.java)

            // @TODO: 최적의 해상도를 찾는 작업 필요
            val previewSize = sizesForStream[0]

            // 권한이 없는 경우
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // 카메라 열기
            cameraManager.openCamera(cameraId, object: CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "Camera onOpened")

                    // 카메라 Preview 노출
                    showCameraPreview(camera, previewSize)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.i(TAG, "Camera onDisconnected")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, errorCode: Int) {
                    Log.i(TAG, "Camera onError $errorCode")
                    camera.close()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * 카메라 preview 를 화면에 보여주는 함수
     * @param cameraDevice 카메라 디바이스 객체
     * @param previewSize preview 이미지 사이즈
     */
    fun showCameraPreview(cameraDevice: CameraDevice, previewSize: Size) {
        try {
            val texture = textureView.surfaceTexture
                ?: return

            // 버퍼 사이즈
            texture.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surface = Surface(texture)

            // 카메라 캡쳐 설정 (Camera API2 에서는 다양한 세팅 커스터마이징을 지원)
            // Preview 요구 및 자동(기본) 설정 사용
            val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            val surfaceList = ArrayList<Surface>()
            surfaceList.add(surface)

            // 카메라 하드웨어로부터 Java와 통신을 하게 해주는 세션 생성
            cameraDevice.createCaptureSession(surfaceList, object: CameraCaptureSession.StateCallback() {
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.i(TAG, "onConfigureFailed")
                }

                override fun onConfigured(session: CameraCaptureSession) {
                    Log.i(TAG, "onConfigured")
                    updatePreview(session, captureRequestBuilder)
                }
            }, null)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Preview 영상 업데이트 하는 함수
     * @param session 카메라 세션
     * @param captureRequestBuilder 카메라에 요구할 작업이 담긴 빌더 (여기서는 Preview 를 요구하는 내용이 들어있음)
     */
    private fun updatePreview(session: CameraCaptureSession, captureRequestBuilder: CaptureRequest.Builder) {
//        val thread = HandlerThread("CameraPreview")
//        thread.start()
//        val backgroundHandler = Handler(thread.looper)

        try {
            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "카메라 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                openCamera()
            }
        }
    }
}