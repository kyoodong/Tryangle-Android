package com.gomson.tryangle.album

import android.net.Uri

class Bucket(public val name: String, image: DeviceAlbum) {
    public val images = mutableListOf<DeviceAlbum>()

    init {
        images.add(image)
    }
}