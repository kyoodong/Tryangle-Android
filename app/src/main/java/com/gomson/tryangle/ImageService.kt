package com.gomson.tryangle

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {

    @Multipart
    @POST("image-segmentation")
    fun imageSegmentation(@Part photo: MultipartBody.Part)
            : Call<Map<String, Any>>
}