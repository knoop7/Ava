
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/fft_util.h"
#include "tensorflow/lite/experimental/microfrontend/lib/filterbank_util.h"
#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale_util.h"
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction_util.h"
#include "tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control_util.h"
#include "tensorflow/lite/experimental/microfrontend/lib/window_util.h"

#ifdef __cplusplus
extern "C" {
#endif

struct FrontendConfig {
  struct WindowConfig window;
  struct FilterbankConfig filterbank;
  struct NoiseReductionConfig noise_reduction;
  struct PcanGainControlConfig pcan_gain_control;
  struct LogScaleConfig log_scale;
};


void FrontendFillConfigWithDefaults(struct FrontendConfig* config);


int FrontendPopulateState(const struct FrontendConfig* config,
                          struct FrontendState* state, int sample_rate);


void FrontendFreeStateContents(struct FrontendState* state);

#ifdef __cplusplus
}  
#endif

#endif  
