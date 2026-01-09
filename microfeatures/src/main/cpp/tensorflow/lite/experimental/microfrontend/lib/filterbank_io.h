
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FILTERBANK_IO_H_

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/filterbank.h"

#ifdef __cplusplus
extern "C" {
#endif

void FilterbankWriteMemmapPreamble(FILE* fp,
                                   const struct FilterbankState* state);
void FilterbankWriteMemmap(FILE* fp, const struct FilterbankState* state,
                           const char* variable);

#ifdef __cplusplus
}  
#endif

#endif  
