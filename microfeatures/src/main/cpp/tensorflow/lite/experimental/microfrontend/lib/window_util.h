
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_UTIL_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_UTIL_H_

#include "tensorflow/lite/experimental/microfrontend/lib/window.h"

#ifdef __cplusplus
extern "C" {
#endif

struct WindowConfig {
  
  size_t size_ms;
  
  size_t step_size_ms;
};


void WindowFillConfigWithDefaults(struct WindowConfig* config);


int WindowPopulateState(const struct WindowConfig* config,
                        struct WindowState* state, int sample_rate);


void WindowFreeStateContents(struct WindowState* state);

#ifdef __cplusplus
}  
#endif

#endif  
