package com.gomson.tryangle.network

import android.util.Log
import com.gomson.tryangle.MainActivity
import com.gomson.tryangle.deserializer.AccessTokenDeserializer
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.dto.GuideImageListDTO
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

internal class NetworkManager {

    companion object {
//        private const val URL = "http://14.35.207.80:7778"
        private const val URL = "http://14.35.207.80:7776"
        private val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .callTimeout(1, TimeUnit.MINUTES)
            .build()

        val gson = GsonBuilder().registerTypeAdapter(AccessToken::class.java, AccessTokenDeserializer())
            .create()

        private val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        // 추천 이미지 받을 서버 서비스
        val imageService = retrofit.create(ImageRetrofitService::class.java)
        val accessTokenService = retrofit.create(AccessTokenRetrofitService::class.java)
    }
}