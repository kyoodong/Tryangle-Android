package com.gomson.tryangle

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import java.io.File
import java.io.Serializable
import java.security.AccessController.getContext

fun getActivity(context: Context): Activity? {
    var c = context
    while (c is ContextWrapper) {
        if (c is Activity) {
            return c
        }
        c = (context as ContextWrapper).baseContext
    }
    return null
}

fun Boolean.visibleIf(): Int = if (this) View.VISIBLE else View.GONE

interface OnItemClickListener<T> {
    fun onItemClick(view: View, position: Int, item: T)
}

