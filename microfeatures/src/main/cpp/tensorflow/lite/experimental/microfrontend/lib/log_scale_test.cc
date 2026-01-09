
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale.h"

#include "tensorflow/lite/experimental/microfrontend/lib/log_scale_util.h"
#include "tensorflow/lite/micro/testing/micro_test.h"

namespace {

const int kScaleShift = 6;
const int kCorrectionBits = -1;

}  

TF_LITE_MICRO_TESTS_BEGIN

TF_LITE_MICRO_TEST(LogScaleTest_CheckOutputValues) {
  struct LogScaleState state;
  state.enable_log = true;
  state.scale_shift = kScaleShift;

  uint32_t fake_signal[] = {3578, 1533};
  uint16_t* output = LogScaleApply(&state, fake_signal,
                                   sizeof(fake_signal) / sizeof(fake_signal[0]),
                                   kCorrectionBits);

  const uint16_t expected[] = {479, 425};
  for (size_t i = 0; i < sizeof(expected) / sizeof(expected[0]); ++i) {
    TF_LITE_MICRO_EXPECT_EQ(output[i], expected[i]);
  }
}

TF_LITE_MICRO_TEST(LogScaleTest_CheckOutputValuesNoLog) {
  struct LogScaleState state;
  state.enable_log = false;
  state.scale_shift = kScaleShift;

  uint32_t fake_signal[] = {85964, 45998};
  uint16_t* output = LogScaleApply(&state, fake_signal,
                                   sizeof(fake_signal) / sizeof(fake_signal[0]),
                                   kCorrectionBits);

  const uint16_t expected[] = {65535, 45998};
  for (size_t i = 0; i < sizeof(expected) / sizeof(expected[0]); ++i) {
    TF_LITE_MICRO_EXPECT_EQ(output[i], expected[i]);
  }
}

TF_LITE_MICRO_TESTS_END
