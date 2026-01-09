
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_NOISE_REDUCTION_IO_H_

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction.h"

#ifdef __cplusplus
extern "C" {
#endif

void NoiseReductionWriteMemmapPreamble(FILE* fp,
                                       const struct NoiseReductionState* state);
void NoiseReductionWriteMemmap(FILE* fp,
                               const struct NoiseReductionState* state,
                               const char* variable);

#ifdef __cplusplus
}  
#endif

#endif  
