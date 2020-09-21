package com.gomson.tryangle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.popup_more.view.*
import kotlinx.android.synthetic.main.popup_ratio.view.*

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    val CAMERA_PERMISSION_CODE = 100

    lateinit var popupMoreView: PopupWindow
    lateinit var popupRatioView: PopupWindow
    var currentRatio = RatioMode.RATIO_1_1

    val ratioPopupViewClickListener = View.OnClickListener{ view->
        var clickRatio = RatioMode.RATIO_3_4
        when(view.id){
            R.id.ratio3_4 -> {clickRatio = RatioMode.RATIO_3_4}
            R.id.ratio1_1 -> {clickRatio = RatioMode.RATIO_1_1}
            R.id.ratio9_16 -> {clickRatio = RatioMode.RATIO_9_16}
            R.id.ratioFull -> {clickRatio = RatioMode.RATIO_FULL}
            R.id.constraintLayout -> {clickRatio = RatioMode.RATIO_FULL}
        }

        if(clickRatio != currentRatio) {
            currentRatio = clickRatio
            cameraView.updateRatioMode(currentRatio)
        }
        popupRatioView.dismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutInflater.inflate(R.layout.popup_more, null).let {
            popupMoreView = PopupWindow(it, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
            it.flashLayout.setOnClickListener {

            }
            it.timerLayout.setOnClickListener {

            }
            it.gridLayout.setOnClickListener {

            }
            it.settingLayout.setOnClickListener {

            }
        }

        layoutInflater.inflate(R.layout.popup_ratio, null).let{
            it.ratio1_1.setOnClickListener(ratioPopupViewClickListener)
            it.ratio3_4.setOnClickListener(ratioPopupViewClickListener)
            it.ratio9_16.setOnClickListener(ratioPopupViewClickListener)
            it.ratioFull.setOnClickListener(ratioPopupViewClickListener)
            popupRatioView = PopupWindow(it, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        }

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }

        ratioBtn.setOnClickListener {
            popupRatioView.animationStyle=-1
            popupRatioView.showAsDropDown(topLayout, 0, 0)
        }
        moreBtn.setOnClickListener{
            popupMoreView.showAsDropDown(topLayout,0,0)
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "카메라 권한 획득에 실패했습니다.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                // 권한이 없는 경우
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                cameraView.openCamera()
            }
        }
    }
}