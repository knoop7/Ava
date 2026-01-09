#ifndef _KISS_FFT_GUTS_H
#define _KISS_FFT_GUTS_H



#include "kiss_fft.h"
#include <limits.h>

#define MAXFACTORS 32


struct kiss_fft_state{
    int nfft;
    int inverse;
    int factors[2*MAXFACTORS];
    kiss_fft_cpx twiddles[1];
};


#ifdef FIXED_POINT
#if (FIXED_POINT==32)
# define FRACBITS 31
# define SAMPPROD int64_t
#define SAMP_MAX 2147483647
#else
# define FRACBITS 15
# define SAMPPROD int32_t 
#define SAMP_MAX 32767
#endif

#define SAMP_MIN -SAMP_MAX

#if defined(CHECK_OVERFLOW)
#  define CHECK_OVERFLOW_OP(a,op,b)  \
	if ( (SAMPPROD)(a) op (SAMPPROD)(b) > SAMP_MAX || (SAMPPROD)(a) op (SAMPPROD)(b) < SAMP_MIN ) { \
		fprintf(stderr,"WARNING:overflow @ " __FILE__ "(%d): (%d " #op" %d) = %ld\n",__LINE__,(a),(b),(SAMPPROD)(a) op (SAMPPROD)(b) );  }
#endif


#   define smul(a,b) ( (SAMPPROD)(a)*(b) )
#   define sround( x )  (kiss_fft_scalar)( ( (x) + (1<<(FRACBITS-1)) ) >> FRACBITS )

#   define S_MUL(a,b) sround( smul(a,b) )

#   define C_MUL(m,a,b) \
      do{ (m).r = sround( smul((a).r,(b).r) - smul((a).i,(b).i) ); \
          (m).i = sround( smul((a).r,(b).i) + smul((a).i,(b).r) ); }while(0)

#   define DIVSCALAR(x,k) \
	(x) = sround( smul(  x, SAMP_MAX/k ) )

#   define C_FIXDIV(c,div) \
	do {    DIVSCALAR( (c).r , div);  \
		DIVSCALAR( (c).i  , div); }while (0)

#   define C_MULBYSCALAR( c, s ) \
    do{ (c).r =  sround( smul( (c).r , s ) ) ;\
        (c).i =  sround( smul( (c).i , s ) ) ; }while(0)

#else  

#   define S_MUL(a,b) ( (a)*(b) )
#define C_MUL(m,a,b) \
    do{ (m).r = (a).r*(b).r - (a).i*(b).i;\
        (m).i = (a).r*(b).i + (a).i*(b).r; }while(0)
#   define C_FIXDIV(c,div) 
#   define C_MULBYSCALAR( c, s ) \
    do{ (c).r *= (s);\
        (c).i *= (s); }while(0)
#endif

#ifndef CHECK_OVERFLOW_OP
#  define CHECK_OVERFLOW_OP(a,op,b) 
#endif

#define  C_ADD( res, a,b)\
    do { \
	    CHECK_OVERFLOW_OP((a).r,+,(b).r)\
	    CHECK_OVERFLOW_OP((a).i,+,(b).i)\
	    (res).r=(a).r+(b).r;  (res).i=(a).i+(b).i; \
    }while(0)
#define  C_SUB( res, a,b)\
    do { \
	    CHECK_OVERFLOW_OP((a).r,-,(b).r)\
	    CHECK_OVERFLOW_OP((a).i,-,(b).i)\
	    (res).r=(a).r-(b).r;  (res).i=(a).i-(b).i; \
    }while(0)
#define C_ADDTO( res , a)\
    do { \
	    CHECK_OVERFLOW_OP((res).r,+,(a).r)\
	    CHECK_OVERFLOW_OP((res).i,+,(a).i)\
	    (res).r += (a).r;  (res).i += (a).i;\
    }while(0)

#define C_SUBFROM( res , a)\
    do {\
	    CHECK_OVERFLOW_OP((res).r,-,(a).r)\
	    CHECK_OVERFLOW_OP((res).i,-,(a).i)\
	    (res).r -= (a).r;  (res).i -= (a).i; \
    }while(0)


#ifdef FIXED_POINT
#  define KISS_FFT_COS(phase)  floor(.5+SAMP_MAX * cos (phase))
#  define KISS_FFT_SIN(phase)  floor(.5+SAMP_MAX * sin (phase))
#  define HALF_OF(x) ((x)>>1)
#elif defined(USE_SIMD)
#  define KISS_FFT_COS(phase) _mm_set1_ps( cos(phase) )
#  define KISS_FFT_SIN(phase) _mm_set1_ps( sin(phase) )
#  define HALF_OF(x) ((x)*_mm_set1_ps(.5))
#else
#  define KISS_FFT_COS(phase) (kiss_fft_scalar) cos(phase)
#  define KISS_FFT_SIN(phase) (kiss_fft_scalar) sin(phase)
#  define HALF_OF(x) ((x)*.5)
#endif

#define  kf_cexp(x,phase) \
	do{ \
		(x)->r = KISS_FFT_COS(phase);\
		(x)->i = KISS_FFT_SIN(phase);\
	}while(0)



#define pcpx(c)\
    fprintf(stderr,"%g + %gi\n",(double)((c)->r),(double)((c)->i) )


#ifdef KISS_FFT_USE_ALLOCA




#include <alloca.h>
#define  KISS_FFT_TMP_ALLOC(nbytes) alloca(nbytes)
#define  KISS_FFT_TMP_FREE(ptr) 
#else
#define  KISS_FFT_TMP_ALLOC(nbytes) KISS_FFT_MALLOC(nbytes)
#define  KISS_FFT_TMP_FREE(ptr) KISS_FFT_FREE(ptr)
#endif

#endif 
