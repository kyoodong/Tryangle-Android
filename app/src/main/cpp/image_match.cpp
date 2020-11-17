#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

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
JNIEXPORT jobject JNICALL
Java_com_gomson_tryangle_ImageAnalyzer_MatchFeature(JNIEnv *env, jobject thiz, jlong mat_addr_input1,
                                                   jlong mat_addr_input2, jint ratio_in_roi) {
    jclass cls = env->FindClass("com/gomson/tryangle/dto/MatchingResult");
    jmethodID constructor = env->GetMethodID(cls, "<init>", "(IFFFFFFFF)V");

    struct timeval startTime;

    Mat &inputImage1 = *(Mat *) mat_addr_input1;
    Mat &inputImage2 = *(Mat *) mat_addr_input2;
    Mat image1, image2;
    Mat resizedImage1, resizedImage2;

    gettimeofday(&startTime, NULL);
    cvtColor(inputImage1, image1, COLOR_RGB2GRAY);
    cvtColor(inputImage2, image2, COLOR_RGB2GRAY);

    resize(image1, resizedImage1, Size(image1.cols / 2, image1.rows / 2));
    resize(image2, resizedImage2, Size(image2.cols / 2, image2.rows / 2));

//    Ptr<AKAZE> detector = AKAZE::create();
    Ptr<SIFT> detector = SIFT::create();
//    Ptr<ORB> detector = ORB::create();
//    Ptr<FastFeatureDetector> detector = FastFeatureDetector::create();
//    Ptr<SiftFeatureDetector> detector = SiftFeatureDetector::create();
//    Ptr<SiftDescriptorExtractor> extractor = SiftDescriptorExtractor::create();

    std::vector<KeyPoint> keypoints1, keypoints2;
    Mat descriptors1, descriptors2;

    detector->detectAndCompute( resizedImage1, noArray(), keypoints1, descriptors1 );
    detector->detectAndCompute( resizedImage2, noArray(), keypoints2, descriptors2 );

    if (keypoints1.size() < 2 || keypoints2.size() < 2)
        return nullptr;

//    detector->detect(image1, keypoints1);
//    detector->detect(image2, keypoints2);

//    FlannBasedMatcher matcher = FlannBasedMatcher();
    Ptr<DescriptorMatcher> matcher = DescriptorMatcher::create(
            DescriptorMatcher::FLANNBASED);
    std::vector< std::vector<DMatch> > knn_matches;

//    extractor->compute(image1, keypoints1, descriptors1);
//    extractor->compute(image2, keypoints2, descriptors2);

//    if (descriptors1.empty() || descriptors2.empty() || keypoints1.empty() || keypoints2.empty())
//        return -1;

    matcher->knnMatch( descriptors1, descriptors2, knn_matches, 2 );
//    matcher.knnMatch(descriptors1, descriptors2, knn_matches, 2);

    int count = 0;
    const float ratio_thresh = 0.7f;
    std::vector<DMatch> good_matches;
    for (size_t i = 0; i < knn_matches.size(); i++)
    {
        if (knn_matches[i][0].distance < ratio_thresh * knn_matches[i][1].distance)
        {
            good_matches.push_back(knn_matches[i][0]);
            count++;
        }
    }

    if (knn_matches.empty())
        return env->NewObject(cls, constructor, 0, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f);

    std::vector<Point2f> obj;
    std::vector<Point2f> scene;

    if (good_matches.empty())
        return env->NewObject(cls, constructor, 0, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f);

    for (DMatch match : good_matches) {
        obj.push_back(keypoints1[match.queryIdx].pt);
        scene.push_back(keypoints2[match.trainIdx].pt);
    }

    Mat H = findHomography(obj, scene, RANSAC);
    if (H.empty())
        return env->NewObject(cls, constructor, 0, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f, 0.f);

    std::vector<Point2f> obj_corners(4);
    obj_corners[0] = Point2f(0, 0);
    obj_corners[1] = Point2f( (float)resizedImage1.cols, 0 );
    obj_corners[2] = Point2f( (float)resizedImage1.cols, (float)resizedImage1.rows );
    obj_corners[3] = Point2f( 0, (float)resizedImage1.rows );
    std::vector<Point2f> scene_corners(4);
    perspectiveTransform( obj_corners, scene_corners, H);

    // 매칭 비율 리턴
    float ratio = (float) count / knn_matches.size() * 100;
    jobject result = env->NewObject(cls, constructor, (int) (ratio / ratio_in_roi * 100),
                                    scene_corners[0].x * 2, scene_corners[0].y * 2,
                                    scene_corners[1].x * 2, scene_corners[1].y * 2,
                                    scene_corners[2].x * 2, scene_corners[2].y * 2,
                                    scene_corners[3].x * 2, scene_corners[3].y * 2);
    return result;
}
