
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_WINDOW_H_

#include <stdint.h>
#include <stdlib.h>

#define kFrontendWindowBits 12

#ifdef __cplusplus
extern "C" {
#endif

struct WindowState {
  size_t size;
  int16_t* coefficients;
  size_t step;

  int16_t* input;
  size_t input_used;
  int16_t* output;
  int16_t max_abs_output_value;
};



int WindowProcessSamples(struct WindowState* state, const int16_t* samples,
                         size_t num_samples, size_t* num_samples_read);

void WindowReset(struct WindowState* state);

#ifdef __cplusplus
}  
#endif

#endif  
