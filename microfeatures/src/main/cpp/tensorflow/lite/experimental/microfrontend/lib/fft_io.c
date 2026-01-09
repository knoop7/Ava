
#include "tensorflow/lite/experimental/microfrontend/lib/fft_io.h"

void FftWriteMemmapPreamble(FILE* fp, const struct FftState* state) {
  fprintf(fp, "static int16_t fft_input[%zu];\n", state->fft_size);
  fprintf(fp, "static struct complex_int16_t fft_output[%zu];\n",
          state->fft_size / 2 + 1);
  fprintf(fp, "static char fft_scratch[%zu];\n", state->scratch_size);
  fprintf(fp, "\n");
}

void FftWriteMemmap(FILE* fp, const struct FftState* state,
                    const char* variable) {
  fprintf(fp, "%s->input = fft_input;\n", variable);
  fprintf(fp, "%s->output = fft_output;\n", variable);
  fprintf(fp, "%s->fft_size = %zu;\n", variable, state->fft_size);
  fprintf(fp, "%s->input_size = %zu;\n", variable, state->input_size);
  fprintf(fp, "%s->scratch = fft_scratch;\n", variable);
  fprintf(fp, "%s->scratch_size = %zu;\n", variable, state->scratch_size);
}
