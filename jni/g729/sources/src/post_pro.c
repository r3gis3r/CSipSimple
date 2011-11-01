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
 * Function Post_Process()                                                *
 *                                                                        *
 * Post-processing of output speech.                                      *
 *   - 2nd order high pass filter with cut off frequency at 100 Hz.       *
 *   - Multiplication by two of output speech with saturation.            *
 *-----------------------------------------------------------------------*/

#include "typedef.h"
#include "basic_op.h"
#include "oper_32b.h"

#include "ld8a.h"

#include "g729a_decoder.h"

/*------------------------------------------------------------------------*
 * 2nd order high pass filter with cut off frequency at 100 Hz.           *
 * Designed with SPPACK efi command -40 dB att, 0.25 ri.                  *
 *                                                                        *
 * Algorithm:                                                             *
 *                                                                        *
 *  y[i] = b[0]*x[i]   + b[1]*x[i-1]   + b[2]*x[i-2]                      *
 *                     + a[1]*y[i-1]   + a[2]*y[i-2];                     *
 *                                                                        *
 *     b[3] = {0.93980581E+00, -0.18795834E+01, 0.93980581E+00};          *
 *     a[3] = {0.10000000E+01, 0.19330735E+01, -0.93589199E+00};          *
 *-----------------------------------------------------------------------*/

/* Initialization of static values */

void Init_Post_Process(g729a_post_process_state *state)
{
  state->y2_hi = 0;
  state->y2_lo = 0;
  state->y1_hi = 0;
  state->y1_lo = 0;
//  state->y1 = 0LL;
//  state->y2 = 0LL;
  state->x1 = 0;
  state->x2 = 0;
}

/* acelp_high_pass_filter */
void Post_Process(
  g729a_post_process_state *state,
  Word16 sigin[],    /* input signal */
  Word16 sigout[],   /* output signal */
  Word16 lg)         /* length of signal    */
{
  Word16 i;
  Word32 L_tmp;

  for(i=0; i<lg; i++)
  {
     /*  y[i] = b[0]*x[i]   + b[1]*x[i-1]   + b[2]*x[i-2]    */
     /*                     + a[1]*y[i-1] + a[2] * y[i-2];      */
     L_tmp  = ((Word32) state->y1_hi) * 15836;
     L_tmp += (Word32)(((Word32) state->y1_lo * 15836) >> 15);
     L_tmp += ((Word32) state->y2_hi) * (-7667);
     L_tmp += (Word32)(((Word32) state->y2_lo * (-7667)) >> 15);
     L_tmp += 7699 * (sigin[i] - 2*state->x1/*signal[i-1]*/ + state->x2/*signal[i-2]*/);
    //L_tmp  = (state->y1 * 15836) >> 16;
    //L_tmp += (state->y2 * -7667) >> 16;
    //L_tmp += 7699 * (signal[i] - 2*state->x1/*signal[i-1]*/ + state->x2/*signal[i-2]*/);
    L_tmp  = L_shl(L_tmp, 3);

    state->x2 = state->x1;
    state->x1 = sigin[i];

     /* Multiplication by two of output speech with saturation. */
    sigout[i] = g_round(L_shl(L_tmp, 1));

    state->y2_hi = state->y1_hi;
    state->y2_lo = state->y1_lo;
    state->y1_hi = (Word16) (L_tmp >> 16);
    state->y1_lo = (Word16)((L_tmp >> 1) - (state->y1_hi << 15));
    //state->y2 = state->y1;
    //state->y1 = state->L_tmp;
  }
}
