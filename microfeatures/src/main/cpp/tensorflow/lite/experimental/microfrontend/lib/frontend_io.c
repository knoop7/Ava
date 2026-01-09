
#include "tensorflow/lite/experimental/microfrontend/lib/frontend_io.h"

#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/fft_io.h"
#include "tensorflow/lite/experimental/microfrontend/lib/filterbank_io.h"
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale_io.h"
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction_io.h"
#include "tensorflow/lite/experimental/microfrontend/lib/window_io.h"

int WriteFrontendStateMemmap(const char* header, const char* source,
                             const struct FrontendState* state) {
  
  FILE* fp = fopen(header, "w");
  if (!fp) {
    fprintf(stderr, "Failed to open header '%s' for write\n", header);
    return 0;
  }
  fprintf(fp, "#ifndef FRONTEND_STATE_MEMMAP_H_\n");
  fprintf(fp, "#define FRONTEND_STATE_MEMMAP_H_\n");
  fprintf(fp, "\n");
  fprintf(fp, "#include \"frontend.h\"\n");
  fprintf(fp, "\n");
  fprintf(fp, "struct FrontendState* GetFrontendStateMemmap();\n");
  fprintf(fp, "\n");
  fprintf(fp, "#endif  // FRONTEND_STATE_MEMMAP_H_\n");
  fclose(fp);

  
  fp = fopen(source, "w");
  if (!fp) {
    fprintf(stderr, "Failed to open source '%s' for write\n", source);
    return 0;
  }
  fprintf(fp, "#include \"%s\"\n", header);
  fprintf(fp, "\n");
  WindowWriteMemmapPreamble(fp, &state->window);
  FftWriteMemmapPreamble(fp, &state->fft);
  FilterbankWriteMemmapPreamble(fp, &state->filterbank);
  NoiseReductionWriteMemmapPreamble(fp, &state->noise_reduction);
  fprintf(fp, "static struct FrontendState state;\n");
  fprintf(fp, "struct FrontendState* GetFrontendStateMemmap() {\n");
  WindowWriteMemmap(fp, &state->window, "  (&state.window)");
  FftWriteMemmap(fp, &state->fft, "  (&state.fft)");
  FilterbankWriteMemmap(fp, &state->filterbank, "  (&state.filterbank)");
  NoiseReductionWriteMemmap(fp, &state->noise_reduction,
                            "  (&state.noise_reduction)");
  LogScaleWriteMemmap(fp, &state->log_scale, "  (&state.log_scale)");
  fprintf(fp, "  FftInit(&state.fft);\n");
  fprintf(fp, "  FrontendReset(&state);\n");
  fprintf(fp, "  return &state;\n");
  fprintf(fp, "}\n");
  fclose(fp);
  return 1;
}
