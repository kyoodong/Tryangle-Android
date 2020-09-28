package com.gomson.tryangle.network

import android.content.Context
import com.gomson.tryangle.R
import com.gomson.tryangle.domain.AccessToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

open class BaseService {

    companion object {
        const val ACCESS_TOKEN_KEY = "accessToken"
        const val EXPIRED_AT_KEY = "expiredAt"
    }

    var accessToken: AccessToken? = null
    var context: Context

    constructor(context: Context) {
        this.context = context
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        val token = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        if (token != null) {
            val expiredAt = sharedPreferences.getLong(EXPIRED_AT_KEY, 0)
            accessToken = AccessToken(token, Date(expiredAt))
        }
    }

    fun hasValidToken(): Boolean {
        return accessToken != null && accessToken!!.expiredAt.time > System.currentTimeMillis()
    }

    // @TODO 여러 리퀘스트가 동시에 호출된 경우 여러번 호출 될 위험이 있음
    fun issueToken(callback: Callback<AccessToken>?) {
        val call = NetworkManager.accessTokenService.issueAccessToken()
        call.enqueue(object : Callback<AccessToken> {
            override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                if (response.isSuccessful) {
                    accessToken = response.body()
                    val sharedPreferences =
                        context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.apply {
                        putString(ACCESS_TOKEN_KEY, accessToken!!.token)
                        putLong(EXPIRED_AT_KEY, accessToken!!.expiredAt.time)
                        apply()
                    }
                }

                callback?.onResponse(call, response)
            }

            override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                callback?.onFailure(call, t)
            }
        })
    }

    fun clearToken() {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply {
            putString(ACCESS_TOKEN_KEY, null)
            putLong(EXPIRED_AT_KEY, 0)
            apply()
        }
        accessToken = null
    }
}