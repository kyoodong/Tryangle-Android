#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"

JNIEXPORT void JNICALL
Java_com_gomson_tryangle_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                       jlong mat_addr_input,
                                                       jlong mat_addr_result) {
    Mat &inputImage = *(Mat *) mat_addr_input;
    Mat &outputImage = *(Mat *) mat_addr_result;
    cvtColor(inputImage, outputImage, COLOR_RGB2GRAY);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_gomson_tryangle_MainActivity_MatchFeature(JNIEnv *env, jobject thiz, jlong mat_addr_input1,
                                                   jlong mat_addr_input2) {
    Mat &inputImage1 = *(Mat *) mat_addr_input1;
    Mat &inputImage2 = *(Mat *) mat_addr_input2;
    Mat resizeImage1, resizeImage2;
    Mat image1, image2;

    cvtColor(inputImage1, image1, COLOR_RGB2GRAY);
    cvtColor(inputImage2, image2, COLOR_RGB2GRAY);

    cv::Size size = cv::Size( 256, 256 );

    cv::resize( image1, resizeImage1, size );
    cv::resize( image2, resizeImage2, size );

    Ptr<AKAZE> detector = AKAZE::create();
//    Ptr<SIFT> detector = SIFT::create();
    std::vector<KeyPoint> keypoints1, keypoints2;
    Mat descriptors1, descriptors2;
    detector->detectAndCompute( resizeImage1, noArray(), keypoints1, descriptors1 );
    detector->detectAndCompute( resizeImage2, noArray(), keypoints2, descriptors2 );

//    FlannBasedMatcher matcher = FlannBasedMatcher();
//    BFMatcher matcher(NORM_HAMMING);
    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create(DescriptorMatcher::BRUTEFORCE);
    std::vector< std::vector<DMatch> > knn_matches;

//    if (descriptors1.empty() || descriptors2.empty() || keypoints1.empty() || keypoints2.empty())
//        return -1;

    matcher->knnMatch( descriptors1, descriptors2, knn_matches, 2 );

    int count = 0;
    const float ratio_thresh = 0.7f;
    std::vector<DMatch> good_matches;
    for (size_t i = 0; i < knn_matches.size(); i++)
    {
        if (knn_matches[i][0].distance < ratio_thresh * knn_matches[i][1].distance)
        {
            count++;
        }
    }

    if (knn_matches.empty())
        return 0;

    // 매칭 비율 리턴
    return count / knn_matches.size() * 100;
}
