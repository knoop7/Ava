
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale_util.h"

void LogScaleFillConfigWithDefaults(struct LogScaleConfig* config) {
  config->enable_log = 1;
  config->scale_shift = 6;
}

int LogScalePopulateState(const struct LogScaleConfig* config,
                          struct LogScaleState* state) {
  state->enable_log = config->enable_log;
  state->scale_shift = config->scale_shift;
  return 1;
}
