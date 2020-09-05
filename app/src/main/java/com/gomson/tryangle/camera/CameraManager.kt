package com.gomson.tryangle.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView

class CameraManager {

    companion object {
        private const val TAG = "CameraManager"
    }

    /**
     * 카메라를 여는 함수
     */
    fun openCamera(context: Context, textureView: TextureView) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

            // 카메라 열기
            cameraManager.openCamera(cameraId, object: CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "Camera onOpened")

                    // 카메라 Preview 노출
                    showCameraPreview(camera, previewSize, textureView)
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
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * 카메라 preview 를 화면에 보여주는 함수
     * @param cameraDevice 카메라 디바이스 객체
     * @param previewSize preview 이미지 사이즈
     */
    fun showCameraPreview(cameraDevice: CameraDevice, previewSize: Size, textureView: TextureView) {
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
        try {
            session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
}