
#include "tensorflow/lite/experimental/microfrontend/lib/fft.h"

#include <string.h>

#include "tensorflow/lite/experimental/microfrontend/lib/kiss_fft_int16.h"

void FftCompute(struct FftState* state, const int16_t* input,
                int input_scale_shift) {
  const size_t input_size = state->input_size;
  const size_t fft_size = state->fft_size;

  int16_t* fft_input = state->input;
  
  size_t i;
  for (i = 0; i < input_size; ++i) {
    fft_input[i] = static_cast<int16_t>(static_cast<uint16_t>(input[i])
                                        << input_scale_shift);
  }
  
  for (; i < fft_size; ++i) {
    fft_input[i] = 0;
  }

  
  kissfft_fixed16::kiss_fftr(
      reinterpret_cast<kissfft_fixed16::kiss_fftr_cfg>(state->scratch),
      state->input,
      reinterpret_cast<kissfft_fixed16::kiss_fft_cpx*>(state->output));
}

void FftInit(struct FftState* state) {
  
}

void FftReset(struct FftState* state) {
  memset(state->input, 0, state->fft_size * sizeof(*state->input));
  memset(state->output, 0, (state->fft_size / 2 + 1) * sizeof(*state->output));
}
