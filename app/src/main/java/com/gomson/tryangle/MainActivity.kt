package com.gomson.tryangle

import android.Manifest.permission
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.gomson.tryangle.album.AlbumActivity
import com.gomson.tryangle.databinding.ActivityMainBinding
import com.gomson.tryangle.domain.*
import com.gomson.tryangle.domain.component.Component
import com.gomson.tryangle.domain.component.ComponentList
import com.gomson.tryangle.domain.component.ObjectComponent
import com.gomson.tryangle.domain.component.PersonComponent
import com.gomson.tryangle.domain.guide.Guide
import com.gomson.tryangle.domain.guide.LayerLayoutGuideManager
import com.gomson.tryangle.dto.MaskList
import com.gomson.tryangle.dto.ObjectComponentListDTO
import com.gomson.tryangle.guider.GuideImageObjectGuider
import com.gomson.tryangle.guider.ObjectGuider
import com.gomson.tryangle.guider.PoseGuider
import com.gomson.tryangle.guider.convertTo
import com.gomson.tryangle.network.BaseService
import com.gomson.tryangle.network.ImageService
import com.gomson.tryangle.network.ModelService
import com.gomson.tryangle.network.NetworkManager
import com.gomson.tryangle.setting.PreferenceActivity
import com.gomson.tryangle.setting.PreferenceFragment
import com.gomson.tryangle.view.guide_image_view.GuideImageAdapter
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_bucket.*
import kotlinx.android.synthetic.main.popup_more.view.*
import kotlinx.android.synthetic.main.popup_ratio.view.*
import kotlinx.android.synthetic.main.view_guide_image_category_tab_layout.view.*
import okhttp3.ResponseBody
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.examples.posenet.lib.Device
import org.tensorflow.lite.examples.posenet.lib.Posenet
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

enum class RatioMode constructor(val width: Int, val height: Int) {
    RATIO_1_1(1, 1),
    RATIO_3_4(3, 4),
    RATIO_9_16(9, 16),
//    RATIO_FULL(0,0)
}

enum class TimerMode constructor(
    val milliseconds: Long,
    val btnImg: Int,
    val info: String,
    val text: String
) {
    TIMER_OFF(0, R.drawable.timer, "타이머 꺼짐", ""),
    TIMER_3S(3000, R.drawable.timer3s, "3초", "3"),
    TIMER_7S(7000, R.drawable.timer7s, "7초", "7"),
    TIMER_10S(10000, R.drawable.timer10s, "10초", "10"),
}

class MainActivity : AppCompatActivity(), ImageAnalyzer.OnAnalyzeListener,
    GuideImageAdapter.OnClickGuideImage, SplashFragment.OnCloseSplashListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(permission.CAMERA, permission.ACCESS_FINE_LOCATION, permission.READ_EXTERNAL_STORAGE)
    }

    // 카메라
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var baseLoaderCallback: BaseLoaderCallback
    private lateinit var handler: Handler
    private var isOpenCvLoaded = false

    private lateinit var camera: Camera
    private lateinit var converter: YuvToRgbConverter

    private val spotList = ArrayList<Spot>()
    private val componentList = ComponentList()
    private val guideComponentList = ArrayList<ObjectComponent>()
    private lateinit var imageAnalyzer: ImageAnalyzer

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var preview: Preview? = null


    val TAG = "MainActivity"

    private var COLOR_WHITE = 0
    private var COLOR_LIGHTGRAY = 0
    private var COLOR_LIGHTMINT = 0

    lateinit var popupMoreView: PopupWindow
    lateinit var popupRatioView: PopupWindow

    var currentRatio = RatioMode.RATIO_1_1
    var isFlash = false
    var isGrid = false
    private val recommendedImageUrlList = ArrayList<String>()
    var currentTimerModeIndex = 0
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageService: ImageService
    private val componentMatcher = ComponentMatcher()
    private var isHighDefinition = false
    private lateinit var sharedPreferences : SharedPreferences

    private var guidingComponentImageView: ImageView? = null
    private var guidingComponent: Component? = null
    private var targetComponent: Component? = null
    private var guidingGuide: Guide? = null
    private lateinit var layerLayoutGuideManager: LayerLayoutGuideManager

    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var splashFragment: SplashFragment
    private lateinit var posenet: Posenet

    private var phaseTargetGuide = false

    init {
        System.loadLibrary("opencv_java4")
        System.loadLibrary("native-lib")
        System.loadLibrary("pytorch_nativeapp")
    }

    val ratioPopupViewClickListener = View.OnClickListener { view ->
        var clickRatio = RatioMode.RATIO_3_4
        binding.previewLayout.layoutParams =
            (binding.previewLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                when (view.id) {
                    R.id.ratio3_4 -> {
                        clickRatio = RatioMode.RATIO_3_4
                        imageAnalyzer.setRatio(4f / 3f)
                    }
                    R.id.ratio1_1 -> {
                        clickRatio = RatioMode.RATIO_1_1
                        imageAnalyzer.setRatio(1f)
                    }
                    R.id.ratio9_16 -> {
                        clickRatio = RatioMode.RATIO_9_16
                        imageAnalyzer.setRatio(16f / 9f)
                    }
                }
            }
        if (clickRatio != currentRatio) {
            setAspectRatioView(clickRatio)
            currentRatio = clickRatio
            imageCapture = getImageCapture(clickRatio.height, clickRatio.width)
            bindCameraConfiguration()
        }
        popupRatioView.dismiss()
    }

    /* 비율에 따른 뷰 설정*/
    private fun setAspectRatioView(ratio: RatioMode) {
        binding.previewLayout.post(Runnable {
            (binding.previewLayout.layoutParams as ConstraintLayout.LayoutParams).apply {
                when (ratio) {
                    RatioMode.RATIO_1_1 -> {
                        height = binding.previewLayout.width
                        topToTop = binding.topLayout.id
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio1_1)
                    }
                    RatioMode.RATIO_3_4 -> {
                        topToTop = ConstraintSet.PARENT_ID
                        height = 0
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio3_4)
                    }
                    RatioMode.RATIO_9_16 -> {
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                        binding.ratioBtn.setBackgroundResource(R.drawable.ratio9_16)
                    }
                }
                binding.previewLayout.postInvalidate()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        converter = YuvToRgbConverter(this)
        imageService = ImageService(baseContext)
        posenet = Posenet(baseContext, "posenet_model.tflite", Device.GPU)

        if(savedInstanceState == null) { // initial transaction should be wrapped like this
            splashFragment = SplashFragment(this)
            supportFragmentManager.beginTransaction()
                .replace(R.id.splashLayout, splashFragment)
                .commitAllowingStateLoss()
        }

        binding.guideImageCategoryTabLayout.addTab(
            GuideTabItem("추천", recommendedImageUrlList)
        )

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                imageAnalyzer.latestLocation = locationResult.locations[locationResult.locations.size - 1]
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // 카메라 권한 체크
        if (allPermissionsGranted()) {
            startCamera()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        COLOR_WHITE = ContextCompat.getColor(this, R.color.colorWhite)
        COLOR_LIGHTGRAY = ContextCompat.getColor(this, R.color.colorLightgray)
        COLOR_LIGHTMINT = ContextCompat.getColor(this, R.color.colorLightMint)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        layoutInflater.inflate(R.layout.popup_more, null).let {
            popupMoreView = PopupWindow(
                it,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            it.flashLayout.setOnClickListener {
                if (camera.cameraInfo.hasFlashUnit()) {
                    isFlash = !isFlash
                    camera.cameraControl.enableTorch(isFlash)
                    popupMoreView.contentView.flash.isSelected = isFlash
                }
            }
            it.timerLayout.setOnClickListener {
                currentTimerModeIndex++
                currentTimerModeIndex %= TimerMode.values().size
                val currentTimer = TimerMode.values()[currentTimerModeIndex]
                it.timer.setImageResource(currentTimer.btnImg)
                it.timerTextView.text = currentTimer.info

                binding.timerTextView.text = currentTimer.text
            }
            it.gridLayout.setOnClickListener {
                isGrid = !isGrid
                binding.gridLinesView.visibility = isGrid.visibleIf()
                popupMoreView.contentView.grid.isSelected = isGrid
            }
            it.settingLayout.setOnClickListener {
                val nextIntent = Intent(this, PreferenceActivity::class.java)
                startActivity(nextIntent)
            }
        }
        handler = Handler(mainLooper)
        baseLoaderCallback = object : BaseLoaderCallback(baseContext) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    SUCCESS -> Log.i(TAG, "OpenCV loaded successfully")
                    else -> super.onManagerConnected(status)
                }
            }
        }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        layoutInflater.inflate(R.layout.popup_ratio, null).let {
            it.ratio1_1.setOnClickListener(ratioPopupViewClickListener)
            it.ratio3_4.setOnClickListener(ratioPopupViewClickListener)
            it.ratio9_16.setOnClickListener(ratioPopupViewClickListener)
            popupRatioView = PopupWindow(
                it,
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
        }

        binding.captureButton.setOnClickListener {
            TimerMode.values()[currentTimerModeIndex].let {
                if ( it == TimerMode.TIMER_OFF ) {
                    takePhoto()
                }
                else{
                    countDownTimer(it)
                }
            }
        }

        binding.ratioBtn.setOnClickListener {
            popupRatioView.contentView.ratio9_16.setColorFilter(
                if (currentRatio == RatioMode.RATIO_9_16) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            popupRatioView.contentView.ratio3_4.setColorFilter(
                if (currentRatio == RatioMode.RATIO_3_4) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
            popupRatioView.contentView.ratio1_1.setColorFilter(
                if (currentRatio == RatioMode.RATIO_1_1) COLOR_LIGHTMINT else COLOR_WHITE,
                android.graphics.PorterDuff.Mode.MULTIPLY
            )

            popupRatioView.animationStyle = -1
            popupRatioView.showAsDropDown(binding.topLayout, 0, 0)
        }
        binding.moreBtn.setOnClickListener {
            popupMoreView.showAsDropDown(binding.topLayout, 0, 0)
        }
        binding.reverseBtn.setOnClickListener {
            cameraSelector = if (CameraSelector.DEFAULT_BACK_CAMERA == cameraSelector)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            bindCameraConfiguration()
        }

        binding.albumBtn.setOnClickListener {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
        }

        isHighDefinition = sharedPreferences.getBoolean(PreferenceFragment.KEY_HIGH_DEFINITION, false)

        guideImageCategoryTabLayout.setOnClickGuideImageListener(this)
        layerLayoutGuideManager = LayerLayoutGuideManager(binding.layerLayout)

        setRecentImageView(getRecentImage())

        setAspectRatioView(currentRatio)
    }

    override fun onResume() {
        super.onResume()

        if (!OpenCVLoader.initDebug()) {
            Log.d(
                TAG,
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            );
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION,
                this,
                baseLoaderCallback
            );
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            isOpenCvLoaded = true;
        }

        sharedPreferences.getBoolean(PreferenceFragment.KEY_HIGH_DEFINITION, false).let{
            if(isHighDefinition != it){
                isHighDefinition = it
                imageCapture = getImageCapture(currentRatio.height, currentRatio.width)
                bindCameraConfiguration()
            }
        }
    }

    private fun setRecentImageView(uri : Uri?){
        uri.let{
            Glide.with(this)
                .load(it)
                .dontAnimate()
                .into(binding.albumBtn)
        }
    }

    /**
     * 필요한 권한을 로드하는 함수
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 사진을 저장할 위치를 리턴하는 함수
     */
    private fun getOutputDirectory(): File {

        val mediaDir = PhotoDownloadManager.getDirectory(this)
        if (!mediaDir.exists()) {
            mediaDir.mkdir()
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun getImageCapture(heightRatio: Int, widthRatio: Int): ImageCapture {
        val imageCapture = ImageCapture.Builder()
            .apply {

                setCaptureMode(if (isHighDefinition)  ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                setTargetAspectRatioCustom(Rational(widthRatio, heightRatio))
            }.build()
        preview = Preview.Builder()
            .setTargetAspectRatioCustom(Rational(widthRatio, heightRatio))
            .build()
        return imageCapture
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            cameraProvider = cameraProviderFuture.get()
            bindCameraConfiguration()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * 카메라 서비스 설정값을 지정하는 함수
     */
    private fun bindCameraConfiguration() {
        // 기본 카메라 비율을 16:9로 설정
        if (imageCapture == null)
            imageCapture = getImageCapture(currentRatio.height, currentRatio.width)

        val preview = preview
            ?: return

        // 이미지 분석 모듈
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalyzer = ImageAnalyzer(baseContext, this)
        imageAnalysis!!.setAnalyzer(cameraExecutor, imageAnalyzer)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalysis
            )
            preview.setSurfaceProvider(binding.previewView.createSurfaceProvider())
            splashFragment.onCameraSetup()
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        imageAnalyzer.setGuide(null, null, null, false)
        binding.layerLayout.removeAllViews()
        binding.guidePercentTextView.text = ""
        binding.guideTextView.text = ""

        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDirectory,
            PhotoDownloadManager.getFileName()
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.captureEffectView.visibility = View.VISIBLE

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    setRecentImageView(savedUri)
                    binding.captureEffectView.visibility = View.GONE

                    MediaScannerConnection.scanFile(
                        baseContext, arrayOf(photoFile.toString()), arrayOf(photoFile.name), null
                    )

                    val photoDownloadManager = PhotoDownloadManager(baseContext, object: PhotoDownloadManager.PhotoSaveCallback {
                        override fun onSaveFinish(savedUri: Uri?) {
                            Log.d(TAG, "save")
                        }
                    })

                    photoDownloadManager.saveFileToAlbum(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            } else {
                Toast.makeText(this, "권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onUpdateGuidingComponentPosition(width: Int, height: Int, leftTopPoint: Point) {
        val guidingComponentImageView = this.guidingComponentImageView
            ?: return

        val layoutWidth = binding.layerLayout.width
        val layoutHeight = binding.layerLayout.height

        val oldX = guidingComponentImageView.x
        val oldY = guidingComponentImageView.y

        val newWidth = width * layoutWidth / imageAnalyzer.width
        val newHeight = height * layoutHeight / imageAnalyzer.height
        val newX = leftTopPoint.x.toFloat() * layoutWidth / imageAnalyzer.width
        val newY = leftTopPoint.y.toFloat() * layoutHeight / imageAnalyzer.height

        for (i in 1 .. 20) {
            handler.postDelayed(Runnable {
                val newWeight = i / 20f
                val oldWeight = 1 - newWeight

                val resultX = oldX * oldWeight + newX * newWeight
                val resultY = oldY * oldWeight + newY * newWeight

                guidingComponentImageView.x = resultX
                guidingComponentImageView.y = resultY
                guidingComponentImageView.requestLayout()
            }, i * 5L)
        }

        runOnUiThread {
            guidingComponentImageView.layoutParams.width = newWidth
            guidingComponentImageView.layoutParams.height = newHeight
        }
    }

    override fun onUpdateComponents(components: ArrayList<Component>) {
        Log.i(TAG, "컴포넌트 업데이트")
        this.componentList.clear()
        this.componentList.addAll(components)
        this.componentList.sortByDescending {
            it.priority
        }
    }

    override fun onUpdateRecommendedImage(imageList: ArrayList<String>) {
        Log.i(TAG, "추천 이미지 ${imageList.size} 개 도착!")
        this.recommendedImageUrlList.clear()
        this.recommendedImageUrlList.addAll(imageList)

        runOnUiThread {
            binding.guidePercentTextView.text = ""
            this.binding.guideTextView.text = getString(R.string.select_guide_image)
            val tab = this.binding.guideImageCategoryTabLayout.tabLayout.getTabAt(0)
            this.binding.guideImageCategoryTabLayout.tabLayout.selectTab(tab)
            binding.guideImageCategoryTabLayout.addImageUrlList(imageList)
        }
    }

    private fun makeStandardGuide(component: ObjectComponent) {
        phaseTargetGuide = false
        component.guideCompleted = true
        component.guideList.clear()

        if (component is PersonComponent) {
            val poseGuider = PoseGuider(
                component.mask[0].size,
                component.mask.size
            )

            poseGuider.initGuideList(component)
        }
        val objectGuider = ObjectGuider(
            component.mask[0].size,
            component.mask.size
        )

        objectGuider.initGuideList(component)
    }

    override fun onMatchGuide() {
        Log.i(TAG, "가이드에 맞음!")
        val guidingComponent = guidingComponent
            ?: return

        val targetComponent = targetComponent
            ?: return

        if (guidingComponent is ObjectComponent) {
            // 가이드 사진 컴포넌트에 대한 가이드
            if (phaseTargetGuide) {
                targetComponent.guideList.remove(guidingGuide)
                targetComponent.guideCompleted = targetComponent.guideList.isEmpty()

                guidingGuide = if (!targetComponent.guideCompleted) {
                    targetComponent.guideList[0]
                } else {
                    null
                }
            }

            // 카메라 사진 컴포넌트에 대한 가이드
            else {
                guidingComponent.guideList.remove(guidingGuide)
                guidingComponent.guideCompleted = guidingComponent.guideList.isEmpty()
                guidingGuide = if (!guidingComponent.guideCompleted) {
                    guidingComponent.guideList[0]
                } else {
                    null
                }
            }

            runOnUiThread {
                displayGuide(guidingGuide)
                imageAnalyzer.setGuide(this.guidingComponent, targetComponent, guidingGuide, true)
            }
        }
    }

    fun countDownTimer(timerMode: TimerMode) {
        binding.timerTextView.visibility = View.VISIBLE
        object : CountDownTimer(timerMode.milliseconds + 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.timerTextView.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                binding.timerTextView.text = timerMode.text
                binding.guideTextView.text = ""
                binding.guidePercentTextView.text = ""
                binding.timerTextView.visibility = View.GONE
                takePhoto()
            }
        }.start()
    }

    /**
     * 가이드 이미지 클릭 이벤트
     */
    override fun onClick(url: String) {
        Log.d(TAG, "가이드 이미지 클릭 ${url}")

        phaseTargetGuide = true
        componentList.resetCompleteGuide()
        binding.layerLayout.removeAllViews()
        var startTime = System.currentTimeMillis()
        var endTime = System.currentTimeMillis()
        imageAnalyzer.setGuide(null, null, null, true)

        Glide.with(baseContext).asBitmap().load("${NetworkManager.URL}/${url}").listener(object :
            RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                Log.i(TAG, "비트맵 로딩 실패 ${NetworkManager.URL}/${url}")
                return false
            }

            override fun onResourceReady(
                resource: Bitmap?,
                model: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                endTime = System.currentTimeMillis()
                Log.d(TAG, "비트맵 로딩 타임 ${endTime - startTime}")
                startTime = System.currentTimeMillis()
                imageService.getObjectComponentByUrl(
                    url,
                    object : Callback<ObjectComponentListDTO> {
                        override fun onResponse(
                            call: Call<ObjectComponentListDTO>,
                            response: Response<ObjectComponentListDTO>
                        ) {
                            endTime = System.currentTimeMillis()
                            Log.d(TAG, "컴포넌트 로딩 타임 ${endTime - startTime}")
                            startTime = System.currentTimeMillis()
                            if (response.isSuccessful) {
                                Log.i(TAG, "가이드 이미지 컴포넌트 로딩 성공")
                                val objectComponentListDTO = response.body()
                                    ?: return

                                val base64String = objectComponentListDTO.maskStr
                                var base64StringList = base64String.split("==")
                                if (base64StringList.size <= 1) {
                                    Log.d(TAG, "마스크 이미지 없음")
                                    Toast.makeText(baseContext, "마스크 이미지 없음", Toast.LENGTH_SHORT)
                                        .show()
                                    return
                                }

                                base64StringList = base64StringList.subList(
                                    0,
                                    base64StringList.size - 1
                                )

                                val maskList = MaskList()
                                for (str in base64StringList) {
                                    val base64 = str.plus("==")
                                    maskList.add(Base64.decode(base64, Base64.DEFAULT))
                                }
                                objectComponentListDTO.deployMask(maskList)
                                val objectComponentList = objectComponentListDTO.objectComponentList

                                if (objectComponentList.isEmpty())
                                    return

                                for (objectComponent in objectComponentList) {
                                    objectComponent.refreshLayer(resource, Guide.GREEN, Guide.TRANS_GREEN)
                                }

                                guideComponentList.clear()
                                guideComponentList.addAll(objectComponentList)

                                // 카메라 오브젝트
                                val cameraObjectComponentList =
                                    componentList.getNotGuidedObjectComponentList()

                                endTime = System.currentTimeMillis()
                                Log.d(TAG, "컴포넌트 준비 타임 ${endTime - startTime}")
                                startTime = System.currentTimeMillis()

                                showCameraObject(cameraObjectComponentList)
                            } else {
                                Log.e(TAG, "가이드 이미지 컴포넌트 로딩 실패 ${response.code()}")
                                val imageView = ImageView(baseContext)
                                imageView.setImageBitmap(resource)
                                imageView.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                imageView.alpha = 0.35f
                                binding.layerLayout.addView(imageView)
                            }
                        }

                        override fun onFailure(
                            call: Call<ObjectComponentListDTO>,
                            t: Throwable
                        ) {
                            t.printStackTrace()
                            Log.e(TAG, "가이드 이미지 컴포넌트 로딩 실패")
                        }
                    })
                return false
            }
        }).submit()
    }

    private fun showCameraObject(cameraObjectComponentList: ArrayList<ObjectComponent>) {
        // 카메라 이미지에 여러 객체가 있을 때 가이드를 받을 객체를 선택하도록 함
        if (cameraObjectComponentList.size > 1) {
            Log.i(TAG, "카메라 오브젝트가 여러개라 선택해야함")
            guideTextView.text = getString(R.string.select_main_object)

            // 메인객체 고르기 메시지 노출
            binding.guideTextView.text = getString(R.string.select_main_object)
            binding.guidePercentTextView.text = ""
            for (component in cameraObjectComponentList) {
                val imageView = binding.layerLayout.createImageView(component)
                imageView.setOnClickListener {
                    Log.i(TAG, "메인 객체 선택")
                    guideTextView.text = ""
                    binding.layerLayout.removeAllViewsWithout(imageView)
                    guidingComponentImageView = imageView
                    match(component)
                }
                binding.layerLayout.addView(imageView)
            }
        }
        // 카메라 오브젝트 컴포넌트가 하나 뿐인 경우
        // 가이드 할 객체가 하나 뿐이라 간단함
        else if (cameraObjectComponentList.size == 1) {
            Log.i(TAG, "카메라 오브젝트가 하나뿐임")
            val component = cameraObjectComponentList[0]
            guidingComponentImageView = binding.layerLayout.createImageView(component)
            binding.layerLayout.addView(guidingComponentImageView)
            match(cameraObjectComponentList[0])
        }
    }

    /**
     * 컴포넌트와 가이드 이미지 컴포넌트를 매칭, 가이드 해주는 메소드
     */
    private fun match(component: ObjectComponent) {
        val targetComponent = componentMatcher.match(component, guideComponentList)
        if (targetComponent == null) {
            Toast.makeText(baseContext, "매칭할 오브젝트가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetComponent.guideList.isEmpty()) {
            if (targetComponent.mask.isEmpty() || targetComponent.mask[0].isEmpty())
                return

            val guider = GuideImageObjectGuider(
                targetComponent.mask[0].size,
                targetComponent.mask.size
            )

            guider.initGuideList(targetComponent)
        }

        guidingComponent = component
        this.targetComponent = targetComponent

        guidingGuide = if (targetComponent.guideList.isEmpty()) {
            // targetComponent 의 가이드가 없으면, guidingGuide 의 표준 구도를 추출
            makeStandardGuide(component)
            if (component.guideList.isEmpty()) null else component.guideList[0]
        } else {
            if (targetComponent.guideList.isEmpty()) null else targetComponent.guideList[0]
        }

        imageAnalyzer.setGuide(guidingComponent, this.targetComponent, guidingGuide, true)

        val imageView = binding.layerLayout.createImageView(targetComponent, true)
        binding.layerLayout.addView(imageView)
        displayGuide(guidingGuide)
    }

    private fun displayGuide(guide: Guide?) {
        if (guide == null) {
            binding.guideTextView.text = getString(R.string.require_more_accurate_position_and_area)
        } else {
            binding.guideTextView.text = GUIDE_MSG_LIST[guide.guideId]
            layerLayoutGuideManager.guide(guide)
        }
    }

    override fun onMatchComponent() {
        Log.i(TAG, "컴포넌트 매칭 성공")
        val component = guidingComponent
            ?: return

        component.guideCompleted = true
        if (!component.standardGuideCompleted && phaseTargetGuide) {
            component.standardGuideCompleted = true

            if (component is PersonComponent) {
                val alpha = 30
                val croppedX = max(component.roi.left - alpha, 0)
                val croppedY = max(component.roi.top - alpha, 0)
                val croppedWidth = min(component.roi.getWidth() + alpha * 2, imageAnalyzer.width - croppedX)
                val croppedHeight = min(component.roi.getHeight() + alpha * 2, imageAnalyzer.height - croppedY)

                val croppedImage = Bitmap.createBitmap(imageAnalyzer.bitmap, croppedX, croppedY, croppedWidth, croppedHeight)
                val rescaledImage = Bitmap.createScaledBitmap(croppedImage, MODEL_WIDTH, MODEL_HEIGHT, true)
                component.person = posenet.estimateSinglePose(rescaledImage).convertTo(
                    Area(
                        Point(croppedX, croppedY),
                        Point(croppedX + croppedWidth, croppedY + croppedHeight)
                    )
                )

                makeStandardGuide(component)
                if (component.guideList.isEmpty()) {
                    layerLayoutGuideManager.guide(null)
                    autoTakePhoto()
                } else {
                    // 표준 구도 가이드 제공
                    guidingGuide = component.guideList[0]
                    imageAnalyzer.setGuide(guidingComponent, null, guidingGuide, true)
                    runOnUiThread {
                        layerLayoutGuideManager.guide(guidingGuide)
                        binding.layerLayout.removeAllViewsWithout(guidingComponentImageView!!)
                        displayGuide(guidingGuide)
                    }
                }
            }
        } else {
            layerLayoutGuideManager.guide(null)
            runOnUiThread {
                autoTakePhoto()
            }
        }
    }

    private fun autoTakePhoto() {
        Log.i(TAG, "모든 컴포넌트 가이드 성공")
        Log.i(TAG, "자동촬영!")
        imageAnalyzer.setGuide(null, null, null, false)
        if (componentList.hasPerson()) {
            binding.guideTextView.text = "자세를 낮추어 아래에서 찍으면 비율이 좋아집니다"
            binding.guidePercentTextView.text = ""
        }

        handler.postDelayed(Runnable {
            binding.layerLayout.removeAllViews()
            countDownTimer(TimerMode.values()[1])
            binding.guideTextView.text = "자동촬영"
            binding.guidePercentTextView.text = ""
        }, 2000)
    }

    private fun getRecentImage(): Uri? {
        if(ContextCompat.checkSelfPermission(
                baseContext,
                permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED)
        {
            return null
        }

        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN
        )
        var selectionClause = null

        /* 최신순 */
        val sortOrder = MediaStore.Images.ImageColumns._ID + " DESC "
        cursor =
            contentResolver.query(uriExternal, projection, selectionClause, null, sortOrder)

        var result: Uri? = null
        if (cursor?.moveToFirst()!!) {
            val thumbColumn: Int =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
            val thumpId: Int = cursor.getInt(thumbColumn)
            result = ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                thumpId.toLong()
            )
        }
        cursor.close()
        return result
    }

    override fun onUpdateSpot(spotList: ArrayList<Spot>) {
        var count = 0
        for (spot in spotList) {
            val imageUrlList = spot.imageUrlList
                ?: continue

            binding.guideImageCategoryTabLayout.addTab(
                GuideTabItem(spot.name, imageUrlList as ArrayList<String>)
            )
            count += 1
        }

        if (count > 0) {
            val tab = this.binding.guideImageCategoryTabLayout.tabLayout.getTabAt(1)
            this.binding.guideImageCategoryTabLayout.tabLayout.selectTab(tab)
        }
    }

    override fun onUpdateRecommendedCacheImage(imageList: ArrayList<String>) {
        Log.i(TAG, "임시 이미지 도착")

        this.recommendedImageUrlList.clear()
        this.recommendedImageUrlList.addAll(imageList)

        runOnUiThread {
            val tab = this.binding.guideImageCategoryTabLayout.tabLayout.getTabAt(0)
            this.binding.guideImageCategoryTabLayout.tabLayout.selectTab(tab)
            binding.guideImageCategoryTabLayout.addImageUrlList(imageList)
        }
    }

    override fun onCloseSplash() {
        supportFragmentManager.beginTransaction()
            .remove(splashFragment)
            .commitAllowingStateLoss()
    }

    override fun onUpdateMatchGuidePercent(percent: Double) {
        val percentString = "%.1f".format(percent * 100) + "%"
        runOnUiThread {
            binding.guidePercentTextView.text = percentString
        }
    }
}

