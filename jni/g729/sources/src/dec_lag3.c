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
 *    Function Dec_lag3                                                   *
 *             ~~~~~~~~                                                   *
 *   Decoding of fractional pitch lag with 1/3 resolution.                *
 * See "Enc_lag3.c" for more details about the encoding procedure.        *
 *------------------------------------------------------------------------*/

#include "typedef.h"

void Dec_lag3(
  Word16 index,       /* input : received pitch index           */
  Word16 pit_min,     /* input : minimum pitch lag              */
  Word16 pit_max,     /* input : maximum pitch lag              */
  Word16 i_subfr,     /* input : subframe flag                  */
  Word16 *T0,         /* output: integer part of pitch lag      */
  Word16 *T0_frac     /* output: fractional part of pitch lag   */
)
{
  Word16 i;
  Word16 T0_min, T0_max;

  if (i_subfr == 0)                  /* if 1st subframe */
  {
    if (index<  197)
    {
      /* *T0 = (index+2)/3 + 19 */
      *T0 = ((Word32)(index+2) * 10923) >> 15;
      *T0 += 19;

      /* *T0_frac = index - *T0*3 + 58 */
      i = *T0 + (*T0 << 1);
      *T0_frac = index - i + 58;
    }
    else
    {
      *T0 = index - 112;
      *T0_frac = 0;
    }

  }

  else  /* second subframe */
  {
    /* find T0_min and T0_max for 2nd subframe */
    T0_min = *T0 - 5;
    if (T0_min < pit_min)
    {
      T0_min = pit_min;
    }

    T0_max = T0_min + 9;
    if (T0_max > pit_max)
    {
      T0_max = pit_max;
      T0_min = T0_max - 9;
    }

    /* i = (index+2)/3 - 1 */
    /* *T0 = i + t0_min;    */
    i = ((Word32)(index + 2) * 10923LL) >> 15;
    i -= 1;
    *T0 = i + T0_min;

    /* t0_frac = index - 2 - i*3; */
    i = i + (i << 1);
    *T0_frac = index - 2 - i;
  }
}


