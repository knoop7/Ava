
#include "tensorflow/lite/experimental/microfrontend/lib/window_util.h"

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>


#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

void WindowFillConfigWithDefaults(struct WindowConfig* config) {
  config->size_ms = 25;
  config->step_size_ms = 10;
}

int WindowPopulateState(const struct WindowConfig* config,
                        struct WindowState* state, int sample_rate) {
  state->size = config->size_ms * sample_rate / 1000;
  state->step = config->step_size_ms * sample_rate / 1000;

  state->coefficients = malloc(state->size * sizeof(*state->coefficients));
  if (state->coefficients == NULL) {
    fprintf(stderr, "Failed to allocate window coefficients\n");
    return 0;
  }

  
  const float arg = (float)M_PI * 2.0f / ((float)state->size);
  size_t i;
  for (i = 0; i < state->size; ++i) {
    float float_value = 0.5f - (0.5f * cosf(arg * (i + 0.5f)));
    
    state->coefficients[i] =
        floorf(float_value * (1 << kFrontendWindowBits) + 0.5f);
  }

  state->input_used = 0;
  state->input = malloc(state->size * sizeof(*state->input));
  if (state->input == NULL) {
    fprintf(stderr, "Failed to allocate window input\n");
    return 0;
  }

  state->output = malloc(state->size * sizeof(*state->output));
  if (state->output == NULL) {
    fprintf(stderr, "Failed to allocate window output\n");
    return 0;
  }

  return 1;
}

void WindowFreeStateContents(struct WindowState* state) {
  free(state->coefficients);
  free(state->input);
  free(state->output);
}
