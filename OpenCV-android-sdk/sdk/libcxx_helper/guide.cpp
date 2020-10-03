#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

//extern "C"
//JNIEXPORT jint JNICALL
//Java_com_gomson_tryangle_MainActivity_MatchFeature(JNIEnv *env, jobject thiz, jlong mat_addr_input1,
//                                                   jlong mat_addr_input2) {
//    Mat &inputImage1 = *(Mat *) mat_addr_input1;
//    Mat &inputImage2 = *(Mat *) mat_addr_input2;
//
//    channels = image.shape[-1]
//    height = image.shape[0]
//    width = image.shape[1]
//    layered_image = np.zeros_like(image)
//    cogs = list()
//    areas = list()
//    for i in range(channels):
//    visits = np.zeros_like(image[:, :, i])
//    is_finish = False
//    for h in range(height):
//    for w in range(width):
//    if image[h][w][i] and not visits[h][w]:
//# bfs 를 돌려서 외곽선 탐색
//    l, cog, area = __get_contour_center_point(image[:, :, i], w, h, visits, threshold)
//    layered_image[:, :, i] += l
//    cogs.append(cog)
//    areas.append(area)
//    is_finish = True
//    break
//    if is_finish:
//        break
//    return layered_image, cogs, areas
//}
