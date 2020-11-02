package com.gomson.tryangle.network

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.dto.GuideImageListDTO
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ImageService(context: Context): BaseService(context) {

    fun recommendImage(bitmap: Bitmap): Response<GuideImageListDTO>? {
        try {
            // TODO: 개발용
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

            if (!hasValidToken()) {
                issueToken(null)
            }

            val call = NetworkManager.imageService.recommendImage(body, accessToken!!.token)
            return call.execute()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getObjectComponentByUrl(url: String, callback: Callback<ArrayList<ObjectComponent>>) {
        try {
            issueToken(null)
            val call = NetworkManager.imageService.getObjectComponentByUrl(url, accessToken!!.token)
            call.enqueue(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}