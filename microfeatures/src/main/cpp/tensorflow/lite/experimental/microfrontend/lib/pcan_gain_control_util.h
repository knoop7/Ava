
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_PCAN_GAIN_CONTROL_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_PCAN_GAIN_CONTROL_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control.h"

#define kWideDynamicFunctionBits 32
#define kWideDynamicFunctionLUTSize (4 * kWideDynamicFunctionBits - 3)

#ifdef __cplusplus
extern "C" {
#endif

struct PcanGainControlConfig {
  
  int enable_pcan;
  
  float strength;
  
  float offset;
  
  int gain_bits;
};

void PcanGainControlFillConfigWithDefaults(
    struct PcanGainControlConfig* config);

int16_t PcanGainLookupFunction(const struct PcanGainControlConfig* config,
                               int32_t input_bits, uint32_t x);

int PcanGainControlPopulateState(const struct PcanGainControlConfig* config,
                                 struct PcanGainControlState* state,
                                 uint32_t* noise_estimate,
                                 const int num_channels,
                                 const uint16_t smoothing_bits,
                                 const int32_t input_correction_bits);

void PcanGainControlFreeStateContents(struct PcanGainControlState* state);

#ifdef __cplusplus
}  
#endif

#endif  
