package com.gomson.tryangle.network

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.dto.GuideImageListDTO
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ImageRetrofitService {

    @Multipart
    @POST("api/image/recommend")
    fun recommendImage(@Part image: MultipartBody.Part, @Query("token") token: String)
            : Call<GuideImageListDTO>

    @GET("api/image/component")
    fun getObjectComponentByUrl(@Query("url") url: String, @Query("token") token: String)
            : Call<ArrayList<ObjectComponent>>
}