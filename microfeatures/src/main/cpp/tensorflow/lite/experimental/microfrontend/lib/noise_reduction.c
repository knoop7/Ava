
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction.h"

#include <string.h>

void NoiseReductionApply(struct NoiseReductionState* state, uint32_t* signal) {
  int i;
  for (i = 0; i < state->num_channels; ++i) {
    const uint32_t smoothing =
        ((i & 1) == 0) ? state->even_smoothing : state->odd_smoothing;
    const uint32_t one_minus_smoothing = (1 << kNoiseReductionBits) - smoothing;

    
    const uint32_t signal_scaled_up = signal[i] << state->smoothing_bits;
    uint32_t estimate =
        (((uint64_t)signal_scaled_up * smoothing) +
         ((uint64_t)state->estimate[i] * one_minus_smoothing)) >>
        kNoiseReductionBits;
    state->estimate[i] = estimate;

    
    if (estimate > signal_scaled_up) {
      estimate = signal_scaled_up;
    }

    const uint32_t floor =
        ((uint64_t)signal[i] * state->min_signal_remaining) >>
        kNoiseReductionBits;
    const uint32_t subtracted =
        (signal_scaled_up - estimate) >> state->smoothing_bits;
    const uint32_t output = subtracted > floor ? subtracted : floor;
    signal[i] = output;
  }
}

void NoiseReductionReset(struct NoiseReductionState* state) {
  memset(state->estimate, 0, sizeof(*state->estimate) * state->num_channels);
}
