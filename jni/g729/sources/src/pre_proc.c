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
 * Function Pre_Process()                                                 *
 *                                                                        *
 * Preprocessing of input speech.                                         *
 *   - 2nd order high pass filter with cut off frequency at 140 Hz.       *
 *   - Divide input by two.                                               *
 *-----------------------------------------------------------------------*/

#include "typedef.h"
#include "ld8a.h"
#include "basic_op.h"


/*------------------------------------------------------------------------*
 * 2nd order high pass filter with cut off frequency at 140 Hz.           *
 * Designed with SPPACK efi command -40 dB att, 0.25 ri.                  *
 *                                                                        *
 * Algorithm:                                                             *
 *                                                                        *
 *  y[i] = b[0]*x[i]/2 + b[1]*x[i-1]/2 + b[2]*x[i-2]/2                    *
 *                     + a[1]*y[i-1]   + a[2]*y[i-2];                     *
 *                                                                        *
 *     b[3] = {0.92727435E+00, -0.18544941E+01, 0.92727435E+00};          *
 *     a[3] = {0.10000000E+01, 0.19059465E+01, -0.91140240E+00};          *
 *                                                                        *
 *  Input are divided by two in the filtering process.                    *
 *-----------------------------------------------------------------------*/
#include "g729a_encoder.h"
/* Initialization of static values */

void Init_Pre_Process(g729a_pre_process_state *state)
{
  state->y2_hi = 0;
  state->y2_lo = 0;
  state->y1_hi = 0;
  state->y1_lo = 0;
 // state->y1 = 0;
 // state->y2 = 0;
  state->x1   = 0;
  state->x2   = 0;
}


void Pre_Process(
  g729a_pre_process_state *state,
  Word16 sigin[],    /* input signal */
  Word16 sigout[],   /* output signal */
  Word16 lg)          /* length of signal    */
{
  Word16 i;
  Word32 L_tmp;
  Word32 L_temp;

  for(i=0; i<lg; i++)
  {
     /*  y[i] = b[0]*x[i]/2 + b[1]*x[i-1]/2 + b140[2]*x[i-2]/2  */
     /*                     + a[1]*y[i-1] + a[2] * y[i-2];      */
     L_tmp     = ((Word32) state->y1_hi) * 7807;
     L_tmp    += (Word32)(((Word32) state->y1_lo * 7807) >> 15);
     //L_tmp = (y1 * 7807LL) >> 12;

     L_tmp    += ((Word32) state->y2_hi) * (-3733);
     L_tmp    += (Word32)(((Word32) state->y2_lo * (-3733)) >> 15);
     //L_tmp += (y2 * -3733LL) >> 12;

     L_tmp += 1899 * (sigin[i] - 2*state->x1/*signal[i-1]*/ + state->x2/*signal[i-2]*/);

     state->x2 = state->x1;
     state->x1 = sigin[i];
     //signal[i] = (Word16)((L_tmp + 0x800L) >> 12);

     state->y2_hi = state->y1_hi;
     state->y2_lo = state->y1_lo;
     //state->y2 = state->y1;

     L_temp = L_tmp;
     L_tmp = L_temp << 4;
     if (L_tmp >> 4 != L_temp)
     //y1 = L_tmp << 4;
     //if (y1 >> 4 != L_tmp)
     {
       sigout[i] = MIN_16;
       //y1 = (L_tmp & 0x80000000 ? MIN_32 : MAX_32);
       if (L_temp & 0x80000000)
       {
         state->y1_hi = MIN_16;
         state->y1_lo = 0x0000;
       }
       else
       {
         state->y1_hi = MAX_16;
         state->y1_lo = 0xffff;
       }
     }
     else
     {
       sigout[i] = (Word16)((L_tmp + 0x00008000) >> 16);
       state->y1_hi = (Word16) (L_tmp >> 16);
       state->y1_lo = (Word16)((L_tmp >> 1) - (state->y1_hi << 15));
     }
  }
}
