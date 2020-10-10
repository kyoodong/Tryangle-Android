#include <jni.h>
#include <opencv2/opencv.hpp>
#include <algorithm>

using namespace cv;

struct point {
    int x, y;
};

struct line {
    struct point start_point, end_point;
};

enum line_type {
    vertical, horizontal
};

void getLine(double x1, double y1, double x2, double y2, double &a, double &b, double &c)
{
    // (x- p1X) / (p2X - p1X) = (y - p1Y) / (p2Y - p1Y)
    a = y1 - y2; // Note: this was incorrectly "y2 - y1" in the original answer
    b = x2 - x1;
    c = x1 * y2 - x2 * y1;
}

// point 와 line의 거리를 구해주는 함수
double dist(struct point point, struct line line)
{
    double a, b, c;
    getLine(line.start_point.x, line.start_point.y, line.end_point.x, line.end_point.y, a, b, c);
    return abs(a * point.x + b * point.y + c) / sqrt(a * a + b * b);
}

int clamp(int num, int min, int max) {
    if (num < min)
        return min;

    if (num > max)
        return max;
    return num;
}

class Cluster {

private:
    std::vector<struct line> lines;
    enum line_type type;
    int threshold = 25;
    struct point center;

public:
    struct line representive_line;

    Cluster(enum line_type type, struct point center) {
        this->type = type;
        this->center = center;
    }

    enum line_type get_type() {
        return type;
    }

    void add_line(struct line line) {
        int size = lines.size();

        lines.push_back(line);

        // 새 대표값 (평균)
        representive_line = {{(representive_line.start_point.x * size + line.start_point.x) / (size + 1),
                            (representive_line.start_point.y * size + line.start_point.y) / (size + 1)},
                    {(representive_line.end_point.x * size + line.end_point.x) / (size + 1),
                            (representive_line.end_point.y * size + line.end_point.y) / (size + 1)}
        };
    }

    bool can_include(const struct line &line) {
        if (type == vertical) {
            return abs(representive_line.start_point.x - line.start_point.x) < threshold
                   && abs(representive_line.end_point.x - line.end_point.x) < threshold;
        }
        return abs(representive_line.start_point.y - line.start_point.y) < threshold
               && abs(representive_line.end_point.y - line.end_point.y) < threshold;
    }

    int score() const {
        int length = (int) sqrt(
                pow((representive_line.start_point.x - representive_line.end_point.x), 2)
                + pow((representive_line.start_point.y - representive_line.end_point.y), 2)
        );
        double distance = dist(center, representive_line);

        return lines.size() * 50 + length + \
               clamp((int) (std::max(center.x, center.y) * 2 - distance), 0, 300);
    }

    bool operator <(const Cluster &target) const {
        return score() < target.score();
    }

    bool operator >(const Cluster &target) const {
        return score() > target.score();
    }
};

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_gomson_tryangle_Hough_find_1hough_1line(JNIEnv *env, jobject thiz, jlong mat_addr_input) {
    jclass class_line = env->FindClass("com/gomson/tryangle/domain/LineComponent");
    jmethodID line_constructor = env->GetMethodID(class_line, "<init>",
                                                  "(JJIIII)V");

    Mat &image = *(Mat *) mat_addr_input;
//    resize(image, image, Size(640, 640));

    float kernel_data[9] = {0, -1, 0,
                          -1, 5, -1,
                          0, -1, 0};
    Mat kernel = Mat(3, 3, CV_32F, kernel_data);
    Mat gray, filtered_image, erode_image, canny_image;
    std::vector<Vec4i> hough_lines;

    cvtColor(image, gray, COLOR_RGB2GRAY);
    filter2D(gray, filtered_image, -1, kernel);

    Mat mask = cv::getStructuringElement(cv::MORPH_RECT, cv::Size(4, 4));
    erode(filtered_image, erode_image, mask, Point(-1, -1), 1, BORDER_REFLECT_101);

    Canny(erode_image, canny_image, 1500, 2500, 5, true);
    HoughLinesP(canny_image, hough_lines, 1, 3.14 / 360, 100, 200, 200);

    if (hough_lines.empty()) {
        return nullptr;
    }

    std::vector<Cluster> clusters;

    for (Vec4i hough_e_line : hough_lines) {
        int x1, y1, x2, y2;

        x1 = hough_e_line[0];
        y1 = hough_e_line[1];
        x2 = hough_e_line[2];
        y2 = hough_e_line[3];

        struct line line = {x1, y1, x2, y2};

        int threshold = 35;

        // 수평선만 검출
        if (abs(y1 - y2) < threshold) {
            if (x2 < x1) {
                int tmp;
                tmp = x2;
                x2 = x1;
                x1 = tmp;

                tmp = y2;
                y2 = y1;
                y1 = tmp;
            }

            bool is_include = false;

            for (Cluster cluster : clusters) {
                if (cluster.can_include(line)) {
                    is_include = true;
                    cluster.add_line(line);
                    break;
                }
            }

            if (!is_include) {
                struct point center = {image.cols, image.rows};
                Cluster cluster = Cluster(vertical, center);
                cluster.add_line(line);
                clusters.push_back(cluster);
            }
        }

        // 수직선
        else if (abs(x1 - x2) < threshold) {
            if (y2 < y1) {
                int tmp;
                tmp = x2;
                x2 = x1;
                x1 = tmp;

                tmp = y2;
                y2 = y1;
                y1 = tmp;
            }

            bool is_include = false;

            for (Cluster cluster : clusters) {
                if (cluster.can_include(line)) {
                    is_include = true;
                    cluster.add_line(line);
                    break;
                }
            }

            if (!is_include) {
                struct point center = {image.cols, image.rows};
                Cluster cluster = Cluster(horizontal, center);
                cluster.add_line(line);
                clusters.push_back(cluster);
            }
        }
    }

    std::sort(clusters.begin(), clusters.end(), std::greater<Cluster>());

    jobjectArray result = env->NewObjectArray(std::min(3, (int) clusters.size()), class_line,
                                              env->NewObject(class_line,
                                                      line_constructor,
                                                             (jlong) 0,
                                                             (jlong) 0,
                                                             0, 0, 0, 0));

    for (int i = 0; i < clusters.size() && i < 3; i++) {
        jobject line = env->NewObject(class_line, line_constructor,
                (jlong) 0,
                (jlong) 0,
                clusters[i].representive_line.start_point.x,
                clusters[i].representive_line.start_point.y,
                clusters[i].representive_line.end_point.x,
                clusters[i].representive_line.end_point.y);
        env->SetObjectArrayElement(result, i, line);
    }

    return result;
}