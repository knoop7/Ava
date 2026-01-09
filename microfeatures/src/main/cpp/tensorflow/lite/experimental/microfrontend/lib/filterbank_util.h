
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/filterbank.h"

#ifdef __cplusplus
extern "C" {
#endif

struct FilterbankConfig {
  
  int num_channels;
  
  float upper_band_limit;
  
  float lower_band_limit;
  
  int output_scale_shift;
};


void FilterbankFillConfigWithDefaults(struct FilterbankConfig* config);


int FilterbankPopulateState(const struct FilterbankConfig* config,
                            struct FilterbankState* state, int sample_rate,
                            int spectrum_size);


void FilterbankFreeStateContents(struct FilterbankState* state);

#ifdef __cplusplus
}  
#endif

#endif  
