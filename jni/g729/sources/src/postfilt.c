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

/*------------------------------------------------------------------------*
 *                         POSTFILTER.C                                   *
 *------------------------------------------------------------------------*
 * Performs adaptive postfiltering on the synthesis speech                *
 * This file contains all functions related to the post filter.           *
 *------------------------------------------------------------------------*/

#include "typedef.h"
#include "basic_op.h"
#include "ld8a.h"
#include "oper_32b.h"

#include "g729a_decoder.h"

/*---------------------------------------------------------------*
 *    Postfilter constant parameters (defined in "ld8a.h")       *
 *---------------------------------------------------------------*
 *   L_FRAME     : Frame size.                                   *
 *   L_SUBFR     : Sub-frame size.                               *
 *   M           : LPC order.                                    *
 *   MP1         : LPC order+1                                   *
 *   PIT_MAX     : Maximum pitch lag.                            *
 *   GAMMA2_PST  : Formant postfiltering factor (numerator)      *
 *   GAMMA1_PST  : Formant postfiltering factor (denominator)    *
 *   GAMMAP      : Harmonic postfiltering factor                 *
 *   MU          : Factor for tilt compensation filter           *
 *   AGC_FAC     : Factor for automatic gain control             *
 *---------------------------------------------------------------*/

/*---------------------------------------------------------------*
 * Procedure    Init_Post_Filter:                                *
 *              ~~~~~~~~~~~~~~~~                                 *
 *  Initializes the postfilter parameters:                       *
 *---------------------------------------------------------------*/

void Init_Post_Filter(g729a_post_filter_state *state)
{
  state->res2  = state->res2_buf + PIT_MAX;
  state->scal_res2  = state->scal_res2_buf + PIT_MAX;

  Set_zero(state->mem_syn_pst, M);
  Set_zero(state->res2_buf, PIT_MAX+L_SUBFR);
  Set_zero(state->scal_res2_buf, PIT_MAX+L_SUBFR);
}

/*------------------------------------------------------------------------*
 *  Procedure     Post_Filter:                                            *
 *                ~~~~~~~~~~~                                             *
 *------------------------------------------------------------------------*
 *  The postfiltering process is described as follows:                    *
 *                                                                        *
 *  - inverse filtering of syn[] through A(z/GAMMA2_PST) to get res2[]    *
 *  - use res2[] to compute pitch parameters                              *
 *  - perform pitch postfiltering                                         *
 *  - tilt compensation filtering; 1 - MU*k*z^-1                          *
 *  - synthesis filtering through 1/A(z/GAMMA1_PST)                       *
 *  - adaptive gain control                                               *
 *------------------------------------------------------------------------*/



void Post_Filter(
  g729a_post_filter_state *state,
  Word16 *syn,       /* in/out: synthesis speech (postfiltered is output)    */
  Word16 *Az_4,      /* input : interpolated LPC parameters in all subframes */
  Word16 *T          /* input : decoded pitch lags in all subframes          */
)
{
 /*-------------------------------------------------------------------*
  *           Declaration of parameters                               *
  *-------------------------------------------------------------------*/

 Word16 res2_pst[L_SUBFR];  /* res2[] after pitch postfiltering */
 Word16 syn_pst[L_FRAME];   /* post filtered synthesis speech   */

 Word16 Ap3[MP1], Ap4[MP1];  /* bandwidth expanded LP parameters */

 Word16 *Az;                 /* pointer to Az_4:                 */
                             /*  LPC parameters in each subframe */
 Word16   t0_max, t0_min;    /* closed-loop pitch search range   */
 Word16   i_subfr;           /* index for beginning of subframe  */

 Word16 h[L_H];

 Word16  i, j;
 Word16  temp1, temp2;
 Word32  L_tmp1, L_tmp2;

   /*-----------------------------------------------------*
    * Post filtering                                      *
    *-----------------------------------------------------*/

    Az = Az_4;

    for (i_subfr = 0; i_subfr < L_FRAME; i_subfr += L_SUBFR)
    {
      /* Find pitch range t0_min - t0_max */
      t0_min = *T++ - 3;
      t0_max = t0_min + 6;
      if (t0_max > PIT_MAX) {
        t0_max = PIT_MAX;
        t0_min = t0_max - 6;
      }

      /* Find weighted filter coefficients Ap3[] and ap[4] */

      Weight_Az(Az, GAMMA2_PST, M, Ap3);
      Weight_Az(Az, GAMMA1_PST, M, Ap4);

      /* filtering of synthesis speech by A(z/GAMMA2_PST) to find res2[] */

      Residu(Ap3, &syn[i_subfr], state->res2, L_SUBFR);

      /* scaling of "res2[]" to avoid energy overflow */

      for (j=0; j<L_SUBFR; j++)
        state->scal_res2[j] = state->res2[j] >> 2;

      /* pitch postfiltering */

      pit_pst_filt(state->res2, state->scal_res2, t0_min, t0_max, L_SUBFR, res2_pst);

      /* tilt compensation filter */

      /* impulse response of A(z/GAMMA2_PST)/A(z/GAMMA1_PST) */

      Copy(Ap3, h, M+1);
      Set_zero(&h[M+1], L_H-M-1);
      Syn_filt(Ap4, h, h, L_H, &h[M+1], 0);

      /* 1st correlation of h[] */
      L_tmp1 = h[L_H-1] * h[L_H-1];
      L_tmp2 = 0;
      for (i=0; i<L_H-1; i++)
      {
        L_tmp1 += h[i] * h[i];
        L_tmp2 += h[i] * h[i+1];
      }
      temp1 = L_tmp1 >> 15;
      temp2 = L_tmp2 >> 15;

      if(temp2 <= 0) {
        temp2 = 0;
      }
      else {
        temp2 = mult(temp2, MU);
        temp2 = div_s_g729(temp2, temp1);
      }

      preemphasis(res2_pst, temp2, L_SUBFR);

      /* filtering through  1/A(z/GAMMA1_PST) */

      Syn_filt(Ap4, res2_pst, &syn_pst[i_subfr], L_SUBFR, state->mem_syn_pst, 1);

      /* scale output to input */

      agc(&syn[i_subfr], &syn_pst[i_subfr], L_SUBFR);

      /* update res2[] buffer;  shift by L_SUBFR */

      Copy(&(state->res2[L_SUBFR-PIT_MAX]), &(state->res2[-PIT_MAX]), PIT_MAX);
      Copy(&(state->scal_res2[L_SUBFR-PIT_MAX]), &(state->scal_res2[-PIT_MAX]), PIT_MAX);

      Az += MP1;
    }

    /* update syn[] buffer */

    Copy(&syn[L_FRAME-M], &syn[-M], M);

    /* overwrite synthesis speech by postfiltered synthesis speech */

    Copy(syn_pst, syn, L_FRAME);
}

/*---------------------------------------------------------------------------*
 * procedure pitch_pst_filt                                                  *
 * ~~~~~~~~~~~~~~~~~~~~~~~~                                                  *
 * Find the pitch period  around the transmitted pitch and perform           *
 * harmonic postfiltering.                                                   *
 * Filtering through   (1 + g z^-T) / (1+g) ;   g = min(pit_gain*gammap, 1)  *
 *--------------------------------------------------------------------------*/

void pit_pst_filt(
  Word16 *signal,      /* (i)     : input signal                        */
  Word16 *scal_sig,    /* (i)     : input signal (scaled, divided by 4) */
  Word16 t0_min,       /* (i)     : minimum value in the searched range */
  Word16 t0_max,       /* (i)     : maximum value in the searched range */
  Word16 L_subfr,      /* (i)     : size of filtering                   */
  Word16 *signal_pst   /* (o)     : harmonically postfiltered signal    */
)
{
  Word16 i, j, t0;
  Word16 g0, gain, cmax, en, en0;
  Word16 *p, *p1, *deb_sig;
  Word32 corr, cor_max, ener, ener0, temp;

/*---------------------------------------------------------------------------*
 * Compute the correlations for all delays                                   *
 * and select the delay which maximizes the correlation                      *
 *---------------------------------------------------------------------------*/

  deb_sig = &scal_sig[-t0_min];
  cor_max = MIN_32;
  t0 = t0_min;             /* Only to remove warning from some compilers */
  for (i=t0_min; i<=t0_max; i++)
  {
    corr = 0;
    p    = scal_sig;
    p1   = deb_sig;
    for (j=0; j<L_subfr; j++)
      corr += *p++ * *p1++;
    corr <<= 1;

    if (corr > cor_max)
    {
      cor_max = corr;
      t0 = i;
    }
    deb_sig--;
  }

  /* Compute the signal energy in the present subframe */
  ener0 = 0;
  /* Compute the energy of the signal delayed by t0 */
  ener = 0;
  for ( i=0; i<L_subfr; i++)
  {
    ener0 += scal_sig[i   ]*scal_sig[i   ];
    ener  += scal_sig[i-t0]*scal_sig[i-t0];
  }
  ener0 <<= 1; ener0 += 1;
  ener  <<= 1; ener  += 1;

  if (cor_max < 0)
    cor_max = 0;

  /* scale "cor_max", "ener" and "ener0" on 16 bits */

  temp = cor_max;
  if (ener > temp)
  {
    temp = ener;
  }
  if (ener0 > temp)
  {
    temp = ener0;
  }
  j = norm_l_g729(temp);
  cmax = g_round(L_shl(cor_max, j));
  en = g_round(L_shl(ener, j));
  en0 = g_round(L_shl(ener0, j));

  /* prediction gain (dB)= -10 log(1-cor_max*cor_max/(ener*ener0)) */

  /* temp = (cor_max * cor_max) - (0.5 * ener * ener0)  */
  temp = (Word32)cmax * (Word32)cmax - ((Word32)en * (Word32)en0 >> 1);

  if (temp < (Word32)0)           /* if prediction gain < 3 dB   */
  {                               /* switch off pitch postfilter */
		Copy(signal, signal_pst, L_subfr);
    return;
  }

  if (cmax > en)      /* if pitch gain > 1 */
  {
    g0 = INV_GAMMAP;
    gain = GAMMAP_2;
  }
  else {
    /* cmax(Q14) = cmax(Q15) * GAMMAP */
    cmax = (Word16)((Word32)cmax * (Word32)GAMMAP >> 16);
    i = cmax + (en >> 1);  /* Q14 */
    if(i > 0)
    {
      gain = div_s_g729(cmax, i);    /* gain(Q15) = cor_max/(cor_max+ener)  */
      g0 = 32767 - gain;    /* g0(Q15) = 1 - gain */
    }
    else
    {
      g0 =  32767;
      gain = 0;
    }
  }


  for (i = 0; i < L_subfr; i++)
  {
    /* signal_pst[i] = g0*signal[i] + gain*signal[i-t0]; */
		signal_pst[i] = (Word16)(((Word32)g0    * (Word32)signal[i])    >> 15) +
                    (Word16)(((Word32)gain * (Word32)signal[i-t0]) >> 15);
  }
}


/*---------------------------------------------------------------------*
 * routine preemphasis()                                               *
 * ~~~~~~~~~~~~~~~~~~~~~                                               *
 * Preemphasis: filtering through 1 - g z^-1                           *
 *---------------------------------------------------------------------*/

void preemphasis(
  Word16 *signal,  /* (i/o)   : input signal overwritten by the output */
  Word16 g,        /* (i) Q15 : preemphasis coefficient                */
  Word16 L         /* (i)     : size of filtering                      */
)
{
  static Word16 mem_pre = 0;
  Word16 temp, i;

  temp = signal[L-1];

  for (i = L - 1; i > 0; --i)
    signal[i] -= ((Word32)g * (Word32)signal[i-1]) >> 15;

  signal[0] -= ((Word32)g * (Word32)mem_pre) >> 15;

  mem_pre = temp;
}



/*----------------------------------------------------------------------*
 *   routine agc()                                                      *
 *   ~~~~~~~~~~~~~                                                      *
 * Scale the postfilter output on a subframe basis by automatic control *
 * of the subframe gain.                                                *
 *  gain[n] = AGC_FAC * gain[n-1] + (1 - AGC_FAC) g_in/g_out            *
 *----------------------------------------------------------------------*/

void agc(
  Word16 *sig_in,   /* (i)     : postfilter input signal  */
  Word16 *sig_out,  /* (i/o)   : postfilter output signal */
  Word16 l_trm      /* (i)     : subframe size            */
)
{
  static Word16 past_gain=4096;         /* past_gain = 1.0 (Q12) */
  Word16 i, exp;
  Word16 gain_in, gain_out, g0, gain;                     /* Q12 */
  Word32 s;

  Word16 sig;

  /* calculate gain_out with exponent */
  s = 0;
  for(i=0; i<l_trm; i++)
  {
    sig = sig_out[i] >> 2;
    s = L_mac(s, sig, sig);
  }

  if (s == 0) {
    past_gain = 0;
    return;
  }
  exp = norm_l_g729(s) - 1;
  gain_out = g_round(L_shl(s, exp));

  /* calculate gain_in with exponent */
  s = 0;
  for(i=0; i<l_trm; i++)
  {
    sig = sig_in[i] >> 2;
    s = L_mac(s, sig, sig);
  }

  if (s == 0) {
    g0 = 0;
  }
  else {
    i = norm_l_g729(s);
    gain_in = g_round(L_shl(s, i));
    exp = exp - i;

   /*---------------------------------------------------*
    *  g0(Q12) = (1-AGC_FAC) * sqrt(gain_in/gain_out);  *
    *---------------------------------------------------*/

    s = L_deposit_l_g729(div_s_g729(gain_out,gain_in));   /* Q15 */
    s = L_shl(s, 7);           /* s(Q22) = gain_out / gain_in */
    s = L_shr(s, exp);         /* Q22, add exponent */

    /* i(Q12) = s(Q19) = 1 / sqrt(s(Q22)) */
    s = Inv_sqrt(s);           /* Q19 */
    i = g_round(L_shl(s,9));     /* Q12 */

    /* g0(Q12) = i(Q12) * (1-AGC_FAC)(Q15) */
    g0 = mult(i, AGC_FAC1);       /* Q12 */
  }

  /* compute gain(n) = AGC_FAC gain(n-1) + (1-AGC_FAC)gain_in/gain_out */
  /* sig_out(n) = gain(n) sig_out(n)                                   */

  gain = past_gain;
  for(i=0; i<l_trm; i++) {
    gain = ((Word32)gain * (Word32)AGC_FAC) >> 15;
    gain += g0;
    sig_out[i] = (Word32)sig_out[i] * (Word32)gain >> 12;
  }
  past_gain = gain;
}

