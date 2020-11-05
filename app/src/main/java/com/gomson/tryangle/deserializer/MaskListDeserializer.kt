package com.gomson.tryangle.deserializer

import android.util.Base64
import com.gomson.tryangle.dto.MaskList
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class MaskListDeserializer: JsonDeserializer<MaskList> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MaskList {
        val jsonArray = json?.asJsonArray
            ?: throw NullPointerException("Response Json String is null")

        val maskList = MaskList()
        for (i in 0 until jsonArray.size()) {
            val base64String = jsonArray.get(i).asString
            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
            maskList.add(byteArray)
        }
        return maskList
    }
}