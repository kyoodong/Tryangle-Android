package com.gomson.tryangle

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat

class PhotoDownloadManager(val context: Context, val callback: PhotoSaveCallback) {

    val outputDirectory = getDirectory(context)
    companion object{
        private val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        /**
         * 사진을 저장할 위치를 리턴하는 함수
         */
        fun getDirectory(context: Context): File {
            val mediaDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), context.resources.getString(R.string.app_name)
            )
            if (!mediaDir.exists()) {
                mediaDir.mkdir()
            }

            return if (mediaDir != null && mediaDir.exists())
                mediaDir else context.filesDir
        }

        /**
         * 파일 이름 생성
         */
        fun getFileName(): String {
            return SimpleDateFormat(FILENAME_FORMAT)
                .format(System.currentTimeMillis()) + ".jpg"
        }

    }


    fun saveFileToAlbum(uri: Uri) {
        var savedUri:Uri? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            savedUri = saveFilebeforeQ(uri)
        } else {
            savedUri = saveFileAfterQ(uri)
        }
        callback.onSaveFinish(savedUri)
    }

    /**
     * 버전 Q 이상 사진 다운로드
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileAfterQ(uri: Uri): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, getFileName())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val item = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val inStream = FileInputStream(File(uri.path))
        val outStream = resolver.openOutputStream(item!!)
        outStream!!.write(inStream.readBytes())
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        item?.let { resolver.update(it, values, null, null) }
        return item
    }

    /**
     * 버전 Q 미만 사진 다운로드
     */
    private fun saveFilebeforeQ(uri: Uri): Uri? {
        val saveFile = File(
            outputDirectory, getFileName()
        )

        val inStream =
            FileInputStream(File(uri.path))
        val outStream = FileOutputStream(saveFile)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()

        MediaScannerConnection.scanFile(
            context,
            arrayOf(saveFile.path),
            arrayOf("image/jpg"),
            null
        )
        return Uri.fromFile(saveFile)
    }

    interface PhotoSaveCallback {
        fun onSaveFinish(savedUri: Uri?)
    }
}