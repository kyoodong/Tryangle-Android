package com.gomson.tryangle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MLExecutionViewModel : ViewModel() {
    private val _resultingBitmap = MutableLiveData<ModelExecutionResult>()

    val resultingBitmap: LiveData<ModelExecutionResult>
        get() = _resultingBitmap

    private val viewModelJob = Job()
    private val viewModelScope = CoroutineScope(viewModelJob)

    fun onApplyModel(
        filePath: String,
        imageSegmentationModel: ImageSegmentationModelExecutor,
        inferenceThread: ExecutorCoroutineDispatcher
    ) {
        viewModelScope.launch(inferenceThread) {
            val contentImage = BitmapFactory.decodeFile(filePath)
            val result = imageSegmentationModel.execute(contentImage)
            _resultingBitmap.postValue(result)
        }
    }

    fun onApplyModel(
        bitmap: Bitmap,
        imageSegmentationModel: ImageSegmentationModelExecutor,
        inferenceThread: ExecutorCoroutineDispatcher
    ) {
        viewModelScope.launch(inferenceThread) {
            val contentImage = bitmap
            val result = imageSegmentationModel.execute(contentImage)
            _resultingBitmap.postValue(result)
        }
    }
}