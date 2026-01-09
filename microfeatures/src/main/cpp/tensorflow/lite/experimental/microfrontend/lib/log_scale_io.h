
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_IO_H_

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/log_scale.h"

#ifdef __cplusplus
extern "C" {
#endif

void LogScaleWriteMemmap(FILE* fp, const struct LogScaleState* state,
                         const char* variable);

#ifdef __cplusplus
}  
#endif

#endif  
