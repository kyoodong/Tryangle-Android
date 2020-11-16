package com.gomson.tryangle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gomson.tryangle.domain.AccessToken
import com.gomson.tryangle.network.ModelService
import kotlinx.android.synthetic.main.fragment_splash.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val MODEL_VERSION = "MODEL_VERSION"
private const val FEATURE_VERSION = "FEATURE_VERSION"

private const val TAG = "SplashActivity"

class SplashFragment(private val listener: OnCloseSplashListener) : Fragment() {

    private lateinit var modelService: ModelService
    private lateinit var tokenManager: TokenManager
    private var isReady = false

    interface OnCloseSplashListener {
        fun onCloseSplash()
    }

    private fun getModelVersion(): String? {
        val sharedPreferences =
            requireActivity().getSharedPreferences(requireActivity().getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getString(MODEL_VERSION, null)
    }

    private fun setModelVersion(version: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(requireActivity().getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(MODEL_VERSION, version)
        editor.commit()
    }

    private fun getFeatureVersion(): String? {
        val sharedPreferences =
            requireActivity().getSharedPreferences(requireActivity().getString(R.string.app_name), Context.MODE_PRIVATE)
        return sharedPreferences.getString(FEATURE_VERSION, null)
    }

    private fun setFeatureVersion(version: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(requireActivity().getString(R.string.app_name), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(FEATURE_VERSION, version)
        editor.commit()
    }

    fun goToMain() {
        if (tokenManager.isExpired()) {
            tokenManager.setLastVersionCheckTime(System.currentTimeMillis())
        }

        listener.onCloseSplash()
    }

    private fun showWait(message: String) {
        waitLayout.visibility = View.VISIBLE
        messageTextView.text = message
    }

    private fun hideWait() {
        waitLayout.visibility = View.INVISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash, container, false)
        return view
    }

    override fun onResume() {
        super.onResume()

        modelService = ModelService(requireActivity())
        tokenManager = TokenManager(requireContext())

        val modelVersion = getModelVersion()
        val featureVersion = getFeatureVersion()

        Log.i(TAG, "토큰발급 요청")

        // @TODO invalid token 체크

        isReady = true
        // 2주
        if (tokenManager.isExpired()) {
            isReady = false
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
                                                Toast.makeText(requireActivity(), "모델 다운로드 실패", Toast.LENGTH_SHORT).show()
                                                requireActivity().finish()
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
                                    Toast.makeText(requireActivity(), "모델 최신 버전 로딩 실패! ${response.code()}", Toast.LENGTH_SHORT).show()
                                    requireActivity().finish()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.i(TAG, "모델 버전 로딩 실패")
                                t.printStackTrace()
                                Toast.makeText(requireActivity(), "모델 최신 버전 로딩 실패!", Toast.LENGTH_SHORT).show()
                                requireActivity().finish()
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
                                                Toast.makeText(requireActivity(), "피쳐 다운로드 실패!", Toast.LENGTH_SHORT).show()
                                                requireActivity().finish()
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
                                    Toast.makeText(requireActivity(), "피쳐 최신 버전 로딩 실패! ${response.code()}", Toast.LENGTH_SHORT).show()
                                    requireActivity().finish()
                                }
                            }

                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                Log.i(TAG, "피쳐 버전 로딩 실패")
                                t.printStackTrace()
                                Toast.makeText(requireActivity(), "피쳐 최신 버전 로딩 실패!", Toast.LENGTH_SHORT).show()
                                requireActivity().finish()
                            }
                        })
                    } else {
                        Log.i(TAG, "토큰발급 실패")
                        Toast.makeText(requireActivity(), "토큰 발급 실패!", Toast.LENGTH_SHORT).show()
                        requireActivity().finish()
                    }
                }

                override fun onFailure(call: Call<AccessToken>, t: Throwable) {
                    Log.i(TAG, "토큰발급 실패")
                    Toast.makeText(requireActivity(), "토큰 발급 실패!", Toast.LENGTH_SHORT).show()
                    requireActivity().finish()
                }
            })
        }
    }

    fun onCameraSetup() {
        if (isReady) {
            listener.onCloseSplash()
        }
    }
}