
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_UTIL_H_

#include <stdint.h>
#include <stdlib.h>

#include "tensorflow/lite/experimental/microfrontend/lib/log_scale.h"

#ifdef __cplusplus
extern "C" {
#endif

struct LogScaleConfig {
  
  int enable_log;
  
  int scale_shift;
};


void LogScaleFillConfigWithDefaults(struct LogScaleConfig* config);


int LogScalePopulateState(const struct LogScaleConfig* config,
                          struct LogScaleState* state);

#ifdef __cplusplus
}  
#endif

#endif  
