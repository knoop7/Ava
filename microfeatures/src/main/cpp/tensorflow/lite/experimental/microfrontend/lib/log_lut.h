
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_LUT_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_LOG_LUT_H_

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif



#define kLogSegments 128
#define kLogSegmentsLog2 7


#define kLogScale 65536
#define kLogScaleLog2 16
#define kLogCoeff 45426

extern const uint16_t kLogLut[];

#ifdef __cplusplus
}  
#endif

#endif  
