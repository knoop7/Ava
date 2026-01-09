
#ifndef TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_BITS_H_
#define TENSORFLOW_LITE_EXPERIMENTAL_MICROFRONTEND_LIB_BITS_H_

#ifdef __cplusplus
#include <cstdint>

extern "C" {
#endif

static inline int CountLeadingZeros32Slow(uint64_t n) {
  int zeroes = 28;
  if (n >> 16) zeroes -= 16, n >>= 16;
  if (n >> 8) zeroes -= 8, n >>= 8;
  if (n >> 4) zeroes -= 4, n >>= 4;
  return "\4\3\2\2\1\1\1\1\0\0\0\0\0\0\0"[n] + zeroes;
}

static inline int CountLeadingZeros32(uint32_t n) {
#if defined(_MSC_VER)
  unsigned long result = 0;  
  if (_BitScanReverse(&result, n)) {
    return 31 - result;
  }
  return 32;
#elif defined(__GNUC__)

  
  if (n == 0) {
    return 32;
  }
  return __builtin_clz(n);
#else
  return CountLeadingZeros32Slow(n);
#endif
}

static inline int MostSignificantBit32(uint32_t n) {
  return 32 - CountLeadingZeros32(n);
}

static inline int CountLeadingZeros64Slow(uint64_t n) {
  int zeroes = 60;
  if (n >> 32) zeroes -= 32, n >>= 32;
  if (n >> 16) zeroes -= 16, n >>= 16;
  if (n >> 8) zeroes -= 8, n >>= 8;
  if (n >> 4) zeroes -= 4, n >>= 4;
  return "\4\3\2\2\1\1\1\1\0\0\0\0\0\0\0"[n] + zeroes;
}

static inline int CountLeadingZeros64(uint64_t n) {
#if defined(_MSC_VER) && defined(_M_X64)
  
  unsigned long result = 0;  
  if (_BitScanReverse64(&result, n)) {
    return 63 - result;
  }
  return 64;
#elif defined(_MSC_VER)
  
  unsigned long result = 0;  
  if ((n >> 32) && _BitScanReverse(&result, n >> 32)) {
    return 31 - result;
  }
  if (_BitScanReverse(&result, n)) {
    return 63 - result;
  }
  return 64;
#elif defined(__GNUC__)

  
  if (n == 0) {
    return 64;
  }
  return __builtin_clzll(n);
#else
  return CountLeadingZeros64Slow(n);
#endif
}

static inline int MostSignificantBit64(uint64_t n) {
  return 64 - CountLeadingZeros64(n);
}

#ifdef __cplusplus
}  
#endif

#endif  
