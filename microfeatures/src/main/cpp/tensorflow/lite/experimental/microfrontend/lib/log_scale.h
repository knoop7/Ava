
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_SCALE_H_

#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

struct LogScaleState {
  int enable_log;
  int scale_shift;
};



uint16_t* LogScaleApply(struct LogScaleState* state, uint32_t* signal,
                        int signal_size, int correction_bits);

#ifdef __cplusplus
}  
#endif

#endif  
