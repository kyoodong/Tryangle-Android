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
import java.lang.Exception

private const val TAG = "ImageService"

class ImageService(context: Context): BaseService(context) {

    fun recommendImage(bitmap: Bitmap, callback: Callback<GuideImageListDTO>) {
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

//        val call = NetworkManager.imageService.recommendImage(body, accessToken!!.token)
//        call.enqueue(callback)

//        try {
//
//            call.enqueue(callback)
//        } catch (e: EOFException) {
//            Log.e(TAG, "객체가 없어 추천 이미지를 받지 못함")
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    fun getObjectComponentByUrl(url: String, callback: Callback<ObjectComponentListDTO>) {
        try {
            issueToken(null)
            val call = NetworkManager.imageService.getObjectComponentByUrl(url, accessToken!!.token)
            call.enqueue(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSpotByLocation(x: Double, y: Double, callback: Callback<List<Spot>>) {
        try {
            issueToken(null)
            val call = NetworkManager.imageService.getSpotByLocation(x, y, accessToken!!.token)
            call.enqueue(callback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}