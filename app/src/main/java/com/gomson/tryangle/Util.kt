package com.gomson.tryangle

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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