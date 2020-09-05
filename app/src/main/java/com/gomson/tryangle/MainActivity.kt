package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gomson.tryangle.camera.CameraManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_CODE = 100
        private const val CAPTURE_INTERVAL = 500
    }

    private var useGPU = true
    private lateinit var imageSegmentationModel: ImageSegmentationModelExecutor
    private val inferenceThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val mainScope = MainScope()

    private var lensFacing = CameraCharacteristics.LENS_FACING_BACK

    private lateinit var viewModel: MLExecutionViewModel
    private var lastCapturedTime: Long = 0

    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private val cameraManager = CameraManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, requiredPermissions, CAMERA_PERMISSION_CODE)
        } else {
            init()
        }
    }

    private fun init() {
        // TextureView 세팅
        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                Log.i(TAG, "onSurfaceTextureSizeChanged (height, width) ($height, $width)")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                val now = SystemClock.uptimeMillis()
                if (now - lastCapturedTime < CAPTURE_INTERVAL)
                    return

                lastCapturedTime = now
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
                cameraManager.openCamera(this@MainActivity, textureView)
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
                init()
            }
        }
    }
}