package com.gomson.tryangle.photo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
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
import com.gomson.tryangle.MainActivity
import com.gomson.tryangle.R
import com.gomson.tryangle.databinding.ActivityPhotoBinding
import com.gomson.tryangle.visibleIf
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropFragment
import com.yalantis.ucrop.UCropFragmentCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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

class PhotoActivity : AppCompatActivity(), UCropFragmentCallback {

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var contentUri: Uri? = null
    private lateinit var binding: ActivityPhotoBinding
    private lateinit var fragment: UCropFragment
    private var currentMode = PhotoEditMode.MAIN
    private val REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 100
    private val TAG = "PhotoActivity"
    private lateinit var outputDirectory:File

    private lateinit var cropAdapter: CropAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo)
        contentUri = intent.data
        outputDirectory = getOutputDirectory()
//        crop
        initCrop()
        cropAdapter = CropAdapter(this, CropRatio.values())
        binding.cropRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.cropRecyclerView.adapter = cropAdapter

//         main
        binding.back.setOnClickListener {
            finishMode()
        }
        binding.cropView.setOnClickListener{
            changeCropMode()
        }
        binding.filterView.setOnClickListener{

        }
        binding.deleteView.setOnClickListener{
            contentResolver.delete(contentUri!!, null, null)
            finishMode()
        }
        binding.finish.setOnClickListener{
            fragment.cropAndSaveImage()
//            saveCroppedImage()
        }
    }

    /** 현재 모드 종료  */
    fun finishMode(){
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

    private fun initCrop() {
        var uCrop = contentUri?.let {
            UCrop.of(
                it,
                Uri.fromFile(File(cacheDir, "tryangle.jpg"))
            )
        }

        uCrop = advancedConfig(uCrop!!)

        fragment = uCrop.getFragment(uCrop.getIntent(this).getExtras())
        supportFragmentManager.beginTransaction()
            .add(R.id.imageLayout, fragment, UCropFragment.TAG)
            .commitAllowingStateLoss()
    }

    /** 메인모드로 전환 */
    private fun changeMainMode() {
        currentMode = PhotoEditMode.MAIN
        updateView(PhotoEditMode.MAIN.layoutId)
        binding.finish.visibility = View.GONE
    }

    /** 사진 크기 편집 모드로 전환 */
    private fun changeCropMode() {
        binding.finish.visibility = View.VISIBLE
        currentMode = PhotoEditMode.CROP
        updateView(PhotoEditMode.CROP.layoutId)
    }

    /** ucrop 라이브러리 고급 설정 */
    private fun advancedConfig(uCrop: UCrop): UCrop {
        val options = UCrop.Options()
        options.setCompressionFormat(Bitmap.CompressFormat.PNG)
        options.setCompressionQuality(80)
        options.setHideBottomControls(true)
        options.setFreeStyleCropEnabled(true)

        /*
        If you want to configure how gestures work for all UCropActivity tabs

        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        * */

        /*
        This sets max size for bitmap that will be decoded from source Uri.
        More size - more memory allocation, default implementation uses screen diagonal.

        options.setMaxBitmapSize(640);
        * */


        /*

        Tune everything (ﾉ◕ヮ◕)ﾉ*:･ﾟ✧

        options.setMaxScaleMultiplier(5);
        options.setImageToCropBoundsAnimDuration(666);
        options.setDimmedLayerColor(Color.CYAN);
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setCropGridStrokeWidth(20);
        options.setCropGridColor(Color.GREEN);
        options.setCropGridColumnCount(2);
        options.setCropGridRowCount(1);
        options.setToolbarCropDrawable(R.drawable.your_crop_icon);
        options.setToolbarCancelDrawable(R.drawable.your_cancel_icon);

        // Color palette
        options.setToolbarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setRootViewBackgroundColor(ContextCompat.getColor(this, R.color.your_color_res));
        options.setActiveControlsWidgetColor(ContextCompat.getColor(this, R.color.your_color_res));

        // Aspect ratio options
        options.setAspectRatioOptions(1,
            new AspectRatio("WOW", 1, 2),
            new AspectRatio("MUCH", 3, 4),
            new AspectRatio("RATIO", CropImageView.DEFAULT_ASPECT_RATIO, CropImageView.DEFAULT_ASPECT_RATIO),
            new AspectRatio("SO", 16, 9),
            new AspectRatio("ASPECT", 1, 1));

       */return uCrop.withOptions(options)
    }


    private fun saveCroppedImage() {
        val imageUri = contentUri
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                 arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE) ,
                REQUEST_STORAGE_WRITE_ACCESS_PERMISSION
            )
            return
        }

        if (imageUri != null && imageUri.scheme == "file") {
            try {
                copyFileToDownloads(imageUri)
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                Log.e(
                    TAG,
                    imageUri.toString(),
                    e
                )
                Toast.makeText(
                    this,
                    "파일을 저장하지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "쓰기 권한이 필요합니다",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.progressBar.visibility = View.GONE
    }


    private fun copyFileToDownloads(croppedFileUri: Uri) {
        val saveFile = File(outputDirectory, SimpleDateFormat(FILENAME_FORMAT)
            .format(System.currentTimeMillis()) + ".jpg"
        )

        val inStream =
            FileInputStream(File(croppedFileUri.path))
        val outStream = FileOutputStream(saveFile)
        val inChannel = inStream.channel
        val outChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()

        MediaScannerConnection.scanFile(
            baseContext, arrayOf(saveFile.toString()), arrayOf(saveFile.name), null
        )

        Toast.makeText(this,"사진이 저장되었습니다", Toast.LENGTH_SHORT).show()
        finishMode()
    }


    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    /* 화면 전환 */
    private fun updateView(@LayoutRes id: Int) {
        var targetConstSet = ConstraintSet()
        targetConstSet.clone(this, id)
        targetConstSet.applyTo(binding.root)

        val trans = ChangeBounds()
        trans.interpolator = AccelerateInterpolator()
        TransitionManager.beginDelayedTransition(binding.root, trans)
    }

    override fun onCropFinish(result: UCropFragment.UCropResult?) {
        contentUri = result?.mResultData?.let { UCrop.getOutput(it) }
        saveCroppedImage()
//        saveCroppedImage(result?.mResultData?.let { UCrop.getOutput(it) })
        changeMainMode()
        initCrop()
    }

    override fun loadingProgress(showLoader: Boolean) {
        binding.progressBar.visibility = showLoader.visibleIf()
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
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // todo

                }else {
                    Toast.makeText(this, "쓰기 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

    }
}