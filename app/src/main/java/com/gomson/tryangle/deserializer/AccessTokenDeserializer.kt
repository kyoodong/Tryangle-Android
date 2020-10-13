package com.gomson.tryangle.deserializer

import com.gomson.tryangle.domain.AccessToken
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.util.*

class AccessTokenDeserializer: JsonDeserializer<AccessToken> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): AccessToken {
        val jsonObject = json?.asJsonObject ?: throw NullPointerException("Response Json String is null")

        val id = jsonObject["id"].asLong
        val token = jsonObject["token"].asString
        val createdAtArr = jsonObject["createdAt"].asJsonArray
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, createdAtArr[0].asInt)
        calendar.set(Calendar.MONTH, createdAtArr[1].asInt)
        calendar.set(Calendar.DAY_OF_MONTH, createdAtArr[2].asInt)
        calendar.set(Calendar.HOUR_OF_DAY, createdAtArr[3].asInt)
        calendar.set(Calendar.MINUTE, createdAtArr[4].asInt)
        calendar.set(Calendar.SECOND, createdAtArr[5].asInt)
        val createdAt = calendar.time

        val expiredAtArr = jsonObject["expiredAt"].asJsonArray
        calendar.set(Calendar.YEAR, expiredAtArr[0].asInt)
        calendar.set(Calendar.MONTH, expiredAtArr[1].asInt)
        calendar.set(Calendar.DAY_OF_MONTH, expiredAtArr[2].asInt)
        calendar.set(Calendar.HOUR_OF_DAY, expiredAtArr[3].asInt)
        calendar.set(Calendar.MINUTE, expiredAtArr[4].asInt)
        calendar.set(Calendar.SECOND, expiredAtArr[5].asInt)
        val expiredAt = calendar.time

        val accessCount = jsonObject["accessCount"].asInt
        val ip = jsonObject["ip"].asString

        return AccessToken(id, token, createdAt, expiredAt, accessCount, ip)
    }
}