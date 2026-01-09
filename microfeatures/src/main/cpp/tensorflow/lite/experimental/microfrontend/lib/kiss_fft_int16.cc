#include <cstdint>

#include "tensorflow/lite/experimental/microfrontend/lib/kiss_fft_common.h"

#define FIXED_POINT 16
namespace kissfft_fixed16 {
#include "kiss_fft.c"
#include "tools/kiss_fftr.c"
}  
#undef FIXED_POINT