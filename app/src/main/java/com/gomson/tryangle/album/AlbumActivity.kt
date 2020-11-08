package com.gomson.tryangle.album

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gomson.tryangle.OnItemClickListener
import com.gomson.tryangle.R
import kotlinx.android.synthetic.main.activity_album.*
import java.util.*
import kotlin.collections.HashMap


class AlbumActivity : AppCompatActivity() {

    val uriList: MutableList<DeviceAlbum> = arrayListOf()
    val bucketHashMap = HashMap<String, Bucket>()
    lateinit var pathAdapter: BucketAdapter
    lateinit var albumAdapter: AlbumAdapter
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private var selectedItem = 0
    lateinit var bucketPopupWindow: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)
        back.setOnClickListener { finish() }

        if (permissionsGranted()) {
            getBucketImages()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
            return
        }
        pathAdapter = BucketAdapter(this, ArrayList(bucketHashMap.values))
        albumAdapter = AlbumAdapter(this)
        albumAdapter.addAll(uriList)

        albumRecyclerView.adapter = albumAdapter
        val lm = GridLayoutManager(this, 3)
        albumRecyclerView.layoutManager = lm
        albumRecyclerView.setHasFixedSize(true)

        bucketPopupWindow = getBucketPopup()
        bucketView.setOnClickListener {
            bucketPopupWindow.showAsDropDown(actionBarLayout, 0, 0)
        }
    }

    /* 디바이스의 이미지와 폴더리스트를 가져옴 */
    private fun getBucketImages() {
        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val columnIndexID: Int
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN
        )
        var selectionClause = null

        /* 최신순 */
        val sortOrder = MediaStore.Images.ImageColumns._ID + " DESC "
        cursor = contentResolver.query(uriExternal, projection, selectionClause, null, sortOrder)

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dateTakenColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            columnIndexID = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketColumn: Int = cursor.getColumnIndex(
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            while (cursor.moveToNext()) {
                val imageId = cursor.getLong(columnIndexID)
                val id = cursor.getLong(idColumn)
                val dateTaken = Date(cursor.getLong(dateTakenColumn))
                val displayName = cursor.getString(displayNameColumn)
                val uriImage = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                val bucket = cursor.getString(bucketColumn)
                val image = DeviceAlbum(id, displayName, dateTaken, uriImage)
                if (bucketHashMap.containsKey(bucket)) {
                    bucketHashMap.get(bucket)?.images?.add(image)
                } else {
                    bucketHashMap.put(bucket, Bucket(bucket, image))
                }
                uriList.add(image)
            }
            cursor.close()
        }
    }

    /* 이미지 저장된 경로 선택하는 팝업 */
    private fun getBucketPopup(): PopupWindow {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.view_recyclerview, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
        )
        val popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val adapter = BucketAdapter(this, ArrayList(bucketHashMap.values))
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

        adapter.setOnItemClickListner(
            object : OnItemClickListener<Bucket> {
                override fun onItemClick(view: View, position: Int, item: Bucket) {
                    uriList.clear()
                    albumAdapter.clear()
                    bucketHashMap[item.name]?.images?.let { albumAdapter.addAll(it) }
                    albumAdapter.notifyDataSetChanged()
                    selectedItem = position
                    popupWindow.dismiss()
                    bucketView.text = item.name
                }
            }
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        return popupWindow
    }


    private fun permissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (permissionsGranted()) {
                getBucketImages()
                albumAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "읽기 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}