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

#include <stdio.h>
#include "typedef.h"
#include "basic_op.h"
#include "ld8a.h"


/* lsf_decode */
void Lsp_get_quant(
  Word16 lspcb1[][M],      /* (i) Q13 : first stage LSP codebook      */
  Word16 lspcb2[][M],      /* (i) Q13 : Second stage LSP codebook     */
  Word16 code0,            /* (i)     : selected code of first stage  */
  Word16 code1,            /* (i)     : selected code of second stage */
  Word16 code2,            /* (i)     : selected code of second stage */
  Word16 fg[][M],          /* (i) Q15 : MA prediction coef.           */
  Word16 freq_prev[][M],   /* (i) Q13 : previous LSP vector           */
  Word16 lspq[],           /* (o) Q13 : quantized LSP parameters      */
  Word16 fg_sum[]          /* (i) Q15 : present MA prediction coef.   */
)
{
  static const UWord8 gap[2]={GAP1, GAP2};
  Word16 j, i;
  Word16 buf[M];           /* Q13 */
  Word32 diff, acc;

  for ( j = 0 ; j < NC ; j++ )
  {
    buf[j]    = lspcb1[code0][j]    + lspcb2[code1][j];
    buf[j+NC] = lspcb1[code0][j+NC] + lspcb2[code2][j+NC];
  }

  /* Lsp_expand_1_2 */
  for (i = 0; i < 2; ++i)
    for ( j = 1 ; j < M ; j++ )
    {
      diff = (buf[j-1] - buf[j] + gap[i]) >> 1;
      if ( diff > 0 )
      {
        buf[j-1] -= diff;
        buf[j]   += diff;
      }
    }

  /* Lsp_prev_compose
   * compose LSP parameter from elementary LSP with previous LSP. */
  for ( i = 0 ; i < M ; i++ ) {
    acc = buf[i] * fg_sum[i];
    for ( j = 0 ; j < MA_NP ; j++ )
      acc += freq_prev[j][i] * fg[j][i];

    lspq[i] = acc >> 15;
  }

  /* Lsp_prev_update */
  for ( j = MA_NP-1 ; j > 0 ; j-- )
    Copy(freq_prev[j-1], freq_prev[j], M);
  Copy(buf, freq_prev[0], M);

  Lsp_stability( lspq );
}

/*
  Functions which use previous LSP parameter (freq_prev).
*/

/*
  extract elementary LSP from composed LSP with previous LSP
*/
void Lsp_prev_extract(
  Word16 lsp[M],                /* (i) Q13 : unquantized LSP parameters  */
  Word16 lsp_ele[M],            /* (o) Q13 : target vector               */
  Word16 fg[MA_NP][M],          /* (i) Q15 : MA prediction coef.         */
  Word16 freq_prev[MA_NP][M],   /* (i) Q13 : previous LSP vector         */
  Word16 fg_sum_inv[M]          /* (i) Q12 : inverse previous LSP vector */
)
{
  Word16 j, k;
  Word32 L_temp;                /* Q19 */
  Word16 temp;                  /* Q13 */


  for ( j = 0 ; j < M ; j++ ) {
    L_temp = ((Word32)lsp[j]) << 15;
    for ( k = 0 ; k < MA_NP ; k++ )
      L_temp -= freq_prev[k][j] * fg[k][j];

    temp = (Word16)(L_temp >> 15);
    L_temp = ((Word32)temp * (Word32)fg_sum_inv[j]) >> 12;
    lsp_ele[j] = (Word16)L_temp;
  }
  return;
}


/*
  update previous LSP parameter
*/
void Lsp_prev_update(
  Word16 lsp_ele[M],             /* (i)   Q13 : LSP vectors           */
  Word16 freq_prev[MA_NP][M]     /* (i/o) Q13 : previous LSP vectors  */
)
{
  Word16 k;

  for ( k = MA_NP-1 ; k > 0 ; k-- )
    Copy(freq_prev[k-1], freq_prev[k], M);

  Copy(lsp_ele, freq_prev[0], M);
}

/* ff_acelp_reorder_lsf */
void Lsp_stability(
  Word16 buf[]       /* (i/o) Q13 : quantized LSP parameters      */
)
{
  Word16 j;
  Word16 tmp;
  Word32 L_diff;

  for(j=0; j<M-1; j++) {
    L_diff = buf[j+1] - buf[j];

    if( L_diff < 0L ) {
      /* exchange buf[j]<->buf[j+1] */
      tmp      = buf[j+1];
      buf[j+1] = buf[j];
      buf[j]   = tmp;
    }
  }

  if( buf[0] < L_LIMIT) {
    buf[0] = L_LIMIT;
  }

  for(j=0; j<M-1; j++) {
    L_diff = buf[j+1] - buf[j];

    if( L_diff < (Word32)GAP3) {
      buf[j+1] = buf[j] + GAP3;
    }
  }

  if( buf[M-1] > M_LIMIT) {
    buf[M-1] = M_LIMIT;
  }
}
