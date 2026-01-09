
#include "tensorflow/lite/experimental/microfrontend/lib/fft_util.h"

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/kiss_fft_int16.h"

int FftPopulateState(struct FftState* state, size_t input_size) {
  state->input_size = input_size;
  state->fft_size = 1;
  while (state->fft_size < state->input_size) {
    state->fft_size <<= 1;
  }

  state->input = reinterpret_cast<int16_t*>(
      malloc(state->fft_size * sizeof(*state->input)));
  if (state->input == nullptr) {
    fprintf(stderr, "Failed to alloc fft input buffer\n");
    return 0;
  }

  state->output = reinterpret_cast<complex_int16_t*>(
      malloc((state->fft_size / 2 + 1) * sizeof(*state->output) * 2));
  if (state->output == nullptr) {
    fprintf(stderr, "Failed to alloc fft output buffer\n");
    return 0;
  }

  
  size_t scratch_size = 0;
  kissfft_fixed16::kiss_fftr_cfg kfft_cfg = kissfft_fixed16::kiss_fftr_alloc(
      state->fft_size, 0, nullptr, &scratch_size);
  if (kfft_cfg != nullptr) {
    fprintf(stderr, "Kiss memory sizing failed.\n");
    return 0;
  }
  state->scratch = malloc(scratch_size);
  if (state->scratch == nullptr) {
    fprintf(stderr, "Failed to alloc fft scratch buffer\n");
    return 0;
  }
  state->scratch_size = scratch_size;
  
  kfft_cfg = kissfft_fixed16::kiss_fftr_alloc(state->fft_size, 0,
                                              state->scratch, &scratch_size);
  if (kfft_cfg != state->scratch) {
    fprintf(stderr, "Kiss memory preallocation strategy failed.\n");
    return 0;
  }
  return 1;
}

void FftFreeStateContents(struct FftState* state) {
  free(state->input);
  free(state->output);
  free(state->scratch);
}
