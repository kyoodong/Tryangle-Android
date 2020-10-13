package com.gomson.tryangle.network

import com.gomson.tryangle.domain.AccessToken
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST

interface AccessTokenRetrofitService {

    @GET("api/access-token")
    fun issueAccessToken(): Call<AccessToken>
}