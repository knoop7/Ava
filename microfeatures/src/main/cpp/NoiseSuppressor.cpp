#include <jni.h>
#include "webrtc_ns/noise_suppression.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeCreate(JNIEnv *env, jobject thiz) {
    NsHandle *handle = WebRtcNs_Create();
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT jint JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeInit(JNIEnv *env, jobject thiz,
                                                          jlong handle, jint sampleRate) {
    NsHandle *ns = reinterpret_cast<NsHandle *>(handle);
    return WebRtcNs_Init(ns, static_cast<uint32_t>(sampleRate));
}

JNIEXPORT jint JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeSetPolicy(JNIEnv *env, jobject thiz,
                                                               jlong handle, jint mode) {
    NsHandle *ns = reinterpret_cast<NsHandle *>(handle);
    return WebRtcNs_set_policy(ns, mode);
}

JNIEXPORT void JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeProcess(JNIEnv *env, jobject thiz,
                                                             jlong handle,
                                                             jshortArray inputArray,
                                                             jshortArray outputArray) {
    NsHandle *ns = reinterpret_cast<NsHandle *>(handle);
    
    jshort *input = env->GetShortArrayElements(inputArray, nullptr);
    jshort *output = env->GetShortArrayElements(outputArray, nullptr);
    
    const int16_t *inFrame[1] = {input};
    int16_t *outFrame[1] = {output};
    
    WebRtcNs_Analyze(ns, input);
    WebRtcNs_Process(ns, inFrame, 1, outFrame);
    
    env->ReleaseShortArrayElements(inputArray, input, JNI_ABORT);
    env->ReleaseShortArrayElements(outputArray, output, 0);
}

JNIEXPORT void JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeDestroy(JNIEnv *env, jobject thiz,
                                                             jlong handle) {
    NsHandle *ns = reinterpret_cast<NsHandle *>(handle);
    WebRtcNs_Free(ns);
}

JNIEXPORT jfloat JNICALL
Java_com_example_microfeatures_NoiseSuppressor_nativeGetSpeechProbability(JNIEnv *env, jobject thiz,
                                                                          jlong handle) {
    NsHandle *ns = reinterpret_cast<NsHandle *>(handle);
    return WebRtcNs_prior_speech_probability(ns);
}

}
