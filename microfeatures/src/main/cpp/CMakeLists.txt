# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("microfeatures")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        tensorflow/lite/experimental/microfrontend/lib/kiss_fft_int16.cc
        tensorflow/lite/experimental/microfrontend/lib/fft.cc
        tensorflow/lite/experimental/microfrontend/lib/fft_util.cc
        tensorflow/lite/experimental/microfrontend/lib/filterbank.c
        tensorflow/lite/experimental/microfrontend/lib/filterbank_util.c
        tensorflow/lite/experimental/microfrontend/lib/frontend.c
        tensorflow/lite/experimental/microfrontend/lib/frontend_util.c
        tensorflow/lite/experimental/microfrontend/lib/log_lut.c
        tensorflow/lite/experimental/microfrontend/lib/log_scale.c
        tensorflow/lite/experimental/microfrontend/lib/log_scale_util.c
        tensorflow/lite/experimental/microfrontend/lib/noise_reduction.c
        tensorflow/lite/experimental/microfrontend/lib/noise_reduction_util.c
        tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control.c
        tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control_util.c
        tensorflow/lite/experimental/microfrontend/lib/window.c
        tensorflow/lite/experimental/microfrontend/lib/window_util.c
        kissfft/kiss_fft.c
        kissfft/tools/kiss_fftr.c
        MicroFrontend.cpp
)
set(BASEPATH "${CMAKE_SOURCE_DIR}")
include_directories("${BASEPATH}" "${BASEPATH}/kissfft")

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
#target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        #android
        #log)

add_compile_definitions(VERSION_INFO=1.0.0)
set (CMAKE_CXX_FLAGS "-DFIXED_POINT=16")