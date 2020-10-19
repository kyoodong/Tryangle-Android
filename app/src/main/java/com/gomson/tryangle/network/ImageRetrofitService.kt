package com.gomson.tryangle.network

import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.dto.GuideImageListDTO
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface ImageRetrofitService {

    @Multipart
    @POST("api/image/segmentation")
    fun imageSegmentation(@Part image: MultipartBody.Part, @Query("token") token: String)
            : Call<List<ObjectComponent>>

    @Multipart
    @POST("api/image/recommend")
    fun recommendImage(@Part image: MultipartBody.Part, @Query("token") token: String)
            : Call<GuideImageListDTO>
}