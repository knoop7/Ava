
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_IO_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FRONTEND_IO_H_

#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"

#ifdef __cplusplus
extern "C" {
#endif

int WriteFrontendStateMemmap(const char* header, const char* source,
                             const struct FrontendState* state);

#ifdef __cplusplus
}  
#endif

#endif  
