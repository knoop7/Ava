
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_IO_H_

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/fft.h"

#ifdef __cplusplus
extern "C" {
#endif

void FftWriteMemmapPreamble(FILE* fp, const struct FftState* state);
void FftWriteMemmap(FILE* fp, const struct FftState* state,
                    const char* variable);

#ifdef __cplusplus
}  
#endif

#endif  
