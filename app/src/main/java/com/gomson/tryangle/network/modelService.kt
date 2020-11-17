package com.gomson.tryangle.network

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.domain.Spot
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.dto.GuideImageListDTO
import com.gomson.tryangle.dto.ObjectComponentListDTO
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.FileOutputStream
import java.lang.Exception

private const val TAG = "ImageService"

class ModelService(context: Context): BaseService(context) {

    fun getLatestModelVersion(callback: Callback<ResponseBody>) {
        val call = NetworkManager.modelService.getLatestModelVersion(accessToken!!.token)
        call.enqueue(callback)
    }

    fun getLatestFeatureVersion(callback: Callback<ResponseBody>) {
        val call = NetworkManager.modelService.getLatestFeatureVersion(accessToken!!.token)
        call.enqueue(callback)
    }

    fun downloadModel(version: String, callback: ModelCallback) {
        val call = NetworkManager.modelService.downloadModel(version)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        saveToFile(responseBody, "resnet50_model.pt")
                        callback.onSuccess()
                    } else {
                        callback.onFailure()
                    }
                } else {
                    callback.onFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onFailure()
            }
        })
    }

    fun downloadFeature(version: String, callback: ModelCallback) {
        val featureCall = NetworkManager.modelService.downloadFeature(version)
        val featureNamesCall = NetworkManager.modelService.downloadFeatureNames(version)
        var count = 0

        featureCall.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        count++
                        saveToFile(responseBody, "vecs.bin")

                        if (count == 2) {
                            callback.onSuccess()
                        }
                    } else {
                        callback.onFailure()
                    }
                } else {
                    callback.onFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onFailure()
            }
        })

        featureNamesCall.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        count++
                        saveToFile(responseBody, "vecs_names.txt")

                        if (count == 2) {
                            callback.onSuccess()
                        }
                    } else {
                        callback.onFailure()
                    }
                } else {
                    callback.onFailure()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onFailure()
            }
        })
    }

    fun saveToFile(responseBody: ResponseBody, name: String) {
        val inputStream = responseBody.byteStream()
        val externalPath = context.getExternalFilesDir(null)
        val path = "${externalPath}/${name}"
        val fos = FileOutputStream(path)
        val buffer = ByteArray(4096)
        var length = 0

        while (true) {
            length = inputStream.read(buffer, 0, buffer.size)
            if (length <= 0)
                break

            fos.write(buffer, 0, length)
        }
        fos.close()
        inputStream.close()
    }

    interface ModelCallback {
        fun onSuccess()
        fun onFailure()
    }
}