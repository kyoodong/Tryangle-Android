package com.gomson.tryangle

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.network.ModelService
import kotlinx.android.synthetic.main.activity_splash.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val MODEL_VERSION = "MODEL_VERSION"
private const val FEATURE_VERSION = "FEATURE_VERSION"
private const val LAST_VERSION_CHECKED = "LAST_VERSION_CHECKED"

private const val TAG = "SplashActivity"

private const val TOKEN_INTERVAL = 1000 * 60 * 60 * 24 * 14

class SplashActivity : AppCompatActivity() {

    private lateinit var modelService: ModelService

    private fun getModelVersion(): String? {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getString(MODEL_VERSION, null)
    }

    private fun setModelVersion(version: String) {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(MODEL_VERSION, version)
        editor.commit()
    }

    private fun getFeatureVersion(): String? {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getString(FEATURE_VERSION, null)
    }

    private fun setFeatureVersion(version: String) {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(FEATURE_VERSION, version)
        editor.commit()
    }

    private fun getLastVersionCheck(): Long {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LAST_VERSION_CHECKED, 0)
    }

    private fun setLastVersionCheck(time: Long) {
        val sharedPreferences =
            baseContext.getSharedPreferences(baseContext.getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putLong(LAST_VERSION_CHECKED, time)
        editor.commit()
    }

    private fun goToMain() {
        if (System.currentTimeMillis() - getLastVersionCheck() >= TOKEN_INTERVAL) {
            setLastVersionCheck(System.currentTimeMillis())
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showWait(message: String) {
        waitLayout.visibility = View.VISIBLE
        messageTextView.text = message
    }

    private fun hideWait() {
        waitLayout.visibility = View.INVISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()

        modelService = ModelService(baseContext)

        val modelVersion = getModelVersion()
        val featureVersion = getFeatureVersion()

        Log.i(TAG, "토큰발급 요청")

        val lastVersionCheckTime = getLastVersionCheck()

        // @TODO invalid token 체크

        // 2주
        if (System.currentTimeMillis() - lastVersionCheckTime >= TOKEN_INTERVAL) {
            showWait("토큰 발급 중...")
            modelService.issueToken(object : Callback<AccessToken> {
                override fun onResponse(call: Call<AccessToken>, response: Response<AccessToken>) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "토큰발급 성공")
                        var count = 0
                        showWait("모델 버전 확인 중...")
                        Log.i(TAG, "모델 버전 확인 요청")
                        modelService.getLatestModelVersion(object: Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.isSuccessful) {
                                    Log.i(TAG, "모델 버전 로딩 성공")
                                    // 모델 버전이 다름
                                    val latestVersion = response.body()!!.string()
                                    if (modelVersion == null || modelVersion != latestVersion) {
                                        Log.i(TAG, "모델 버전 다름")
                                        showWait("최신버전 모델 다운로드 중...")
                                        modelService.downloadModel(latestVersion, object: ModelService.ModelCallback {
                                            override fun onSuccess() {
                                                Log.i(TAG, "모델 다운로드 성공")
                                                count++

                                                if (count == 2) {
                                                    goToMain()
                                                }
                                                setModelVersion(latestVersion)
                                            }

                                            override fun onFailure() {
                                                Log.i(TAG, "모델 다운로드 실패")
                                                Toast.makeText(baseContext, "모델 다운로드 실패", Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                        })
                                    } else {
                                        Log.i(TAG, "모델 버전 같음")
                                        count++
                                    }

                                    if (count == 2) {
                                        goToMain()
                                    }
                                } else {
                                    Log.i(TAG, "모델 버전 로딩 실패")
                                    Toast.makeText(baseContext, "모델 최신 버전 로딩 실패! ${response.code()}", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.i(TAG, "모델 버전 로딩 실패")
                                t.printStackTrace()
                                Toast.makeText(baseContext, "모델 최신 버전 로딩 실패!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        })

                        Log.i(TAG, "피쳐 버전 확인 요청")
                        modelService.getLatestFeatureVersion(object: Callback<ResponseBody> {
                            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                if (response.isSuccessful) {
                                    Log.i(TAG, "피쳐 버전 로딩 성공")

                                    val latestVersion = response.body()!!.string()
                                    if (featureVersion == null || featureVersion != latestVersion) {
                                        Log.i(TAG, "피쳐 버전 다름")
                                        showWait("최신버전 피쳐 다운로드 중...")
                                        modelService.downloadFeature(latestVersion, object: ModelService.ModelCallback {
                                            override fun onSuccess() {
                                                Log.i(TAG, "피쳐 다운로드 성공")
                                                count++

                                                if (count == 2) {
                                                    goToMain()
                                                }
                                                setFeatureVersion(latestVersion)
                                            }

                                            override fun onFailure() {
                                                Log.i(TAG, "피쳐 다운로드 실패")
                                                Toast.makeText(baseContext, "피쳐 다운로드 실패!", Toast.LENGTH_SHORT).show()
                                                finish()
                                            }
                                        })
                                    } else {
                                        Log.i(TAG, "피쳐 버전 같음")
                                        count++
                                    }
                                    if (count == 2) {
                                        goToMain()
                                    }
                                } else {
                                    Log.i(TAG, "피쳐 버전 로딩 실패")
                                    Toast.makeText(baseContext, "피쳐 최신 버전 로딩 실패! ${response.code()}", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.i(TAG, "피쳐 버전 로딩 실패")
                                t.printStackTrace()
                                Toast.makeText(baseContext, "피쳐 최신 버전 로딩 실패!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        })
                    } else {
                        Log.i(TAG, "토큰발급 실패")
                        Toast.makeText(baseContext, "토큰 발급 실패!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                    Log.i(TAG, "토큰발급 실패")
                    Toast.makeText(baseContext, "토큰 발급 실패!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        } else {
            goToMain()
        }
    }
}