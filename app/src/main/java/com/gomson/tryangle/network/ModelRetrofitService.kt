package com.gomson.tryangle.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ModelRetrofitService {

    @GET("api/mobile-model/version/model")
    fun getLatestModelVersion(@Query("token") token: String): Call<ResponseBody>

    @GET("models/{version}/resnet50_model.pt")
    fun downloadModel(@Path("version") version: String): Call<ResponseBody>

    @GET("api/mobile-model/version/feature")
    fun getLatestFeatureVersion(@Query("token") token: String): Call<ResponseBody>

    @GET("/vectors/{version}/vecs.bin")
    fun downloadFeature(@Path("version") version: String): Call<ResponseBody>

    @GET("/vectors/{version}/vecs_names.txt")
    fun downloadFeatureNames(@Path("version") version: String): Call<ResponseBody>
}