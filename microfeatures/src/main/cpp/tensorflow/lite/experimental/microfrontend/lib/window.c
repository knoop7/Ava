
#include "tensorflow/lite/experimental/microfrontend/lib/window.h"

#include <string.h>

int WindowProcessSamples(struct WindowState* state, const int16_t* samples,
                         size_t num_samples, size_t* num_samples_read) {
  const int size = state->size;

  
  size_t max_samples_to_copy = state->size - state->input_used;
  if (max_samples_to_copy > num_samples) {
    max_samples_to_copy = num_samples;
  }
  memcpy(state->input + state->input_used, samples,
         max_samples_to_copy * sizeof(*samples));
  *num_samples_read = max_samples_to_copy;
  state->input_used += max_samples_to_copy;

  if (state->input_used < state->size) {
    
    return 0;
  }

  
  const int16_t* coefficients = state->coefficients;
  const int16_t* input = state->input;
  int16_t* output = state->output;
  int i;
  int16_t max_abs_output_value = 0;
  for (i = 0; i < size; ++i) {
    int16_t new_value =
        (((int32_t)*input++) * *coefficients++) >> kFrontendWindowBits;
    *output++ = new_value;
    if (new_value < 0) {
      new_value = -new_value;
    }
    if (new_value > max_abs_output_value) {
      max_abs_output_value = new_value;
    }
  }
  
  memmove(state->input, state->input + state->step,
          sizeof(*state->input) * (state->size - state->step));
  state->input_used -= state->step;
  state->max_abs_output_value = max_abs_output_value;

  
  return 1;
}

void WindowReset(struct WindowState* state) {
  memset(state->input, 0, state->size * sizeof(*state->input));
  memset(state->output, 0, state->size * sizeof(*state->output));
  state->input_used = 0;
  state->max_abs_output_value = 0;
}
