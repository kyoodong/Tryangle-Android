package com.gomson.tryangle

import android.content.Context

private const val TOKEN_INTERVAL = 1000 * 60 * 60 * 24 * 14
private const val LAST_VERSION_CHECKED = "LAST_VERSION_CHECKED"

class TokenManager(
    private val context: Context
) {

    fun getLastVersionCheckTime(): Long {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LAST_VERSION_CHECKED, 0)
    }

    fun setLastVersionCheckTime(time: Long) {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(LAST_VERSION_CHECKED, time)
        editor.commit()
    }

    fun isExpired(): Boolean {
        // @TODO invalid token 확인
        return System.currentTimeMillis() - getLastVersionCheckTime() >= TOKEN_INTERVAL
    }
}