
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_H_

#define kNoiseReductionBits 14

#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

struct NoiseReductionState {
  int smoothing_bits;
  uint16_t even_smoothing;
  uint16_t odd_smoothing;
  uint16_t min_signal_remaining;
  int num_channels;
  uint32_t* estimate;
};



void NoiseReductionApply(struct NoiseReductionState* state, uint32_t* signal);

void NoiseReductionReset(struct NoiseReductionState* state);

#ifdef __cplusplus
}  
#endif

#endif  
