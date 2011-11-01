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

#include "typedef.h"
#include "basic_op.h"

#include "ld8a.h"
#include "tab_ld8a.h"

/*___________________________________________________________________________
 |                                                                           |
 |   Function Name : Pow2()                                                  |
 |                                                                           |
 |     L_x = pow(2.0, exponent.fraction)                                     |
 |---------------------------------------------------------------------------|
 |  Algorithm:                                                               |
 |                                                                           |
 |   The function Pow2(L_x) is approximated by a table and linear            |
 |   interpolation.                                                          |
 |                                                                           |
 |   1- i = bit10-b15 of fraction,   0 <= i <= 31                            |
 |   2- a = bit0-b9   of fraction                                            |
 |   3- L_x = tabpow[i]<<16 - (tabpow[i] - tabpow[i+1]) * a * 2                 |
 |   4- L_x = L_x >> (30-exponent)     (with rounding)                       |
 |___________________________________________________________________________|
*/


Word32 Pow2(        /* (o) Q0  : result       (range: 0<=val<=0x7fffffff) */
  Word16 exponent,  /* (i) Q0  : Integer part.      (range: 0<=val<=30)   */
  Word16 fraction   /* (i) Q15 : Fractional part.   (range: 0.0<=val<1.0) */
)
{
  Word16 exp, i, a, tmp;
  Word32 L_x;

  L_x = fraction<<6;
  /* Extract b0-b16 of fraction */

  i = ((Word16)(L_x >> 16)) & 31;             /* Extract b10-b15 of fraction */
  a = (Word16)((L_x >> 1) & 0x7fff);          /* Extract b0-b9   of fraction */

  L_x = ((Word32) tabpow[i] << 16);             /* tabpow[i] << 16       */

  tmp = tabpow[i] - tabpow[i + 1];
  L_x -= (((Word32) tmp) * a) << 1;  /* L_x -= tmp*a*2        */

  exp = 30 - exponent;
  L_x = L_x + ((Word32) 1 << (exp-1));
  L_x = (Word16)(L_x >> exp);

  return(L_x);
}

/*___________________________________________________________________________
 |                                                                           |
 |   Function Name : Log2()                                                  |
 |                                                                           |
 |       Compute log2(L_x).                                                  |
 |       L_x is positive.                                                    |
 |                                                                           |
 |       if L_x is negative or zero, result is 0.                            |
 |---------------------------------------------------------------------------|
 |  Algorithm:                                                               |
 |                                                                           |
 |   The function Log2(L_x) is approximated by a table and linear            |
 |   interpolation.                                                          |
 |                                                                           |
 |   1- Normalization of L_x.                                                |
 |   2- exponent = 30-exponent                                               |
 |   3- i = bit25-b31 of L_x,    32 <= i <= 63  ->because of normalization.  |
 |   4- a = bit10-b24                                                        |
 |   5- i -=32                                                               |
 |   6- fraction = tablog[i]<<16 - (tablog[i] - tablog[i+1]) * a * 2            |
 |___________________________________________________________________________|
*/

void Log2(
  Word32 L_x,       /* (i) Q0 : input value                                 */
  Word16 *exponent, /* (o) Q0 : Integer part of Log2.   (range: 0<=val<=30) */
  Word16 *fraction  /* (o) Q15: Fractional  part of Log2. (range: 0<=val<1) */
)
{
  Word16 exp, i, a, tmp;
  Word32 L_y;

  if( L_x <= (Word32)0 )
  {
    *exponent = 0;
    *fraction = 0;
    return;
  }

  exp = norm_l_g729(L_x);
  L_x <<= exp;               /* L_x is normalized */

  /* Calculate exponent portion of Log2 */
  *exponent = 30 - exp;

  /* At this point, L_x > 0       */
  /* Shift L_x to the right by 10 to extract bits 10-31,  */
  /* which is needed to calculate fractional part of Log2 */
  L_x >>= 10;
  i = (Word16)(L_x >> 15);    /* Extract b25-b31 */
  a = L_x & 0x7fff;           /* Extract b10-b24 of fraction */

  /* Calculate table index -> subtract by 32 is done for           */
  /* proper table indexing, since 32<=i<=63 (due to normalization) */
  i -= 32;

  /* Fraction part of Log2 is approximated by using table[]    */
  /* and linear interpolation, i.e.,                           */
  /* fraction = table[i]<<16 - (table[i] - table[i+1]) * a * 2 */
  L_y = (Word32) tablog[i] << 16;  /* table[i] << 16        */
  tmp = tablog[i] - tablog[i + 1];  /* table[i] - table[i+1] */
  L_y -= (((Word32) tmp) * a) << 1; /* L_y -= tmp*a*2        */

  *fraction = (Word16)(L_y >> 16);
}

/*___________________________________________________________________________
 |                                                                           |
 |   Function Name : Inv_sqrt                                                |
 |                                                                           |
 |       Compute 1/sqrt(L_x).                                                |
 |       L_x is positive.                                                    |
 |                                                                           |
 |       if L_x is negative or zero, result is 1 (3fff ffff).                |
 |---------------------------------------------------------------------------|
 |  Algorithm:                                                               |
 |                                                                           |
 |   The function 1/sqrt(L_x) is approximated by a table and linear          |
 |   interpolation.                                                          |
 |                                                                           |
 |   1- Normalization of L_x.                                                |
 |   2- If (30-exponent) is even then shift right once.                      |
 |   3- exponent = (30-exponent)/2  +1                                       |
 |   4- i = bit25-b31 of L_x,    16 <= i <= 63  ->because of normalization.  |
 |   5- a = bit10-b24                                                        |
 |   6- i -=16                                                               |
 |   7- L_y = tabsqr[i]<<16 - (tabsqr[i] - tabsqr[i+1]) * a * 2                 |
 |   8- L_y >>= exponent                                                     |
 |___________________________________________________________________________|
*/


Word32 Inv_sqrt(   /* (o) Q30 : output value   (range: 0<=val<1)           */
  Word32 L_x       /* (i) Q0  : input value    (range: 0<=val<=7fffffff)   */
)
{
  Word16 exp, i, a, tmp;
  Word32 L_y;

  if( L_x <= (Word32)0) return ( (Word32)0x3fffffffL);

  exp = norm_l_g729(L_x);
  L_x <<= exp;               /* L_x is normalize */

  exp = 30 - exp;
  //if( (exp & 1) == 0 )                  /* If exponent even -> shift right */
      //L_x >>= 1;

  L_x >>= (10 - (exp & 1));

  exp >>= 1;
  exp += 1;

  //L_x >>= 9;
  i = (Word16)(L_x >> 16);        /* Extract b25-b31 */
  a = (Word16)(L_x >> 1);         /* Extract b10-b24 */
  a &= (Word16) 0x7fff;

  i   -= 16;

  L_y = (Word32)tabsqr[i] << 16;    /* inv_sqrt_tbl[i] << 16    */
  tmp =  tabsqr[i] - tabsqr[i + 1];
  L_y -= ((Word32)tmp * a) << 1;        /* L_y -=  tmp*a*2         */

  L_y >>= exp;                /* denormalization */

  return(L_y);
}


