package com.gomson.tryangle.network

import com.gomson.tryangle.deserializer.AccessTokenDeserializer
import com.gomson.tryangle.domain.AccessToken
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

private const val TIMEOUT = 3L

internal class NetworkManager {

    companion object {
//        private const val URL = "http://121.139.71.162:7778"
        const val URL = "http://121.139.71.162:7776"

        private val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.MINUTES)
            .readTimeout(TIMEOUT, TimeUnit.MINUTES)
            .writeTimeout(TIMEOUT, TimeUnit.MINUTES)
            .callTimeout(TIMEOUT, TimeUnit.MINUTES)
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