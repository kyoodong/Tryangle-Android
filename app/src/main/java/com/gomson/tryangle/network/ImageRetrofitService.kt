package com.gomson.tryangle.network

import com.gomson.tryangle.dto.GuideImageListDTO
import com.gomson.tryangle.dto.ObjectComponentListDTO
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ImageRetrofitService {

    @Multipart
    @POST("api/image/recommend")
    fun recommendImage(@Part image: MultipartBody.Part, @Query("token") token: String)
            : Call<GuideImageListDTO>

    @GET("api/image/component")
    fun getObjectComponentByUrl(@Query("url") url: String, @Query("token") token: String)
            : Call<ObjectComponentListDTO>
}