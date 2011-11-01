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

/*------------------------------------------------------*
 * Parity_pitch - compute parity bit for first 6 MSBs   *
 *------------------------------------------------------*/

#include "typedef.h"
#include "ld8a.h"

Word16 Parity_Pitch(    /* output: parity bit (XOR of 6 MSB bits)    */
   Word16 pitch_index   /* input : index for which parity to compute */
)
{
  Word16 temp, sum, i, bit;

  temp = pitch_index >> 1;

  sum = 1;
  for (i = 0; i <= 5; i++) {
    temp >>= 1;
    bit = temp & (Word16)1;
    sum += bit;
  }
  sum = sum & (Word16)1;

  return sum;
 }

/*--------------------------------------------------------------------*
 * check_parity_pitch - check parity of index with transmitted parity *
 *--------------------------------------------------------------------*/

Word16  Check_Parity_Pitch( /* output: 0 = no error, 1= error */
  Word16 pitch_index,       /* input : index of parameter     */
  Word16 parity             /* input : parity bit             */
)
{
  Word16 temp, sum, i, bit;

  temp = pitch_index >> 1;

  sum = 1;
  for (i = 0; i <= 5; i++) {
    temp = temp >> 1;
    bit  = temp & (Word16)1;
    sum += bit;
  }
  sum += parity;
  sum = sum & (Word16)1;

  return sum;
}

