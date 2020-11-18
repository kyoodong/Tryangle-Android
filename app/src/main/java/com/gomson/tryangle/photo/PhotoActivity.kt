package com.gomson.tryangle.photo

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gomson.tryangle.OnItemClickListener
import com.gomson.tryangle.PhotoDownloadManager
import com.gomson.tryangle.R
import com.gomson.tryangle.databinding.ActivityPhotoBinding
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.view.CropImageView
import com.yalantis.ucrop.view.GestureCropImageView
import com.yalantis.ucrop.view.OverlayView
import com.yalantis.ucrop.view.TransformImageView.TransformImageListener
import java.io.File

enum class CropRatio constructor(
    val text: String,
    @DrawableRes val resources: Int,
    var x: Int,
    var y: Int
) {

    RATIO_ORIGINAL("원본", R.drawable.crop_original, 0, 0),
    RATIO_FREE("자유", R.drawable.crop_free, 0, 0),
    RATIO_1_1("1:1", R.drawable.crop_1_1, 1, 1),
    RATIO_3_4("3:4", R.drawable.crop_3_4, 3, 4),
    RATIO_4_3("4:3", R.drawable.crop_4_3, 4, 3),
    RATIO_9_16("9:16", R.drawable.crop_16_9, 9, 16),
}

enum class PhotoEditMode constructor(@LayoutRes val layoutId: Int) {
    MAIN(R.layout.activity_photo),
    CROP(R.layout.activity_photo_crop),
//    FILTER(R.layout.activity_photo_filter),
}

private const val DELETE_PERMISSION_REQUEST = 30

class PhotoActivity : AppCompatActivity(), PhotoDownloadManager.PhotoSaveCallback {

    companion object {
        val DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG
        const val DEFAULT_COMPRESS_QUALITY = 90
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42
    }

    private var contentUri: Uri? = null
    private lateinit var binding: ActivityPhotoBinding

    private var currentMode = PhotoEditMode.MAIN
    private val REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 100
    private val TAG = "PhotoActivity"
    private lateinit var overlayView: OverlayView
    private lateinit var outputDirectory: File
    private lateinit var gestureCropImageView: GestureCropImageView
    private lateinit var downloadManager: PhotoDownloadManager

    private lateinit var cropAdapter: CropAdapter
    var ratio = -1F
    var corner = -1
    val reflect = OverlayView::class.java
        .getDeclaredField("mCurrentTouchCornerIndex")
        .apply { isAccessible = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo)
        contentUri = intent.data
        downloadManager = PhotoDownloadManager(this, this)


        initCropView()
        initMainView()

    }

    private fun initCropView() {
        cropAdapter = CropAdapter(this, CropRatio.values())
        binding.cropRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.cropRecyclerView.adapter = cropAdapter
        gestureCropImageView = binding.ucrop.cropImageView
        cropAdapter.setOnItemClickListener(
            object : OnItemClickListener<CropRatio> {
                override fun onItemClick(view: View, position: Int, item: CropRatio) {
                    if(item != CropRatio.RATIO_FREE){
                        gestureCropImageView.targetAspectRatio = getRatioAspect(item)
                        gestureCropImageView.setImageToWrapCropBounds()
                    }
                    cropAdapter.currentRatio = item
                }
            })

        overlayView = binding.ucrop.overlayView
        overlayView.visibility = View.GONE
        overlayView.freestyleCropMode = OverlayView.FREESTYLE_CROP_MODE_ENABLE

        binding.ucrop.cropImageView.setCropBoundsChangeListener {
            overlayView.setTargetAspectRatio(it)
            ratio = it
        }

        gestureCropImageView.isScaleEnabled = true
        gestureCropImageView.isRotateEnabled = false

        overlayView.setOnTouchListener { _, event ->
            val action = event.action and MotionEvent.ACTION_MASK

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN ||
                cropAdapter.currentRatio == CropRatio.RATIO_FREE) {
                overlayView.onTouchEvent(event).also { corner = reflect.getInt(overlayView) }
            } else {
                if (action == MotionEvent.ACTION_MOVE && corner in 0..3) {
                    newPositionForUpdateCropViewRect(
                        corner,
                        ratio,
                        event.x,
                        event.y,
                        overlayView.cropViewRect
                    )
                        .run { event.setLocation(first, second) }
                }
                overlayView.onTouchEvent(event)
            }
        }

        gestureCropImageView.setTransformImageListener(mImageListener)
        setImageData(contentUri)
        setupRotateWidget()
    }

    private val mImageListener: TransformImageListener = object : TransformImageListener {
        override fun onRotate(currentAngle: Float) {
            binding.degreeView.text = currentAngle.toString()
        }

        override fun onScale(currentScale: Float) {
            binding.degreeView.text = currentScale.toString()
        }

        override fun onLoadComplete() {
            binding.ucrop.animate().alpha(1f).setDuration(300).interpolator = AccelerateInterpolator()
        }

        override fun onLoadFailure(e: java.lang.Exception) {

        }
    }

    private fun setImageData(uri: Uri?) {
        uri?.let {
            val outputUri = Uri.fromFile(File(cacheDir, "tryangle.jpg"))
            gestureCropImageView.setImageUri(it, outputUri)
        }
    }

    private fun setupRotateWidget() {
        binding.rotateWheelView.setScrollingListener(object :
            ProgressWheelView.ScrollingListener {
            override fun onScroll(delta: Float, totalDistance: Float) {
                gestureCropImageView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT)
            }

            override fun onScrollEnd() {
                gestureCropImageView.setImageToWrapCropBounds()
            }

            override fun onScrollStart() {
                gestureCropImageView.cancelAllAnimations()
            }
        })


        binding.rotateImageView.setOnClickListener { rotateByAngle(90) }
    }

    private fun rotateByAngle(angle: Int) {
        gestureCropImageView.postRotate(angle.toFloat())
        gestureCropImageView.setImageToWrapCropBounds()
    }

    private fun initMainView() {
        binding.back.setOnClickListener {
            finishMode()
        }
        binding.cropView.setOnClickListener {
            changeCropMode()
        }
        binding.filterView.setOnClickListener {

        }
        binding.deleteView.setOnClickListener {
            try {
                val uri = contentUri
                if (uri != null) {
                    contentResolver.delete(uri, null, null)
                    finishMode()
                }
            } catch (e: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val intentSender = e.userAction.actionIntent.intentSender
                    intentSender?.let {
                        startIntentSenderForResult(
                            intentSender,
                            DELETE_PERMISSION_REQUEST,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    }
                }
            }
        }
        binding.finish.setOnClickListener {
            cropAndSaveImage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == DELETE_PERMISSION_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = contentUri
                if (uri != null) {
                    contentResolver.delete(uri, null, null)
                    finishMode()
                }
            }
        }
    }

    private fun cropAndSaveImage() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_WRITE_ACCESS_PERMISSION
            )
            return
        }
        binding.progressBar.visibility = View.VISIBLE
        gestureCropImageView.cropAndSaveImage(
            DEFAULT_COMPRESS_FORMAT,
            DEFAULT_COMPRESS_QUALITY,
            object : BitmapCropCallback {
                override fun onBitmapCropped(
                    resultUri: Uri,
                    offsetX: Int,
                    offsetY: Int,
                    imageWidth: Int,
                    imageHeight: Int
                ) {
                    downloadManager.saveFileToAlbum(resultUri)
                }

                override fun onCropFailure(t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    /** 현재 모드 종료  */
    fun finishMode() {
        when (currentMode) {
            PhotoEditMode.MAIN -> {
                finish()
            }
            PhotoEditMode.CROP -> {
                changeMainMode()
            }
        }
    }

    override fun onBackPressed() {
        finishMode()
    }


    /** 메인모드로 전환 */
    private fun changeMainMode() {
        currentMode = PhotoEditMode.MAIN
        binding.finish.visibility = View.GONE
        updateView(PhotoEditMode.MAIN.layoutId)
        overlayView.visibility = View.GONE
    }

    /** 사진 크기 편집 모드로 전환 */
    private fun changeCropMode() {
        currentMode = PhotoEditMode.CROP
        binding.finish.visibility = View.VISIBLE
        updateView(PhotoEditMode.CROP.layoutId)
        overlayView.visibility = View.VISIBLE
    }

    /** ucrop 라이브러리 고급 설정 */
    private fun advancedConfig(uCrop: UCrop): UCrop {
        val options = UCrop.Options()
        options.setCompressionFormat(Bitmap.CompressFormat.PNG)
        options.setCompressionQuality(90)
        options.setHideBottomControls(true)
        options.setFreeStyleCropEnabled(true)
       return uCrop.withOptions(options)
    }


    private fun getRatioAspect(cropRatio: CropRatio): Float {
        if (cropRatio == CropRatio.RATIO_ORIGINAL) {
            return CropImageView.SOURCE_IMAGE_ASPECT_RATIO
        }
        return cropRatio.x.toFloat() / cropRatio.y.toFloat()
    }


    /** 화면 전환 */
    private fun updateView(@LayoutRes id: Int) {
        var targetConstSet = ConstraintSet()
        targetConstSet.clone(this, id)
        targetConstSet.applyTo(binding.root)

        val trans = ChangeBounds()
        trans.interpolator = AccelerateInterpolator()
        TransitionManager.beginDelayedTransition(binding.root, trans)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_STORAGE_WRITE_ACCESS_PERMISSION -> {
                if (!grantResults.isEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    cropAndSaveImage()

                } else {
                    Toast.makeText(this, "쓰기 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

    }

    /** 비율 고정하면서 수정되는 overlayview 좌표  */
    fun newPositionForUpdateCropViewRect(
        corner: Int,
        ratio: Float,
        x: Float,
        y: Float,
        rect: RectF
    ): Pair<Float, Float> {
        if (corner !in 0..3) return x to y

        var k = 1f / ratio

        if (corner == 1 || corner == 3) k = -k

        val w = if (corner == 0 || corner == 3) rect.left else rect.right
        val h = if (corner == 0 || corner == 1) rect.top else rect.bottom

        val kk1 = k * k + 1
        val tx = ((w * k + y - h) * k + x) / kk1
        val ty = ((y * k + x - w) * k + h) / kk1

        return tx to ty
    }

    /** 사진 저장하고 난 후 */
    override fun onSaveFinish(savedUri: Uri?) {
        contentUri = savedUri
        setImageData(savedUri)
        binding.progressBar.visibility = View.GONE
        changeMainMode()
    }
}