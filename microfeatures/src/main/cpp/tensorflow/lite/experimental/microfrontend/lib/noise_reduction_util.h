
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction.h"

#ifdef __cplusplus
extern "C" {
#endif

struct NoiseReductionConfig {
  
  int smoothing_bits;
  
  float even_smoothing;
  
  float odd_smoothing;
  
  float min_signal_remaining;
};


void NoiseReductionFillConfigWithDefaults(struct NoiseReductionConfig* config);


int NoiseReductionPopulateState(const struct NoiseReductionConfig* config,
                                struct NoiseReductionState* state,
                                int num_channels);


void NoiseReductionFreeStateContents(struct NoiseReductionState* state);

#ifdef __cplusplus
}  
#endif

#endif  
