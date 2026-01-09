
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_H_

#include <stdint.h>
#include <stdlib.h>

#include "tensorflow/lite/experimental/microfrontend/lib/fft.h"
#include "tensorflow/lite/experimental/microfrontend/lib/filterbank.h"
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale.h"
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction.h"
#include "tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control.h"
#include "tensorflow/lite/experimental/microfrontend/lib/window.h"

#ifdef __cplusplus
extern "C" {
#endif

struct FrontendState {
  struct WindowState window;
  struct FftState fft;
  struct FilterbankState filterbank;
  struct NoiseReductionState noise_reduction;
  struct PcanGainControlState pcan_gain_control;
  struct LogScaleState log_scale;
};

struct FrontendOutput {
  const uint16_t* values;
  size_t size;
};








struct FrontendOutput FrontendProcessSamples(struct FrontendState* state,
                                             const int16_t* samples,
                                             size_t num_samples,
                                             size_t* num_samples_read);

void FrontendReset(struct FrontendState* state);

#ifdef __cplusplus
}  
#endif

#endif  
