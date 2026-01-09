#ifndef KISS_FFT_H
#define KISS_FFT_H

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#ifdef __cplusplus
extern "C++" {
#endif



#ifdef USE_SIMD
# include <xmmintrin.h>
# define kiss_fft_scalar __m128
#define KISS_FFT_MALLOC(nbytes) _mm_malloc(nbytes,16)
#define KISS_FFT_FREE _mm_free
#else	


#define KISS_FFT_MALLOC(X) (void*)(0x0) 
#define KISS_FFT_FREE(X) 
#endif	


#ifdef FIXED_POINT
#include <stdint.h> 
# if (FIXED_POINT == 32)
#  define kiss_fft_scalar int32_t
# else	
#  define kiss_fft_scalar int16_t
# endif
#else
# ifndef kiss_fft_scalar

#   define kiss_fft_scalar float
# endif
#endif

typedef struct {
    kiss_fft_scalar r;
    kiss_fft_scalar i;
}kiss_fft_cpx;

typedef struct kiss_fft_state* kiss_fft_cfg;



kiss_fft_cfg kiss_fft_alloc(int nfft,int inverse_fft,void * mem,size_t * lenmem); 


void kiss_fft(kiss_fft_cfg cfg,const kiss_fft_cpx *fin,kiss_fft_cpx *fout);


void kiss_fft_stride(kiss_fft_cfg cfg,const kiss_fft_cpx *fin,kiss_fft_cpx *fout,int fin_stride);


#define kiss_fft_free free


void kiss_fft_cleanup(void);
	


int kiss_fft_next_fast_size(int n);


#define kiss_fftr_next_fast_size_real(n) \
        (kiss_fft_next_fast_size( ((n)+1)>>1)<<1)

#ifdef __cplusplus
} 
#endif

#endif
