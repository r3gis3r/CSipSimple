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

/**************************************************************************
 * Taming functions.                                                      *
 **************************************************************************/

#include "typedef.h"
#include "basic_op.h"
#include "oper_32b.h"
#include "ld8a.h"
#include "tab_ld8a.h"

#include "g729a_encoder.h"

void Init_exc_err(g729a_encoder_state *state)
{
  Word16 i;
  for(i=0; i<4; i++) state->L_exc_err[i] = 0x00004000L;   /* Q14 */
}

/**************************************************************************
 * routine test_err - computes the accumulated potential error in the     *
 * adaptive codebook contribution                                         *
 **************************************************************************/

Word16 test_err(  /* (o) flag set to 1 if taming is necessary  */
 g729a_encoder_state *state,
 Word16 T0,       /* (i) integer part of pitch delay           */
 Word16 T0_frac   /* (i) fractional part of pitch delay        */
)
 {
    Word16 i, t1, zone1, zone2, flag;
    Word32 L_maxloc;

    /*if(T0_frac > 0) {
      t1 = T0 + 1;
    }
    else {
        t1 = T0;
    }*/
    t1 = T0 + (T0_frac > 0);

    i = t1 - (L_SUBFR+L_INTER10);
    if(i < 0) {
        i = 0;
    }
    zone1 = tab_zone[i];

    //i = add(t1, (L_INTER10 - 2));
    i = t1 + (L_INTER10 - 2);
    zone2 = tab_zone[i];

    L_maxloc = -1L;
    flag = 0 ;
    for(i=zone2; i>=zone1; i--) {
      if (state->L_exc_err[i] > L_maxloc) {
                L_maxloc = state->L_exc_err[i];
        }
    }

    if (L_maxloc > L_THRESH_ERR) {
        flag = 1;
    }

    return(flag);
}

/**************************************************************************
 *routine update_exc_err - maintains the memory used to compute the error *
 * function due to an adaptive codebook mismatch between encoder and      *
 * decoder                                                                *
 **************************************************************************/

void update_exc_err(
 g729a_encoder_state *state,
 Word16 gain_pit,      /* (i) pitch gain */
 Word16 T0             /* (i) integer part of pitch delay */
)
 {
    Word16 i, zone1, zone2, n;
    Word32 L_worst, L_temp;
    Word16 hi, lo;

    L_worst = -1L;
    n = T0 - L_SUBFR;

    if(n < 0) {
        hi = (Word16)(state->L_exc_err[0] >> 16);
        lo = (Word16)((state->L_exc_err[0] >> 1) - ((Word32)(hi) << 15));
        L_temp = (hi*gain_pit);
        L_temp += ((lo*gain_pit)>>15);
        L_temp <<= 2;
        L_temp = L_add(0x00004000L, L_temp);

        if(L_temp > L_worst) {
                L_worst = L_temp;
        }

        hi = (Word16)(L_temp >> 16);
        lo = (Word16)((L_temp >> 1) - ((Word32)(hi) << 15));
        L_temp = (hi*gain_pit);
        L_temp += ((lo*gain_pit)>>15);
        L_temp <<= 2;
        L_temp = L_add(0x00004000L, L_temp);
        if (L_temp > L_worst) {
                L_worst = L_temp;
        }
    }

    else {
        zone1 = tab_zone[n];

        i = T0 - 1;
        zone2 = tab_zone[i];

        for(i = zone1; i <= zone2; i++) {
          hi = (Word16)(state->L_exc_err[i] >> 16);
          lo = (Word16)((state->L_exc_err[i] >> 1) - ((Word32)(hi) << 15));
          L_temp = (hi*gain_pit);
          L_temp += ((lo*gain_pit)>>15);
          L_temp <<= 2;
          L_temp = L_add(0x00004000L, L_temp);
          if (L_temp > L_worst)
            L_worst = L_temp;
        }
    }

    for(i=3; i>=1; i--) {
        state->L_exc_err[i] = state->L_exc_err[i-1];
    }
    //L_exc_err[3] = L_exc_err[2];
    //L_exc_err[2] = L_exc_err[1];
    //L_exc_err[1] = L_exc_err[0];
    state->L_exc_err[0] = L_worst;
}
