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
#include "oper_32b.h"
/*___________________________________________________________________________
 |                                                                           |
 |  This file contains operations in double precision.                       |
 |  These operations are not standard double precision operations.           |
 |  They are used where single precision is not enough but the full 32 bits  |
 |  precision is not necessary. For example, the function Div_32() has a     |
 |  24 bits precision which is enough for our purposes.                      |
 |                                                                           |
 |  The double precision numbers use a special representation:               |
 |                                                                           |
 |     L_32 = hi<<16 + lo<<1                                                 |
 |                                                                           |
 |  L_32 is a 32 bit integer.                                                |
 |  hi and lo are 16 bit signed integers.                                    |
 |  As the low part also contains the sign, this allows fast multiplication. |
 |                                                                           |
 |      0x8000 0000 <= L_32 <= 0x7fff fffe.                                  |
 |                                                                           |
 |  We will use DPF (Double Precision Format )in this file to specify        |
 |  this special format.                                                     |
 |___________________________________________________________________________|
*/

/*___________________________________________________________________________
 |                                                                           |
 |  Function L_Extract()                                                     |
 |                                                                           |
 |  Extract from a 32 bit integer two 16 bit DPF.                            |
 |                                                                           |
 |  Arguments:                                                               |
 |                                                                           |
 |   L_32      : 32 bit integer.                                             |
 |               0x8000 0000 <= L_32 <= 0x7fff ffff.                         |
 |   hi        : b16 to b31 of L_32                                          |
 |   lo        : (L_32 - hi<<16)>>1                                          |
 |___________________________________________________________________________|
*/

void L_Extract(Word32 L_32, Word16 *hi, Word16 *lo)
{
  *hi  = extract_h(L_32);
  *lo  = extract_l( L_msu( L_shr(L_32, 1) , *hi, 16384));  /* lo = L_32>>1   */
  return;
}
/*___________________________________________________________________________
 |                                                                           |
 |  This file contains operations in double precision.                       |
 |  These operations are not standard double precision operations.           |
 |  They are used where single precision is not enough but the full 32 bits  |
 |  precision is not necessary. For example, the function Div_32() has a     |
 |  24 bits precision which is enough for our purposes.                      |
 |                                                                           |
 |  The double precision numbers use a special representation:               |
 |                                                                           |
 |     L_32 = hi<<16 + lo<<1                                                 |
 |                                                                           |
 |  L_32 is a 32 bit integer.                                                |
 |  hi and lo are 16 bit signed integers.                                    |
 |  As the low part also contains the sign, this allows fast multiplication. |
 |                                                                           |
 |      0x8000 0000 <= L_32 <= 0x7fff fffe.                                  |
 |                                                                           |
 |  We will use DPF (Double Precision Format )in this file to specify        |
 |  this special format.                                                     |
 |___________________________________________________________________________|
*/

/*___________________________________________________________________________
 |                                                                           |
 |   Function Name : Div_32                                                  |
 |                                                                           |
 |   Purpose :                                                               |
 |             Fractional integer division of two 32 bit numbers.            |
 |             L_num / L_denom.                                              |
 |             L_num and L_denom must be positive and L_num < L_denom.       |
 |             L_denom = denom_hi<<16 + denom_lo<<1                          |
 |             denom_hi is a normalize number.                               |
 |             The result is in Q30.                                         |
 |                                                                           |
 |   Inputs :                                                                |
 |                                                                           |
 |    L_num                                                                  |
 |             32 bit long signed integer (Word32) whose value falls in the  |
 |             range : 0x0000 0000 < L_num < L_denom                         |
 |                                                                           |
 |    L_denom = denom_hi<<16 + denom_lo<<1      (DPF)                        |
 |                                                                           |
 |       denom_hi                                                            |
 |             16 bit positive normalized integer whose value falls in the   |
 |             range : 0x4000 < hi < 0x7fff                                  |
 |       denom_lo                                                            |
 |             16 bit positive integer whose value falls in the              |
 |             range : 0 < lo < 0x7fff                                       |
 |                                                                           |
 |   Return Value :                                                          |
 |                                                                           |
 |    L_div                                                                  |
 |             32 bit long signed integer (Word32) whose value falls in the  |
 |             range : 0x0000 0000 <= L_div <= 0x7fff ffff.                  |
 |             It's a Q31 value                                              |
 |                                                                           |
 |  Algorithm:                                                               |
 |                                                                           |
 |  - find = 1/L_denom.                                                      |
 |      First approximation: approx = 1 / denom_hi                           |
 |      1/L_denom = approx * (2.0 - L_denom * approx )                       |
 |                                                                           |
 |  -  result = L_num * (1/L_denom)                                          |
 |___________________________________________________________________________|
*/

Word32 Div_32(Word32 L_num, Word16 denom_hi, Word16 denom_lo)
{
  Word16 approx, hi, lo, n_hi, n_lo;
  Word32 L_32;


  /* First approximation: 1 / L_denom = 1/denom_hi */

  approx = div_s_g729( (Word16)0x3fff, denom_hi);    /* result in Q14 */
                                                /* Note: 3fff = 0.5 in Q15 */

  /* 1/L_denom = approx * (2.0 - L_denom * approx) */

  L_32 = Mpy_32_16(denom_hi, denom_lo, approx); /* result in Q30 */

  L_32 = L_sub( MAX_32, L_32);      /* result in Q30 */

  hi = (Word16)(L_32 >> 16);
  lo = (L_32 >> 1) - (hi << 15);

  L_32 = Mpy_32_16(hi, lo, approx);             /* = 1/L_denom in Q29 */

  /* L_num * (1/L_denom) */
  hi = (Word16)(L_32 >> 16);
  lo = (L_32 >> 1) - (hi << 15);
  n_hi = (Word16)(L_num >> 16);
  n_lo = (L_num >> 1) - (n_hi << 15);
  L_32 = Mpy_32(n_hi, n_lo, hi, lo);            /* result in Q29   */
  L_32 = L_shl(L_32, 2);                        /* From Q29 to Q31 */

  return( L_32 );
}



