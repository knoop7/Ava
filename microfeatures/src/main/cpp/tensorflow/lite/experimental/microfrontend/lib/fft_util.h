
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/fft.h"

#ifdef __cplusplus
extern "C" {
#endif


int FftPopulateState(struct FftState* state, size_t input_size);


void FftFreeStateContents(struct FftState* state);

#ifdef __cplusplus
}  
#endif

#endif  
