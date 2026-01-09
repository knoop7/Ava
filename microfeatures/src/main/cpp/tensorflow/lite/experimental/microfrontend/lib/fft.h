
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_FFT_H_

#include <stdint.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

struct complex_int16_t {
  int16_t real;
  int16_t imag;
};

struct FftState {
  int16_t* input;
  struct complex_int16_t* output;
  size_t fft_size;
  size_t input_size;
  void* scratch;
  size_t scratch_size;
};

void FftCompute(struct FftState* state, const int16_t* input,
                int input_scale_shift);

void FftInit(struct FftState* state);

void FftReset(struct FftState* state);

#ifdef __cplusplus
}  
#endif

#endif  
