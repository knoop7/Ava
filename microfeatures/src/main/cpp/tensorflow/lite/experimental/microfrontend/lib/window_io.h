
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_IO_H_

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/window.h"

#ifdef __cplusplus
extern "C" {
#endif

void WindowWriteMemmapPreamble(FILE* fp, const struct WindowState* state);
void WindowWriteMemmap(FILE* fp, const struct WindowState* state,
                       const char* variable);

#ifdef __cplusplus
}  
#endif

#endif  
