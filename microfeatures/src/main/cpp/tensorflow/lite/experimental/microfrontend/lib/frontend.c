
#include "tensorflow/lite/experimental/microfrontend/lib/frontend.h"

#include "tensorflow/lite/experimental/microfrontend/lib/bits.h"

struct FrontendOutput FrontendProcessSamples(struct FrontendState* state,
                                             const int16_t* samples,
                                             size_t num_samples,
                                             size_t* num_samples_read) {
  struct FrontendOutput output;
  output.values = NULL;
  output.size = 0;

  
  if (!WindowProcessSamples(&state->window, samples, num_samples,
                            num_samples_read)) {
    return output;
  }

  
  
  int input_shift =
      15 - MostSignificantBit32(state->window.max_abs_output_value);
  FftCompute(&state->fft, state->window.output, input_shift);

  
  int32_t* energy = (int32_t*)state->fft.output;

  FilterbankConvertFftComplexToEnergy(&state->filterbank, state->fft.output,
                                      energy);

  FilterbankAccumulateChannels(&state->filterbank, energy);
  uint32_t* scaled_filterbank = FilterbankSqrt(&state->filterbank, input_shift);

  NoiseReductionApply(&state->noise_reduction, scaled_filterbank);

  if (state->pcan_gain_control.enable_pcan) {
    PcanGainControlApply(&state->pcan_gain_control, scaled_filterbank);
  }

  
  int correction_bits =
      MostSignificantBit32(state->fft.fft_size) - 1 - (kFilterbankBits / 2);
  uint16_t* logged_filterbank =
      LogScaleApply(&state->log_scale, scaled_filterbank,
                    state->filterbank.num_channels, correction_bits);

  output.size = state->filterbank.num_channels;
  output.values = logged_filterbank;
  return output;
}

void FrontendReset(struct FrontendState* state) {
  WindowReset(&state->window);
  FftReset(&state->fft);
  FilterbankReset(&state->filterbank);
  NoiseReductionReset(&state->noise_reduction);
}
