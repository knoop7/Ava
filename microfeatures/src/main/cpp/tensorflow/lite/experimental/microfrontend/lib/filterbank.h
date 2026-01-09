
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_H_

#include <stdint.h>
#include <stdlib.h>

#include "tensorflow/lite/experimental/microfrontend/lib/fft.h"

#define kFilterbankBits 12

#ifdef __cplusplus
extern "C" {
#endif

struct FilterbankState {
  int num_channels;
  int start_index;
  int end_index;
  int16_t* channel_frequency_starts;
  int16_t* channel_weight_starts;
  int16_t* channel_widths;
  int16_t* weights;
  int16_t* unweights;
  uint64_t* work;
};



void FilterbankConvertFftComplexToEnergy(struct FilterbankState* state,
                                         struct complex_int16_t* fft_output,
                                         int32_t* energy);



void FilterbankAccumulateChannels(struct FilterbankState* state,
                                  const int32_t* energy);




uint32_t* FilterbankSqrt(struct FilterbankState* state, int scale_down_shift);

void FilterbankReset(struct FilterbankState* state);

#ifdef __cplusplus
}  
#endif

#endif  
