cmake_minimum_required(VERSION 3.4.1)
set(TARGET pytorch_nativeapp)
project(${TARGET} CXX)
set(CMAKE_CXX_STANDARD 14)

set(build_DIR ${CMAKE_SOURCE_DIR}/build)

set(pytorch_testapp_cpp_DIR ${CMAKE_CURRENT_LIST_DIR}/src/main/cpp)
file(GLOB pytorch_testapp_SOURCES
        ${pytorch_testapp_cpp_DIR}/image_retrieval.cpp
        )

add_library(${TARGET} SHARED
        ${pytorch_testapp_SOURCES}
        )

file(GLOB PYTORCH_INCLUDE_DIRS "${build_DIR}/pytorch_android*.aar/headers")
file(GLOB PYTORCH_LINK_DIRS "${build_DIR}/pytorch_android*.aar/jni/${ANDROID_ABI}")

target_compile_options(${TARGET} PRIVATE
        -fexceptions
        )

set(BUILD_SUBDIR ${ANDROID_ABI})

find_library(PYTORCH_LIBRARY pytorch_jni
        PATHS ${PYTORCH_LINK_DIRS}
        NO_CMAKE_FIND_ROOT_PATH)
find_library(FBJNI_LIBRARY fbjni
        PATHS ${PYTORCH_LINK_DIRS}
        NO_CMAKE_FIND_ROOT_PATH)

set(ENV{OPENCV_ANDROID_SDK} ${CMAKE_CURRENT_LIST_DIR}/../OpenCV-android-sdk)

# OpenCV
if(NOT DEFINED ENV{OPENCV_ANDROID_SDK})
    message(FATAL_ERROR "Environment var OPENCV_ANDROID_SDK is not set")
endif()

set(OPENCV_INCLUDE_DIR "$ENV{OPENCV_ANDROID_SDK}/sdk/native/jni/include")

target_include_directories(${TARGET} PRIVATE
        "${OPENCV_INCLUDE_DIR}"
        ${PYTORCH_INCLUDE_DIRS})

set(OPENCV_LIB_DIR "$ENV{OPENCV_ANDROID_SDK}/sdk/native/libs/${ANDROID_ABI}")

find_library(OPENCV_LIBRARY opencv_java4
        PATHS ${OPENCV_LIB_DIR}
        NO_CMAKE_FIND_ROOT_PATH)

target_link_libraries(${TARGET}
        ${PYTORCH_LIBRARY}
        ${FBJNI_LIBRARY}
        ${OPENCV_LIBRARY}
        log)



###################

set(pathPROJECT /Users/dongkyoo/Develop/TryAngle)
set(pathOPENCV ${pathPROJECT}/OpenCV-android-sdk/sdk)
set(pathLIBOPENCV_JAVA ${pathOPENCV}/native/libs/${ANDROID_ABI}/libopencv_java4.so)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathOPENCV}/native/jni/include)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

file(GLOB native_lib_SOURCES
        ${pytorch_testapp_cpp_DIR}/image_match.cpp
        ${pytorch_testapp_cpp_DIR}/guide.cpp
        ${pytorch_testapp_cpp_DIR}/hough.cpp
        )

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${native_lib_SOURCES}
        )


add_library(lib_opencv SHARED IMPORTED)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        native-lib
        ${OPENCV_LIBRARY}
        ${log-lib})
