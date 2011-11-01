/**
 *  g729a codec for iPhone and iPod Touch
 *  Copyright (C) 2009 Samuel <samuelv0304@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/****************************************************************************************
Portions of this file are derived from the following ITU standard:
   ITU-T G.729A Speech Coder    ANSI-C Source Code
   Version 1.1    Last modified: September 1996

   Copyright (c) 1996,
   AT&T, France Telecom, NTT, Universite de Sherbrooke
****************************************************************************************/

/*-----------------------------------------------------*
 * Function Autocorr()                                 *
 *                                                     *
 *   Compute autocorrelations of signal with windowing *
 *                                                     *
 *-----------------------------------------------------*/

#include "typedef.h"
#include "basic_op.h"
#include "oper_32b.h"

#include "ld8a.h"
#include "tab_ld8a.h"

void Autocorr(
  Word16 x[],      /* (i)    : Input signal                      */
  Word16 m,        /* (i)    : LPC order                         */
  Word16 r_h[],    /* (o)    : Autocorrelations  (msb)           */
  Word16 r_l[]     /* (o)    : Autocorrelations  (lsb)           */
)
{
  Word16 i, j, norm;
  Word16 y[L_WINDOW];
  Word32 sum;

  /* Windowing of signal */
  sum = 0;
  for(i=0; i<L_WINDOW; i++)
  {
    y[i] = (Word16)(((Word32)x[i] * (Word32)hamwindow[i] + 0x4000) >> 15);
    sum += ((Word32)y[i] * (Word32)y[i]) << 1;
    if (sum < 0) // overflow
      break;
  }

  if (i != L_WINDOW) // overflow
  {
    for (; i<L_WINDOW; i++)
      y[i] = (Word16)(((Word32)x[i] * (Word32)hamwindow[i] + 0x4000) >> 15);

    /* Compute r[0] and test for overflow */
    while (1)
    {
      /* If overflow divide y[] by 4 */
      sum = 0;
      for(i=0; i<L_WINDOW; i++)
      {
        y[i] >>= 2;
        sum += ((Word32)y[i] * (Word32)y[i]);
      }
      sum <<= 1;
      sum += 1; /* Avoid case of all zeros */
      if (sum > 0)
        break;
    }
  }
  else
    sum += 1; /* Avoid case of all zeros */

  /* Normalization of r[0] */
  norm = norm_l_g729(sum);
  sum  <<= norm;

  /* Put in DPF format (see oper_32b) */
  r_h[0] = (Word16)(sum >> 16);
  r_l[0] = (Word16)((sum >> 1) - ((Word32)r_h[0] << 15));

  /* r[1] to r[m] */

  for (i = 1; i <= m; i++)
  {
    sum = 0;
    for(j=0; j<L_WINDOW-i; j++)
      sum += (Word32)y[j] * (Word32)y[j+i];

    sum <<= norm + 1;
    r_h[i] = (Word16)(sum >> 16);
    r_l[i] = (Word16)((sum >> 1) - ((Word32)r_h[i] << 15));
  }
}

/*-------------------------------------------------------*
 * Function Lag_window()                                 *
 *                                                       *
 * Lag_window on autocorrelations.                       *
 *                                                       *
 * r[i] *= lag_wind[i]                                   *
 *                                                       *
 *  r[i] and lag_wind[i] are in special double precision.*
 *  See "oper_32b.c" for the format                      *
 *                                                       *
 *-------------------------------------------------------*/

void Lag_window(
  Word16 m,         /* (i)     : LPC order                        */
  Word16 r_h[],     /* (i/o)   : Autocorrelations  (msb)          */
  Word16 r_l[]      /* (i/o)   : Autocorrelations  (lsb)          */
)
{
  Word16 i;
  Word32 x;

  for(i=1; i<=m; i++)
  {
     //x  = Mpy_32(r_h[i], r_l[i], lag_h[i-1], lag_l[i-1]);
     x = (((Word32)r_h[i]*lag_h[i-1])<<1) +
         (( (((Word32)r_h[i]*lag_l[i-1])>>15) + (((Word32)r_l[i]*lag_h[i-1])>>15) )<<1);
     //L_Extract(x, &r_h[i], &r_l[i]);
     r_h[i] = (Word16) (x >> 16);
     r_l[i] = (Word16)((x >> 1) - (r_h[i] << 15));
  }
}


/*___________________________________________________________________________
 |                                                                           |
 |      LEVINSON-DURBIN algorithm in double precision                        |
 |      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~                        |
 |---------------------------------------------------------------------------|
 |                                                                           |
 | Algorithm                                                                 |
 |                                                                           |
 |       R[i]    autocorrelations.                                           |
 |       A[i]    filter coefficients.                                        |
 |       K       reflection coefficients.                                    |
 |       Alpha   prediction gain.                                            |
 |                                                                           |
 |       Initialization:                                                     |
 |               A[0] = 1                                                    |
 |               K    = -R[1]/R[0]                                           |
 |               A[1] = K                                                    |
 |               Alpha = R[0] * (1-K**2]                                     |
 |                                                                           |
 |       Do for  i = 2 to M                                                  |
 |                                                                           |
 |            S =  SUM ( R[j]*A[i-j] ,j=1,i-1 ) +  R[i]                      |
 |                                                                           |
 |            K = -S / Alpha                                                 |
 |                                                                           |
 |            An[j] = A[j] + K*A[i-j]   for j=1 to i-1                       |
 |                                      where   An[i] = new A[i]             |
 |            An[i]=K                                                        |
 |                                                                           |
 |            Alpha=Alpha * (1-K**2)                                         |
 |                                                                           |
 |       END                                                                 |
 |                                                                           |
 | Remarks on the dynamics of the calculations.                              |
 |                                                                           |
 |       The numbers used are in double precision in the following format :  |
 |       A = AH <<16 + AL<<1.  AH and AL are 16 bit signed integers.         |
 |       Since the LSB's also contain a sign bit, this format does not       |
 |       correspond to standard 32 bit integers.  We use this format since   |
 |       it allows fast execution of multiplications and divisions.          |
 |                                                                           |
 |       "DPF" will refer to this special format in the following text.      |
 |       See oper_32b.c                                                      |
 |                                                                           |
 |       The R[i] were normalized in routine AUTO (hence, R[i] < 1.0).       |
 |       The K[i] and Alpha are theoretically < 1.0.                         |
 |       The A[i], for a sampling frequency of 8 kHz, are in practice        |
 |       always inferior to 16.0.                                            |
 |                                                                           |
 |       These characteristics allow straigthforward fixed-point             |
 |       implementation.  We choose to represent the parameters as           |
 |       follows :                                                           |
 |                                                                           |
 |               R[i]    Q31   +- .99..                                      |
 |               K[i]    Q31   +- .99..                                      |
 |               Alpha   Normalized -> mantissa in Q31 plus exponent         |
 |               A[i]    Q27   +- 15.999..                                   |
 |                                                                           |
 |       The additions are performed in 32 bit.  For the summation used      |
 |       to calculate the K[i], we multiply numbers in Q31 by numbers        |
 |       in Q27, with the result of the multiplications in Q27,              |
 |       resulting in a dynamic of +- 16.  This is sufficient to avoid       |
 |       overflow, since the final result of the summation is                |
 |       necessarily < 1.0 as both the K[i] and Alpha are                    |
 |       theoretically < 1.0.                                                |
 |___________________________________________________________________________|
*/


/* Last A(z) for case of unstable filter */

static Word16 old_A[M+1]={4096,0,0,0,0,0,0,0,0,0,0};
static Word16 old_rc[2]={0,0};


void Levinson(
  Word16 Rh[],      /* (i)     : Rh[M+1] Vector of autocorrelations (msb) */
  Word16 Rl[],      /* (i)     : Rl[M+1] Vector of autocorrelations (lsb) */
  Word16 A[],       /* (o) Q12 : A[M]    LPC coefficients  (m = 10)       */
  Word16 rc[]       /* (o) Q15 : rc[M]   Reflection coefficients.         */
)
{
 Word16 i, j;
 Word16 hi, lo;
 Word16 Kh, Kl;                /* reflection coefficient; hi and lo           */
 Word16 alp_h, alp_l, alp_exp; /* Prediction gain; hi lo and exponent         */
 Word16 Ah[M+1], Al[M+1];      /* LPC coef. in double prec.                   */
 Word16 Anh[M+1], Anl[M+1];    /* LPC coef.for next iteration in double prec. */
 Word32 t0, t1, t2;            /* temporary variable                          */


	/* K = A[1] = -R[1] / R[0] */

  /* R[1] in Q31      */
  t1 = (((Word32) Rh[1]) << 16) + ((Word32)Rl[1] << 1);
  t2  = L_abs_g729(t1);                      /* abs R[1]         */
  t0  = Div_32(t2, Rh[0], Rl[0]);       /* R[1]/R[0] in Q31 */
  if(t1 > 0) t0= -t0;          /* -R[1]/R[0]       */
  /* K in DPF         */
  Kh = (Word16)(t0 >> 16);
  Kl = (Word16)((t0 >> 1) - ((Word32)(Kh) << 15));
  rc[0] = Kh;

  /* A[1] in Q27      */
  /* A[1] in DPF      */
  Ah[1] = (Word16)(t0 >> 20);
  Al[1] = (Word16)((t0 >> 5) - ((Word32)(Ah[1]) << 15));

/*  Alpha = R[0] * (1-K**2) */

  t0  = (((Word32)Kh * Kl) >> 15) << 1;     /* K*K in Q31    */
  t0 += ((Word32)Kh * Kh);
  t0 <<= 1;

  /* Some case <0 !! */
  /* 1 - K*K  in Q31 */
 	t0 = (t0 < 0 ? MAX_32 + t0 : MAX_32 - t0);

  /* DPF format      */
  hi = (Word16)(t0 >> 16);
  lo = (Word16)((t0 >> 1) - ((Word32)(hi) << 15));

  t0  = (((Word32)Rh[0] * lo) >> 15);     /* Alpha in Q31    */
  t0 += (((Word32)Rl[0] * hi) >> 15);
  t0 += ((Word32)Rh[0] * hi);
  t0 <<= 1;
/* Normalize Alpha */

  alp_exp = norm_l_g729(t0);
  //t0 = L_shl(t0, alp_exp);
  t0 = t0 << alp_exp;
  /* DPF format    */
  alp_h = (Word16)(t0 >> 16);
  alp_l = (Word16)((t0 >> 1) - ((Word32)(alp_h) << 15));

/*--------------------------------------*
 * ITERATIONS  I=2 to M                 *
 *--------------------------------------*/

  for(i= 2; i<=M; i++)
  {

    /* t0 = SUM ( R[j]*A[i-j] ,j=1,i-1 ) +  R[i] */

    t0 = 0;
    for(j=1; j<i; j++)
    {
      //t0 = L_add(t0, Mpy_32(Rh[j], Rl[j], Ah[i-j], Al[i-j]));
      t0 += (((Word32)Rh[j] * Al[i-j]) >> 15);
      t0 += (((Word32)Rl[j] * Ah[i-j]) >> 15);
      t0 += ((Word32) Rh[j] * Ah[i-j]);
    }

    t0 <<= 5;                          /* result in Q27 -> convert to Q31 */
                                       /* No overflow possible            */
    t1 = ((Word32) Rh[i] << 16) + ((Word32)(Rl[i]) << 1);
    t0 += t1;                           /* add R[i] in Q31                 */

    /* K = -t0 / Alpha */

    t1 = L_abs_g729(t0);
    t2 = Div_32(t1, alp_h, alp_l);     /* abs(t0)/Alpha                   */
    if(t0 > 0) t2= -t2;                /* K =-t0/Alpha                    */
    t2 = L_shl(t2, alp_exp);           /* denormalize; compare to Alpha   */
    /* K in DPF                        */
    Kh = (Word16)(t2 >> 16);
    Kl = (Word16)((t2 >> 1) - ((Word32)(Kh) << 15));
    rc[i-1] = Kh;

    /* Test for unstable filter. If unstable keep old A(z) */
    if (abs_s(Kh) > 32750)
    {
      Copy(old_A, A, M+1);
      rc[0] = old_rc[0];        /* only two rc coefficients are needed */
      rc[1] = old_rc[1];
      return;
    }

    /*------------------------------------------*
     *  Compute new LPC coeff. -> An[i]         *
     *  An[j]= A[j] + K*A[i-j]     , j=1 to i-1 *
     *  An[i]= K                                *
     *------------------------------------------*/


    for(j=1; j<i; j++)
    {
      t0  = (((Word32)Kh* Al[i-j]) >> 15);
      t0 += (((Word32)Kl* Ah[i-j]) >> 15);
      t0 += ((Word32)Kh* Ah[i-j]);

      t0 += ((Word32)Ah[j] << 15) + (Word32)Al[j];

      Anh[j] = (Word16)(t0 >> 15);
      Anl[j] = (Word16)(t0 - ((Word32)(Anh[j] << 15)));
    }

    /* t2 = K in Q31 ->convert to Q27  */
    Anh[i] = (Word16) (t2 >> 20);
    Anl[i] = (Word16)((t2 >> 5) - ((Word32)(Anh[i]) << 15));

    /*  Alpha = Alpha * (1-K**2) */

    t0  = (((Word32)Kh * Kl) >> 15) << 1;     /* K*K in Q31    */
 		t0 += ((Word32)Kh * Kh);
  	t0 <<= 1;

    /* Some case <0 !! */
    /* 1 - K*K  in Q31 */
    t0 = (t0 < 0 ? MAX_32 + t0 : MAX_32 - t0);

    /* DPF format      */
    hi = (Word16)(t0 >> 16);
    lo = (Word16)((t0 >> 1) - ((Word32)(hi) << 15));

		t0  = (((Word32)alp_h * lo) >> 15);     /* Alpha in Q31    */
    t0 += (((Word32)alp_l * hi) >> 15);
    t0 += ((Word32)alp_h * hi);
    t0 <<= 1;

    /* Normalize Alpha */
    j = norm_l_g729(t0);
    t0 <<= j;

    /* DPF format    */
    alp_h = (Word16)(t0 >> 16);
    alp_l = (Word16)((t0 >> 1) - ((Word32)(alp_h) << 15));
	  alp_exp += j;             /* Add normalization to alp_exp */

    /* A[j] = An[j] */
    Copy(&Anh[1], &Ah[1], i);
    Copy(&Anl[1], &Al[1], i);
  }

  /* Truncate A[i] in Q27 to Q12 with rounding */

  A[0] = 4096;
  for(i=1; i<=M; i++)
  {
    t0 = ((Word32) Ah[i] << 15) + Al[i];
    old_A[i] = A[i] = (Word16)((t0 + 0x00002000) >> 14);
  }
  old_rc[0] = rc[0];
  old_rc[1] = rc[1];
}



/*-------------------------------------------------------------*
 *  procedure Az_lsp:                                          *
 *            ~~~~~~                                           *
 *   Compute the LSPs from  the LPC coefficients  (order=10)   *
 *-------------------------------------------------------------*/

/* local function */

static Word16 Chebps_11(Word16 x, Word16 f[], Word16 n);
static Word16 Chebps_10(Word16 x, Word16 f[], Word16 n);

void Az_lsp(
  Word16 a[],        /* (i) Q12 : predictor coefficients              */
  Word16 lsp[],      /* (o) Q15 : line spectral pairs                 */
  Word16 old_lsp[]   /* (i)     : old lsp[] (in case not found 10 roots) */
)
{
 Word16 i, j, nf, ip;
 Word16 xlow, ylow, xhigh, yhigh, xmid, ymid, xint;
 Word16 x, y, sign, exp;
 Word16 *coef;
 Word16 f1[M/2+1], f2[M/2+1];
 Word32 L_temp1, L_temp2;
 Word16 (*pChebps)(Word16 x, Word16 f[], Word16 n);

/*-------------------------------------------------------------*
 *  find the sum and diff. pol. F1(z) and F2(z)                *
 *    F1(z) <--- F1(z)/(1+z**-1) & F2(z) <--- F2(z)/(1-z**-1)  *
 *                                                             *
 * f1[0] = 1.0;                                                *
 * f2[0] = 1.0;                                                *
 *                                                             *
 * for (i = 0; i< NC; i++)                                     *
 * {                                                           *
 *   f1[i+1] = a[i+1] + a[M-i] - f1[i] ;                       *
 *   f2[i+1] = a[i+1] - a[M-i] + f2[i] ;                       *
 * }                                                           *
 *-------------------------------------------------------------*/

 pChebps = Chebps_11;

 f1[0] = 2048;          /* f1[0] = 1.0 is in Q11 */
 f2[0] = 2048;          /* f2[0] = 1.0 is in Q11 */

 for (i = 0; i< NC; i++)
 {
	 L_temp1 = (Word32)a[i+1];
   L_temp2 = (Word32)a[M-i];

   /* x = (a[i+1] + a[M-i]) >> 1        */
   x = ((L_temp1 + L_temp2) >> 1);
   /* x = (a[i+1] - a[M-i]) >> 1        */
   y = ((L_temp1 - L_temp2) >> 1);

   /* f1[i+1] = a[i+1] + a[M-i] - f1[i] */
   L_temp1 = (Word32)x - (Word32)f1[i];
   if (L_temp1 > 0x00007fffL || L_temp1 < (Word32)0xffff8000L)
     break;
   f1[i+1] = (Word16)L_temp1;

   /* f2[i+1] = a[i+1] - a[M-i] + f2[i] */
   L_temp2 = (Word32)y + (Word32)f2[i];
   if (L_temp2 > 0x00007fffL || (L_temp2 < (Word32)0xffff8000L))
     break;
   f2[i+1] = (Word16)L_temp2;
 }

 if (i != NC) {
   //printf("===== OVF ovf_coef =====\n");

   pChebps = Chebps_10;

   f1[0] = 1024;          /* f1[0] = 1.0 is in Q10 */
   f2[0] = 1024;          /* f2[0] = 1.0 is in Q10 */

   for (i = 0; i< NC; i++)
   {
     L_temp1 = (Word32)a[i+1];
     L_temp2 = (Word32)a[M-i];
     /* x = (a[i+1] + a[M-i]) >> 2  */
     x = (Word16)((L_temp1 + L_temp2) >> 2);
     /* y = (a[i+1] - a[M-i]) >> 2 */
     y = (Word16)((L_temp1 - L_temp2) >> 2);

     f1[i+1] = x - f1[i];            /* f1[i+1] = a[i+1] + a[M-i] - f1[i] */
     f2[i+1] = y + f2[i];            /* f2[i+1] = a[i+1] - a[M-i] + f2[i] */
   }
 }

/*-------------------------------------------------------------*
 * find the LSPs using the Chebichev pol. evaluation           *
 *-------------------------------------------------------------*/

 nf=0;          /* number of found frequencies */
 ip=0;          /* indicator for f1 or f2      */

 coef = f1;

 xlow = grid[0];
 ylow = (*pChebps)(xlow, coef, NC);

 j = 0;
 while ( (nf < M) && (j < GRID_POINTS) )
 {
   j++;
   xhigh = xlow;
   yhigh = ylow;
   xlow  = grid[j];
   ylow  = (*pChebps)(xlow,coef,NC);

   if (((Word32)ylow*yhigh) <= 0)
   {
     /* divide 2 times the interval */
     for (i = 0; i < 2; i++)
     {
       /* xmid = (xlow + xhigh)/2 */
			 xmid = (xlow >> 1) + (xhigh >> 1);

       ymid = (*pChebps)(xmid,coef,NC);

       if ( ((Word32)ylow*ymid) <= (Word32)0)
       {
         yhigh = ymid;
         xhigh = xmid;
       }
       else
       {
         ylow = ymid;
         xlow = xmid;
       }
     }

    /*-------------------------------------------------------------*
     * Linear interpolation                                        *
     *    xint = xlow - ylow*(xhigh-xlow)/(yhigh-ylow);            *
     *-------------------------------------------------------------*/

     x   = xhigh - xlow;
     y   = yhigh - ylow;

     if(y == 0)
     {
       xint = xlow;
     }
     else
     {
       sign= y;
       y   = abs_s(y);
       exp = norm_s_g729(y);
       y <<= exp;
       y   = div_s_g729( (Word16)16383, y);
       /* y= (xhigh-xlow)/(yhigh-ylow) in Q11 */
			 y = ((Word32)x * (Word32)y) >> (19 - exp);

       if(sign < 0) y = -y;

       /* xint = xlow - ylow*y */
       xint = xlow - (Word16)(((Word32) ylow * y) >> 10);
     }

     lsp[nf] = xint;
     xlow    = xint;
     nf++;

     if(ip == 0)
     {
       ip = 1;
       coef = f2;
     }
     else
     {
       ip = 0;
       coef = f1;
     }
     ylow = (*pChebps)(xlow,coef,NC);

   }
 }

 /* Check if M roots found */

 if (nf < M)
 {
   Copy(old_lsp, lsp, M);
 /* printf("\n !!Not 10 roots found in Az_lsp()!!!\n"); */
 }
}

/*--------------------------------------------------------------*
 * function  Chebps_11, Chebps_10:                              *
 *           ~~~~~~~~~~~~~~~~~~~~                               *
 *    Evaluates the Chebichev polynomial series                 *
 *--------------------------------------------------------------*
 *                                                              *
 *  The polynomial order is                                     *
 *     n = M/2   (M is the prediction order)                    *
 *  The polynomial is given by                                  *
 *    C(x) = T_n(x) + f(1)T_n-1(x) + ... +f(n-1)T_1(x) + f(n)/2 *
 * Arguments:                                                   *
 *  x:     input value of evaluation; x = cos(frequency) in Q15 *
 *  f[]:   coefficients of the pol.                             *
 *                         in Q11(Chebps_11), in Q10(Chebps_10) *
 *  n:     order of the pol.                                    *
 *                                                              *
 * The value of C(x) is returned. (Saturated to +-1.99 in Q14)  *
 *                                                              *
 *--------------------------------------------------------------*/
static Word16 Chebps_11(Word16 x, Word16 f[], Word16 n)
{
  Word16 i, cheb;
  Word16 b1_h, b1_l;
  Word32 t0;
  Word32 L_temp;

 /* Note: All computation are done in Q24. */

  L_temp = 0x01000000;

  /* 2*x in Q24 + f[1] in Q24 */
  t0 = ((Word32)x << 10) + ((Word32)f[1] << 13);

  /* b1 = 2*x + f[1]     */
  b1_h = (Word16)(t0 >> 16);
  b1_l = (Word16)((t0 >> 1) - (b1_h << 15));

  for (i = 2; i<n; i++)
  {
    /* t0 = 2.0*x*b1              */
    t0  = ((Word32) b1_h * x) + (((Word32) b1_l * x) >> 15);
    t0 <<= 2;
    /* t0 = 2.0*x*b1 - b2         */
    t0 -= L_temp;
    /* t0 = 2.0*x*b1 - b2 + f[i]; */
    t0 += ((Word32)f[i] << 13);

    /* b2 = b1; */
    L_temp = ((Word32) b1_h << 16) + ((Word32) b1_l << 1);

    /* b0 = 2.0*x*b1 - b2 + f[i]; */
    b1_h = (Word16)(t0 >> 16);
    b1_l = (Word16)((t0 >> 1) - (b1_h << 15));
  }

  /* t0 = x*b1;              */
  t0  = ((Word32) b1_h * x) + (((Word32) b1_l * x) >> 15);
  t0 <<= 1;
  /* t0 = x*b1 - b2          */
  t0 -= L_temp;
  /* t0 = x*b1 - b2 + f[i]/2 */
  t0 += ((Word32)f[i] << 12);

  /* Q24 to Q30 with saturation */
  /* Result in Q14              */
  if ((UWord32)(t0 - 0xfe000000L) < 0x01ffffffL -  0xfe000000L)
    cheb = (Word16)(t0 >> 10);
  else
    cheb = t0 > (Word32) 0x01ffffffL ? MAX_16 : MIN_16;

  return(cheb);
}

static Word16 Chebps_10(Word16 x, Word16 f[], Word16 n)
{
  Word16 i, cheb;
  Word16 b1_h, b1_l;
  Word32 t0;
  Word32 L_temp;

 /* Note: All computation are done in Q23. */

  L_temp = 0x00800000;

  /* 2*x + f[1] in Q23          */
  t0 = ((Word32)x << 9) + ((Word32)f[1] << 13);

  /* b1 = 2*x + f[1]     */
  b1_h = (Word16)(t0 >> 16);
  b1_l = (Word16)((t0 >> 1) - (b1_h << 15));

  for (i = 2; i<n; i++)
  {
    /* t0 = 2.0*x*b1              */
    t0  = ((Word32) b1_h * x) + (((Word32) b1_l * x) >> 15);
    t0 <<= 2;
    /* t0 = 2.0*x*b1 - b2         */
    t0 -= L_temp;

    /* t0 = 2.0*x*b1 - b2 + f[i]; */
    t0 += ((Word32)f[i] << 13);

    /* b2 = b1; */
    L_temp = ((Word32) b1_h << 16) + ((Word32) b1_l << 1);

    /* b0 = 2.0*x*b1 - b2 + f[i]; */
    b1_h = (Word16)(t0 >> 16);
    b1_l = (Word16)((t0 >> 1) - (b1_h << 15));
  }

  /* t0 = x*b1;              */
  t0  = ((Word32) b1_h * x) + (((Word32) b1_l * x) >> 15);
  t0 <<= 1;
  /* t0 = x*b1 - b2          */
  t0 -= L_temp;
  /* t0 = x*b1 - b2 + f[i]/2 */
  t0 += ((Word32)f[i] << 12);

  /* Q23 to Q30 with saturation */
  /* Result in Q14              */
  if ((UWord32)(t0 - 0xff000000L) < 0x00ffffffL -  0xff000000L)
    cheb = (Word16)(t0 >> 9);
  else
    cheb = t0 > (Word32) 0x00ffffffL ? MAX_16 : MIN_16;

  return(cheb);
}
