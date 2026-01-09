
#include "tensorflow/lite/experimental/microfrontend/lib/log_scale.h"

#include "tensorflow/lite/experimental/microfrontend/lib/bits.h"
#include "tensorflow/lite/experimental/microfrontend/lib/log_lut.h"

#define kuint16max 0x0000FFFF








static uint32_t Log2FractionPart(const uint32_t x, const uint32_t log2x) {
  
  int32_t frac = x - (1LL << log2x);
  if (log2x < kLogScaleLog2) {
    frac <<= kLogScaleLog2 - log2x;
  } else {
    frac >>= log2x - kLogScaleLog2;
  }
  
  const uint32_t base_seg = frac >> (kLogScaleLog2 - kLogSegmentsLog2);
  const uint32_t seg_unit =
      (((uint32_t)1) << kLogScaleLog2) >> kLogSegmentsLog2;

  const int32_t c0 = kLogLut[base_seg];
  const int32_t c1 = kLogLut[base_seg + 1];
  const int32_t seg_base = seg_unit * base_seg;
  const int32_t rel_pos = ((c1 - c0) * (frac - seg_base)) >> kLogScaleLog2;
  return frac + c0 + rel_pos;
}

static uint32_t Log(const uint32_t x, const uint32_t scale_shift) {
  const uint32_t integer = MostSignificantBit32(x) - 1;
  const uint32_t fraction = Log2FractionPart(x, integer);
  const uint32_t log2 = (integer << kLogScaleLog2) + fraction;
  const uint32_t round = kLogScale / 2;
  const uint32_t loge = (((uint64_t)kLogCoeff) * log2 + round) >> kLogScaleLog2;
  
  const uint32_t loge_scaled = ((loge << scale_shift) + round) >> kLogScaleLog2;
  return loge_scaled;
}

uint16_t* LogScaleApply(struct LogScaleState* state, uint32_t* signal,
                        int signal_size, int correction_bits) {
  const int scale_shift = state->scale_shift;
  uint16_t* output = (uint16_t*)signal;
  uint16_t* ret = output;
  int i;
  for (i = 0; i < signal_size; ++i) {
    uint32_t value = *signal++;
    if (state->enable_log) {
      if (correction_bits < 0) {
        value >>= -correction_bits;
      } else {
        value <<= correction_bits;
      }
      if (value > 1) {
        value = Log(value, scale_shift);
      } else {
        value = 0;
      }
    }
    *output++ = (value < kuint16max) ? value : kuint16max;
  }
  return ret;
}
