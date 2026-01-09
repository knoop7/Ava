
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction_io.h"

void NoiseReductionWriteMemmapPreamble(
    FILE* fp, const struct NoiseReductionState* state) {
  fprintf(fp, "static uint32_t noise_reduction_estimate[%d];\n",
          state->num_channels);
  fprintf(fp, "\n");
}

void NoiseReductionWriteMemmap(FILE* fp,
                               const struct NoiseReductionState* state,
                               const char* variable) {
  fprintf(fp, "%s->even_smoothing = %d;\n", variable, state->even_smoothing);
  fprintf(fp, "%s->odd_smoothing = %d;\n", variable, state->odd_smoothing);
  fprintf(fp, "%s->min_signal_remaining = %d;\n", variable,
          state->min_signal_remaining);
  fprintf(fp, "%s->num_channels = %d;\n", variable, state->num_channels);

  fprintf(fp, "%s->estimate = noise_reduction_estimate;\n", variable);
}
