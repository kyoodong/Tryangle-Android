package com.gomson.tryangle

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.view_camera.view.*
import kotlin.math.abs


enum class RatioMode constructor(val width: Int, val height: Int){
    RATIO_3_4(3,4),
    RATIO_1_1(1,1),
    RATIO_9_16(9,16),
    RATIO_FULL(0,0)
}

@SuppressLint("ViewConstructor")
class CameraView(
    context: Context,
    attributeSet: AttributeSet
) : LinearLayout(context, attributeSet){

    init {
        View.inflate(context, R.layout.view_camera, this)
    }

    val TAG = "CameraView"
    lateinit var cameraManager:CameraManager
    lateinit var sizesForStream:Array<Size>
    var currentRatio: RatioMode = RatioMode.RATIO_1_1

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // TextureView 세팅
        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceTextureSizeChanged (height, width) ($height, $width)")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//                Log.i(TAG, "onSurfaceTextureUpdated")
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
    }

    /**
     * 카메라를 여는 함수
     */
    fun openCamera() {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            // 카메라가 여러개 있을 수 있고, 각 카메라의 ID 를 추출
            val cameraIdList = cameraManager.cameraIdList
            val cameraId = cameraIdList[0]

            // 카메라가 지닌 특징을 추출
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val configurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return

            sizesForStream = configurationMap.getOutputSizes(SurfaceTexture::class.java)

            // @TODO: 최적의 해상도를 찾는 작업 필요
            val previewSize = getOptimalPreviewSize(sizesForStream,currentRatio.width,currentRatio.height)?: return

            // 권한이 없는 경우
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
//                finish()
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

    /**
     * 비율 변경*/

    fun updateRatioMode(ratioMode: RatioMode) {
        currentRatio = ratioMode
        when(ratioMode){
            RatioMode.RATIO_FULL -> {
                val activity = getActivity(context)
                val displaySize = Point()
                activity?.windowManager?.defaultDisplay?.getRealSize(displaySize)
                textureView.setAspectRatio(displaySize.x, displaySize.y)
//                if(isSwappedDimension) {
//                    textureView.setAspectRatio(displaySize.x, displaySize.y)
//                } else {
//                    textureView.setAspectRatio(displaySize.y, displaySize.x)
//                }
            }
            else ->{
                val sizes: Array<Size> = sizesForStream
                val optimalSize: Size? = getOptimalPreviewSize(
                    sizes,
                    ratioMode.width,
                    ratioMode.height
                )
                textureView.setAspectRatio(optimalSize!!.width, optimalSize.height)
            }

        }
        openCamera()
    }

    private fun getOptimalPreviewSize(
        sizes: Array<Size>,
        w: Int,
        h: Int
    ): Size? {
        val ASPECT_TOLERANCE = 0.05
        val targetRatio = w.toDouble() / h
        var optimalSize: Size? = null
        var minDiff = Double.MAX_VALUE

        // Find size
        for (size in sizes) {
            val ratio: Double = size.width.toDouble() / size.height.toDouble()
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height.toDouble() - h)
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height.toDouble() - h)
                }
            }
        }
        Log.d("optimal size",optimalSize!!.width.toString()+" "+optimalSize.height)
        return optimalSize
    }


}