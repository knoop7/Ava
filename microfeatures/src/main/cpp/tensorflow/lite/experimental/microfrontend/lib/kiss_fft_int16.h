

#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_KISS_FFT_INT16_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_KISS_FFT_INT16_H_

#include "tensorflow/lite/experimental/microfrontend/lib/kiss_fft_common.h"




#define FIXED_POINT 16
namespace kissfft_fixed16 {
#include "kiss_fft.h"
#include "tools/kiss_fftr.h"
}  
#undef FIXED_POINT
#undef kiss_fft_scalar
#undef KISS_FFT_H

#endif  
