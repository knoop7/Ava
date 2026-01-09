
#include <stdio.h>

#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"
#include "tensorflow/lite/experimental/microfrontend/lib/frontend_io.h"
#include "tensorflow/lite/experimental/microfrontend/lib/frontend_util.h"

int main(int argc, char** argv) {
  if (argc != 3) {
    fprintf(stderr,
            "%s requires exactly two parameters - the names of the header and "
            "source files to save\n",
            argv[0]);
    return 1;
  }
  struct FrontendConfig frontend_config;
  FrontendFillConfigWithDefaults(&frontend_config);

  int sample_rate = 16000;
  struct FrontendState frontend_state;
  if (!FrontendPopulateState(&frontend_config, &frontend_state, sample_rate)) {
    fprintf(stderr, "Failed to populate frontend state\n");
    FrontendFreeStateContents(&frontend_state);
    return 1;
  }

  if (!WriteFrontendStateMemmap(argv[1], argv[2], &frontend_state)) {
    fprintf(stderr, "Failed to write memmap\n");
    FrontendFreeStateContents(&frontend_state);
    return 1;
  }

  FrontendFreeStateContents(&frontend_state);
  return 0;
}
