
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale_io.h"

void LogScaleWriteMemmap(FILE* fp, const struct LogScaleState* state,
                         const char* variable) {
  fprintf(fp, "%s->enable_log = %d;\n", variable, state->enable_log);
  fprintf(fp, "%s->scale_shift = %d;\n", variable, state->scale_shift);
}
