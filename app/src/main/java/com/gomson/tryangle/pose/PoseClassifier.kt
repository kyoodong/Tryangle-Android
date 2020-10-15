package com.gomson.tryangle.pose

import android.util.Log
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.KeyPoint

private const val TAG = "PoseClassifier"

class PoseClassifier {

    private val THRESHOLD = 0.01
    private var keypointMap = HashMap<BodyPart, KeyPoint>()

    fun classify(keypoints: Array<KeyPoint>): PoseClass {
        keypointMap.clear()

        for (keypoint in keypoints) {
            if (keypoint.score > THRESHOLD) {
                keypointMap.put(keypoint.bodyPart, keypoint)
            }
        }

        // 무릎의 높이가 엉덩이의 높이보다 낮은 경우, 서 있다(stand)고 판단
        if ((keypointMap[BodyPart.LEFT_KNEE] != null && keypointMap[BodyPart.LEFT_HIP] != null
                    && keypointMap[BodyPart.LEFT_KNEE]!!.position.y > keypointMap[BodyPart.LEFT_HIP]!!.position.y)
            || (keypointMap[BodyPart.LEFT_ANKLE] != null && keypointMap[BodyPart.LEFT_HIP] != null
                    && keypointMap[BodyPart.LEFT_ANKLE]!!.position.y > keypointMap[BodyPart.LEFT_HIP]!!.position.y)
            || (keypointMap[BodyPart.RIGHT_KNEE] != null && keypointMap[BodyPart.RIGHT_HIP] != null
                    && keypointMap[BodyPart.RIGHT_KNEE]!!.position.y > keypointMap[BodyPart.RIGHT_HIP]!!.position.y)
            || (keypointMap[BodyPart.RIGHT_ANKLE] != null && keypointMap[BodyPart.RIGHT_HIP] != null
                    && keypointMap[BodyPart.RIGHT_ANKLE]!!.position.y > keypointMap[BodyPart.RIGHT_HIP]!!.position.y)) {
            Log.d(TAG, "STAND")
            return PoseClass.STAND
        }

        Log.d(TAG, "UNKNOWN")
        return PoseClass.UNKNOWN
    }

}