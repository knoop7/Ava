
#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction.h"

#include "tensorflow/lite/experimental/microfrontend/lib/noise_reduction_util.h"
#include "tensorflow/lite/micro/testing/micro_test.h"

namespace {

const int kNumChannels = 2;


class NoiseReductionTestConfig {
 public:
  NoiseReductionTestConfig() {
    config_.smoothing_bits = 10;
    config_.even_smoothing = 0.025;
    config_.odd_smoothing = 0.06;
    config_.min_signal_remaining = 0.05;
  }

  struct NoiseReductionConfig config_;
};

}  

TF_LITE_MICRO_TESTS_BEGIN

TF_LITE_MICRO_TEST(NoiseReductionTest_TestNoiseReductionEstimate) {
  NoiseReductionTestConfig config;
  struct NoiseReductionState state;
  TF_LITE_MICRO_EXPECT(
      NoiseReductionPopulateState(&config.config_, &state, kNumChannels));

  uint32_t signal[] = {247311, 508620};
  NoiseReductionApply(&state, signal);

  const uint32_t expected[] = {6321887, 31248341};
  TF_LITE_MICRO_EXPECT_EQ(state.num_channels,
                          static_cast<int>(sizeof(expected) / sizeof(expected[0])));
  int i;
  for (i = 0; i < state.num_channels; ++i) {
    TF_LITE_MICRO_EXPECT_EQ(state.estimate[i], expected[i]);
  }

  NoiseReductionFreeStateContents(&state);
}

TF_LITE_MICRO_TEST(NoiseReductionTest_TestNoiseReduction) {
  NoiseReductionTestConfig config;
  struct NoiseReductionState state;
  TF_LITE_MICRO_EXPECT(
      NoiseReductionPopulateState(&config.config_, &state, kNumChannels));

  uint32_t signal[] = {247311, 508620};
  NoiseReductionApply(&state, signal);

  const uint32_t expected[] = {241137, 478104};
  TF_LITE_MICRO_EXPECT_EQ(state.num_channels,
                          static_cast<int>(sizeof(expected) / sizeof(expected[0])));
  int i;
  for (i = 0; i < state.num_channels; ++i) {
    TF_LITE_MICRO_EXPECT_EQ(signal[i], expected[i]);
  }

  NoiseReductionFreeStateContents(&state);
}

TF_LITE_MICRO_TESTS_END
