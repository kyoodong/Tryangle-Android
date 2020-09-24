package com.gomson.tryangle.network

import com.gomson.tryangle.dto.GuideImageListDTO
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*

interface ImageRetrofitService {

    @Multipart
    @POST("api/image/recommend")
    fun recommendImage(@Part image: MultipartBody.Part, @Query("token") token: String)
            : Call<GuideImageListDTO>
}