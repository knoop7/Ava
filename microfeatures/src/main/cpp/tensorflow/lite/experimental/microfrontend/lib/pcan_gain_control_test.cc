
#include "tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control.h"

#include "tensorflow/lite/experimental/microfrontend/lib/pcan_gain_control_util.h"
#include "tensorflow/lite/micro/testing/micro_test.h"

namespace {

const int kNumChannels = 2;
const int kSmoothingBits = 10;
const int kCorrectionBits = -1;


class PcanGainControlTestConfig {
 public:
  PcanGainControlTestConfig() {
    config_.enable_pcan = 1;
    config_.strength = 0.95;
    config_.offset = 80.0;
    config_.gain_bits = 21;
  }

  struct PcanGainControlConfig config_;
};

}  

TF_LITE_MICRO_TESTS_BEGIN

TF_LITE_MICRO_TEST(PcanGainControlTest_TestPcanGainControl) {
  uint32_t estimate[] = {6321887, 31248341};
  PcanGainControlTestConfig config;
  struct PcanGainControlState state;
  TF_LITE_MICRO_EXPECT(PcanGainControlPopulateState(
      &config.config_, &state, estimate, kNumChannels, kSmoothingBits,
      kCorrectionBits));

  uint32_t signal[] = {241137, 478104};
  PcanGainControlApply(&state, signal);

  const uint32_t expected[] = {3578, 1533};
  TF_LITE_MICRO_EXPECT_EQ(state.num_channels,
                          static_cast<int>(sizeof(expected) / sizeof(expected[0])));
  int i;
  for (i = 0; i < state.num_channels; ++i) {
    TF_LITE_MICRO_EXPECT_EQ(signal[i], expected[i]);
  }

  PcanGainControlFreeStateContents(&state);
}

TF_LITE_MICRO_TESTS_END
