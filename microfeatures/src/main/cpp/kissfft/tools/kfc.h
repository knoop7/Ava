#ifndef KFC_H
#define KFC_H
#include "kiss_fft.h"

#ifdef __cplusplus
extern "C" {
#endif




void kfc_fft(int nfft, const kiss_fft_cpx * fin,kiss_fft_cpx * fout);

void kfc_ifft(int nfft, const kiss_fft_cpx * fin,kiss_fft_cpx * fout);


void kfc_cleanup(void);

#ifdef __cplusplus
}
#endif

#endif
