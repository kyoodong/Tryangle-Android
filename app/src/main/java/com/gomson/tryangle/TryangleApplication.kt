package com.gomson.tryangle

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig

class TryangleApplication: Application(), CameraXConfig.Provider {

    override fun onCreate() {
        super.onCreate()

        CameraX.initialize(baseContext, cameraXConfig)
    }

    override fun getCameraXConfig(): CameraXConfig {
        return Camera2Config.defaultConfig()
    }
}