package com.gomson.tryangle.network

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.domain.ObjectComponent
import com.gomson.tryangle.dto.GuideImageListDTO
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class ImageService(context: Context): BaseService(context) {

    fun imageSegmentation(bitmap: Bitmap): Response<List<ObjectComponent>> {
        issueToken(null)

        // RGB Bitmap -> ByteArray
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val byteArray = bos.toByteArray()
        val requestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            byteArray
        )
        val body = MultipartBody.Part.createFormData(
            "image",
            "${SystemClock.uptimeMillis()}.jpeg",
            requestBody
        )

        val call = NetworkManager.imageService.imageSegmentation(body, accessToken!!.token)
        return call.execute()
    }

    fun recommendImage(bitmap: Bitmap, callback: Callback<GuideImageListDTO>) {
        // RGB Bitmap -> ByteArray
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val byteArray = bos.toByteArray()
        val requestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"),
            byteArray
        )
        val body = MultipartBody.Part.createFormData(
            "image",
            "${SystemClock.uptimeMillis()}.jpeg",
            requestBody
        )

        if (!hasValidToken()) {
            issueToken(object : Callback<AccessToken> {
                override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                    val call = NetworkManager.imageService.recommendImage(body, accessToken!!.token)
                    call.enqueue(callback)
                }

                override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                    t.printStackTrace()
                }
            })
        } else {
            val call = NetworkManager.imageService.recommendImage(body, accessToken!!.token)
            call.enqueue(object : Callback<GuideImageListDTO> {
                override fun onResponse(
                    call: Call<GuideImageListDTO>,
                    response: Response<GuideImageListDTO>
                ) {
                    if (response.isSuccessful) {
                        callback.onResponse(call, response)
                    } else {
                        clearToken()
                    }
                }

                override fun onFailure(call: Call<GuideImageListDTO>, t: Throwable) {
                    clearToken()
                    callback.onFailure(call, t)
                }
            })
        }
    }
}