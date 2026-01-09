
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_PCAN_GAIN_CONTROL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_PCAN_GAIN_CONTROL_H_

#include <stdint.h>
#include <stdlib.h>

#define kPcanSnrBits 12
#define kPcanOutputBits 6

#ifdef __cplusplus
extern "C" {
#endif


struct PcanGainControlState {
  int enable_pcan;
  uint32_t* noise_estimate;
  int num_channels;
  int16_t* gain_lut;
  int32_t snr_shift;
};

int16_t WideDynamicFunction(const uint32_t x, const int16_t* lut);

uint32_t PcanShrink(const uint32_t x);

void PcanGainControlApply(struct PcanGainControlState* state, uint32_t* signal);

#ifdef __cplusplus
}  
#endif

#endif  
