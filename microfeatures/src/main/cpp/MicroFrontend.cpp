


#include <jni.h>
#include <iostream>
#include <vector>

#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"
#include "tensorflow/lite/experimental/microfrontend/lib/frontend_util.h"

static const uint8_t FEATURES_STEP_SIZE = 10;
static const uint8_t PREPROCESSOR_FEATURE_SIZE = 40;
static const uint8_t FEATURE_DURATION_MS = 30;
static const uint16_t AUDIO_SAMPLE_FREQUENCY = 16000;
static const uint16_t SAMPLES_PER_CHUNK =
        FEATURES_STEP_SIZE * (AUDIO_SAMPLE_FREQUENCY / 1000);


static const uint16_t BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * 2;

#define FLOAT32_SCALE 0.0390625f

class MicroFrontend {
private:
    FrontendConfig frontend_config;
    FrontendState frontend_state;

public:
    MicroFrontend();

    FrontendOutput ProcessSamples(int16_t *samples, size_t *num_samples_read);
};

MicroFrontend::MicroFrontend() {
    this->frontend_config.window.size_ms = FEATURE_DURATION_MS;
    this->frontend_config.window.step_size_ms = FEATURES_STEP_SIZE;
    this->frontend_config.filterbank.num_channels = PREPROCESSOR_FEATURE_SIZE;
    this->frontend_config.filterbank.lower_band_limit = 125.0;
    this->frontend_config.filterbank.upper_band_limit = 7500.0;
    this->frontend_config.noise_reduction.smoothing_bits = 10;
    this->frontend_config.noise_reduction.even_smoothing = 0.025;
    this->frontend_config.noise_reduction.odd_smoothing = 0.06;
    this->frontend_config.noise_reduction.min_signal_remaining = 0.05;
    this->frontend_config.pcan_gain_control.enable_pcan = 1;
    this->frontend_config.pcan_gain_control.strength = 0.95;
    this->frontend_config.pcan_gain_control.offset = 80.0;
    this->frontend_config.pcan_gain_control.gain_bits = 21;
    this->frontend_config.log_scale.enable_log = 1;
    this->frontend_config.log_scale.scale_shift = 6;

    FrontendPopulateState(&(this->frontend_config), &(this->frontend_state),
                          AUDIO_SAMPLE_FREQUENCY);
}

FrontendOutput MicroFrontend::ProcessSamples(int16_t *samples, size_t *num_samples_read) {

    return FrontendProcessSamples(&(this->frontend_state), samples,
                                  SAMPLES_PER_CHUNK, num_samples_read);

    

    
    
    

    
    
    
    

    
}
extern "C"
{
static jclass processOutputClass;
static jmethodID processOutputConstructor;

JNIEXPORT void JNICALL
Java_com_example_microfeatures_MicroFrontend_00024Companion_initJni(
        JNIEnv *env,
        jobject thiz,
        jclass jProcessOutputClass
) {
    processOutputClass = reinterpret_cast<jclass>(env->NewGlobalRef(jProcessOutputClass));
    processOutputConstructor = env->GetMethodID(processOutputClass, "<init>", "([FI)V");
}

JNIEXPORT jlong JNICALL
Java_com_example_microfeatures_MicroFrontend_newNativeFrontend(JNIEnv *env, jobject thiz) {
#pragma clang diagnostic push
#pragma ide diagnostic ignored "MemoryLeak"
    auto *nativeFrontend = new MicroFrontend();
#pragma clang diagnostic pop
    return (jlong(nativeFrontend));
}

JNIEXPORT void JNICALL
Java_com_example_microfeatures_MicroFrontend_deleteNativeFrontend(
        JNIEnv *env,
        jobject thiz,
        jlong native_frontend
) {
    auto *nativeFrontend = (MicroFrontend *) native_frontend;
    delete nativeFrontend;
}

JNIEXPORT jobject JNICALL
Java_com_example_microfeatures_MicroFrontend_processSamples(
        JNIEnv *env,
        jobject thiz,
        jlong native_frontend,
        jobject audio
) {
    auto *nativeFrontend = (MicroFrontend *) native_frontend;
    auto *samples_ptr = reinterpret_cast<int16_t *>(env->GetDirectBufferAddress(audio));

    size_t num_samples_read;
    struct FrontendOutput frontend_output =
            nativeFrontend->ProcessSamples(samples_ptr, &num_samples_read);

    
    
    
    jfloatArray jFeatures = env->NewFloatArray((int)frontend_output.size);
    jfloat *jFeaturesPtr = env->GetFloatArrayElements(jFeatures, nullptr);
    if (jFeaturesPtr != nullptr) {
        for (std::size_t i = 0; i < frontend_output.size; ++i) {
            jFeaturesPtr[i] = (float)frontend_output.values[i] * FLOAT32_SCALE;
        }
        env->ReleaseFloatArrayElements(jFeatures, jFeaturesPtr, 0);
    }

    jobject processOutputObject = env->NewObject(
            processOutputClass,
            processOutputConstructor,
            jFeatures,
            static_cast<jint>(num_samples_read)
    );
    return processOutputObject;
}
}