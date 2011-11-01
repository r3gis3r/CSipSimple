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

/*-------------------------------------------------------------*
 *  Procedure Lsp_Az:                                          *
 *            ~~~~~~                                           *
 *   Compute the LPC coefficients from lsp (order=10)          *
 *-------------------------------------------------------------*/

#include "typedef.h"
#include "basic_op.h"
#include "oper_32b.h"

#include "ld8a.h"
#include "tab_ld8a.h"

/* local function */

static void Get_lsp_pol(Word16 *lsp, Word32 *f);

void Lsp_Az(
  Word16 lsp[],    /* (i) Q15 : line spectral frequencies            */
  Word16 a[]       /* (o) Q12 : predictor coefficients (order = 10)  */
)
{
  Word16 i;
  Word32 f1[6], f2[6];
  Word32 ff1, ff2, fff1, fff2;

  Get_lsp_pol(&lsp[0],f1);
  Get_lsp_pol(&lsp[1],f2);

  a[0] = 4096;
  for (i = 1; i <= 5; i++)
  {
    ff1 = f1[i] + f1[i-1];
    ff2 = f2[i] - f2[i-1];

    fff1 = ff1 + ff2 + ((Word32) 1 << 12);
    fff2 = ff1 - ff2 + ((Word32) 1 << 12);

    a[i]    = (Word16)(fff1 >> 13);
    a[11-i] = (Word16)(fff2 >> 13);
  }
}

static inline Word32 mull(Word32 a, Word16 b)
{
  register Word32 ra = a;
  register Word32 rb = b;
  Word32 lo, hi;

  __asm__("smull %0, %1, %2, %3     \n\t"
          "mov   %0, %0,     LSR #16 \n\t"
          "add   %1, %0, %1, LSL #16  \n\t"
          : "=&r"(lo), "=&r"(hi)
          : "r"(rb), "r"(ra));

  return hi;
}

/*-----------------------------------------------------------*
 * procedure Get_lsp_pol:                                    *
 *           ~~~~~~~~~~~                                     *
 *   Find the polynomial F1(z) or F2(z) from the LSPs        *
 *-----------------------------------------------------------*
 *                                                           *
 * Parameters:                                               *
 *  lsp[]   : line spectral freq. (cosine domain)    in Q15  *
 *  f[]     : the coefficients of F1 or F2           in Q24  *
 *-----------------------------------------------------------*/
static void Get_lsp_pol(
    Word16 *lsp,
    Word32 *f/*,
    Flag   *pOverflow*/)
{
    register Word16 i;
    register Word16 j;

    Word16 hi;
    Word16 lo;
    Word32 t0;
 //   OSCL_UNUSED_ARG(pOverflow);

    /* f[0] = 1.0;             */
    *f++ = (Word32) 0x01000000;
    *f++ = (Word32) - *(lsp++) << 10;       /* f[1] =  -2.0 * lsp[0];  */
    lsp++;                                  /* Advance lsp pointer     */

    for (i = 2; i <= 5; i++)
    {
        *f = *(f - 2);

        for (j = 1; j < i; j++)
        {
            hi = (Word16)(*(f - 1) >> 16);

            lo = (Word16)((*(f - 1) >> 1) - ((Word32) hi << 15));

            t0  = ((Word32)hi * *lsp);
            t0 += ((Word32)lo * *lsp) >> 15;

            *(f) +=  *(f - 2);          /*      *f += f[-2]      */
            *(f--) -=  t0 << 2;         /*      *f -= t0         */

        }

        *f -= (Word32)(*lsp++) << 10;

        f  += i;
        lsp++;
    }

    return;
}


/*___________________________________________________________________________
 |                                                                           |
 |   Functions : Lsp_lsf and Lsf_lsp                                         |
 |                                                                           |
 |      Lsp_lsf   Transformation lsp to lsf                                  |
 |      Lsf_lsp   Transformation lsf to lsp                                  |
 |---------------------------------------------------------------------------|
 |  Algorithm:                                                               |
 |                                                                           |
 |   The transformation from lsp[i] to lsf[i] and lsf[i] to lsp[i] are       |
 |   approximated by a look-up table and interpolation.                      |
 |___________________________________________________________________________|
*/
void Lsf_lsp2(
  Word16 lsf[],    /* (i) Q13 : lsf[m] (range: 0.0<=val<PI) */
  Word16 lsp[],    /* (o) Q15 : lsp[m] (range: -1<=val<1)   */
  Word16 m         /* (i)     : LPC order                   */
)
{
  Word16 i;
  UWord8 ind, offset;   /* in Q8 */
  Word16 freq;     /* normalized frequency in Q15 */

  for(i=0; i<m; i++)
  {
    freq = lsf[i] * 20861 >> 15;  /* 20861: 1.0/(2.0*PI) in Q17 */
    ind    = freq >> 8;           /* ind    = b8-b15 of freq */
    offset = freq;                /* offset = b0-b7  of freq */

    if ( ind > 63){
      ind = 63;                 /* 0 <= ind <= 63 */
    }

    lsp[i] = table2[ind]+ (slope_cos[ind]*offset >> 12);
  }
}



void Lsp_lsf2(
  Word16 lsp[],    /* (i) Q15 : lsp[m] (range: -1<=val<1)   */
  Word16 lsf[],    /* (o) Q13 : lsf[m] (range: 0.0<=val<PI) */
  Word16 m         /* (i)     : LPC order                   */
)
{
  Word16 i, ind;
  Word16 offset;   /* in Q15 */
  Word16 freq;     /* normalized frequency in Q16 */

  ind = 63;           /* begin at end of table2 -1 */

  for(i= m-(Word16)1; i >= 0; i--)
  {
    /* find value in table2 that is just greater than lsp[i] */
    for (; table2[ind] < lsp[i] && ind; ind--);

    offset = lsp[i] - table2[ind];

    /* acos(lsp[i])= ind*512 + (slope_acos[ind]*offset >> 11) */
    freq = (ind << 9) + (slope_acos[ind]*offset >> 11);
    lsf[i] = freq * 25736 >> 15;           /* 25736: 2.0*PI in Q12 */
  }
}

/*-------------------------------------------------------------*
 *  procedure Weight_Az                                        *
 *            ~~~~~~~~~                                        *
 * Weighting of LPC coefficients.                              *
 *   ap[i]  =  a[i] * (gamma ** i)                             *
 *                                                             *
 *-------------------------------------------------------------*/


void Weight_Az(
  Word16 a[],      /* (i) Q12 : a[m+1]  LPC coefficients             */
  Word16 gamma,    /* (i) Q15 : Spectral expansion factor.           */
  Word16 m,        /* (i)     : LPC order.                           */
  Word16 ap[]      /* (o) Q12 : Spectral expanded LPC coefficients   */
)
{
  Word16 i, fac;

  ap[0] = a[0];
  fac   = gamma;
  for(i=1; i<m; i++)
  {
    ap[i] = g_round( (Word32)a[i] * (Word32)fac << 1 );
    fac   = g_round( (Word32)fac * (Word32)gamma << 1 );
  }
  ap[m] = g_round( (Word32)a[m] * (Word32)fac << 1 );
}

/*----------------------------------------------------------------------*
 * Function Int_qlpc()                                                  *
 * ~~~~~~~~~~~~~~~~~~~                                                  *
 * Interpolation of the LPC parameters.                                 *
 *----------------------------------------------------------------------*/

/* Interpolation of the quantized LSP's */

void Int_qlpc(
 Word16 lsp_old[], /* input : LSP vector of past frame              */
 Word16 lsp_new[], /* input : LSP vector of present frame           */
 Word16 Az[]       /* output: interpolated Az() for the 2 subframes */
)
{
  Word16 i;
  Word16 lsp[M];

  /*  lsp[i] = lsp_new[i] * 0.5 + lsp_old[i] * 0.5 */
  for (i = 0; i < M; i++)
    lsp[i] = (lsp_new[i] >> 1) + (lsp_old[i] >> 1);


  Lsp_Az(lsp, Az);              /* Subframe 1 */

  Lsp_Az(lsp_new, &Az[MP1]);    /* Subframe 2 */
}


