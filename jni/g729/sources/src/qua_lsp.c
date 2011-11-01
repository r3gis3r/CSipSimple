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

/*-------------------------------------------------------------------*
 * Function  Qua_lsp:                                                *
 *           ~~~~~~~~                                                *
 *-------------------------------------------------------------------*/
#include "typedef.h"
#include "basic_op.h"

#include "ld8a.h"
#include "tab_ld8a.h"

#include "g729a_encoder.h"

static void Lsp_qua_cs   (g729a_encoder_state *state, Word16 flsp_in[M], Word16 lspq_out[M], Word16 *code);
static void Relspwed     (Word16 lsp[], Word16 wegt[], Word16 lspq[],
                          Word16 lspcb1[][M], Word16 lspcb2[][M],
                          Word16 fg[MODE][MA_NP][M], Word16 freq_prev[MA_NP][M],
                          Word16 fg_sum[MODE][M], Word16 fg_sum_inv[MODE][M],
                          Word16 code_ana[]);
static void Get_wegt     (Word16 flsp[], Word16 wegt[]);
static void Lsp_get_tdist(Word16 wegt[], Word16 buf[], Word32 *L_tdist,
                          Word16 rbuf[], Word16 fg_sum[]);
static void Lsp_pre_select(Word16 rbuf[], Word16 lspcb1[][M], Word16 *cand);

void Qua_lsp(
             g729a_encoder_state *state,
  Word16 lsp[],       /* (i) Q15 : Unquantized LSP            */
  Word16 lsp_q[],     /* (o) Q15 : Quantized LSP              */
  Word16 ana[]        /* (o)     : indexes                    */
)
{
  Word16 lsf[M], lsf_q[M];  /* domain 0.0<= lsf <PI in Q13 */

  /* Convert LSPs to LSFs */
  Lsp_lsf2(lsp, lsf, M);

  Lsp_qua_cs(state, lsf, lsf_q, ana );

  /* Convert LSFs to LSPs */
  Lsf_lsp2(lsf_q, lsp_q, M);
}

/* static memory */
static Word16 freq_prev_reset[M] = {  /* Q13:previous LSP vector(init) */
  2339, 4679, 7018, 9358, 11698, 14037, 16377, 18717, 21056, 23396
};     /* PI*(float)(j+1)/(float)(M+1) */


void Lsp_encw_reset(
  g729a_encoder_state *state
)
{
  Word16 i;

  for(i=0; i<MA_NP; i++)
    Copy( &freq_prev_reset[0], &(state->freq_prev[i][0]), M );
}


void Lsp_qua_cs(
                g729a_encoder_state *state,
  Word16 flsp_in[M],    /* (i) Q13 : Original LSP parameters    */
  Word16 lspq_out[M],   /* (o) Q13 : Quantized LSP parameters   */
  Word16 *code          /* (o)     : codes of the selected LSP  */
)
{
  Word16 wegt[M];       /* Q11->normalized : weighting coefficients */

  Get_wegt( flsp_in, wegt );

  Relspwed( flsp_in, wegt, lspq_out, lspcb1, lspcb2, fg,
    state->freq_prev, fg_sum, fg_sum_inv, code);
}

void Relspwed(
  Word16 lsp[],                 /* (i) Q13 : unquantized LSP parameters */
  Word16 wegt[],                /* (i) norm: weighting coefficients     */
  Word16 lspq[],                /* (o) Q13 : quantized LSP parameters   */
  Word16 lspcb1[][M],           /* (i) Q13 : first stage LSP codebook   */
  Word16 lspcb2[][M],           /* (i) Q13 : Second stage LSP codebook  */
  Word16 fg[MODE][MA_NP][M],    /* (i) Q15 : MA prediction coefficients */
  Word16 freq_prev[MA_NP][M],   /* (i) Q13 : previous LSP vector        */
  Word16 fg_sum[MODE][M],       /* (i) Q15 : present MA prediction coef.*/
  Word16 fg_sum_inv[MODE][M],   /* (i) Q12 : inverse coef.              */
  Word16 code_ana[]             /* (o)     : codes of the selected LSP  */
)
{
  static const UWord8 gap[2]={GAP1, GAP2};
  Word16 mode, i, j;
  Word16 index1, index2, mode_index;
  Word16 cand[MODE], cand_cur;
  Word16 tindex1[MODE], tindex2[MODE];
  Word32 L_tdist[MODE];         /* Q26 */
  Word16 rbuf[M];               /* Q13 */
  Word16 buf[M];                /* Q13 */
  Word32 diff;

  Word32 L_dist1, L_dmin1;              /* Q26 */
  Word32 L_dist2, L_dmin2;              /* Q26 */
  Word16 tmp,tmp2;            /* Q13 */

  for(mode = 0; mode<MODE; mode++) {
    Lsp_prev_extract(lsp, rbuf, fg[mode], freq_prev, fg_sum_inv[mode]);

    Lsp_pre_select(rbuf, lspcb1, &cand_cur );
    cand[mode] = cand_cur;

 //   Lsp_select_1(rbuf, lspcb1[cand_cur], wegt, lspcb2, &index1);
 //   Lsp_select_2(rbuf, lspcb1[cand_cur], wegt, lspcb2, &index2);

    for ( j = 0 ; j < M ; j++ )
      buf[j] = rbuf[j] - lspcb1[cand_cur][j];

                     /* avoid the worst case. (all over flow) */
    index1 = 0;
    index2 = 0;
    L_dmin1 = MAX_32;
    L_dmin2 = MAX_32;
    for ( i = 0 ; i < NC1 ; i++ ) {
      L_dist1 = 0;
      L_dist2 = 0;
      for ( j = 0 ; j < NC ; j++ ) {
        tmp = sub(buf[j], lspcb2[i][j]);
        tmp2 = mult( wegt[j], tmp );
        L_dist1 += (Word32)tmp2 * tmp;
        tmp = sub(buf[j+NC], lspcb2[i][j+NC]);
        tmp2 = mult( wegt[j+NC], tmp );
        L_dist2 += (Word32)tmp2 * tmp;
      }
      L_dist1 <<= 1;
      L_dist2 <<= 1;

      if ( L_dist1 < L_dmin1) {
        L_dmin1 = L_dist1;
        index1 = i;
      }
      if ( L_dist2 < L_dmin2) {
        L_dmin2 = L_dist2;
        index2 = i;
      }
    }

    tindex1[mode] = index1;
    tindex2[mode] = index2;

    for( j = 0 ; j < NC ; j++ )
    {
      buf[j   ] = lspcb1[cand_cur][j   ] + lspcb2[index1][j];
      buf[j+NC] = lspcb1[cand_cur][j+NC] + lspcb2[index2][j+NC];
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

    Lsp_get_tdist(wegt, buf, &L_tdist[mode], rbuf, fg_sum[mode]);
  }

  //Lsp_last_select(L_tdist, &mode_index);
  mode_index = 0;
  if (L_tdist[1] < L_tdist[0])
    mode_index = 1;


  code_ana[0] = shl( mode_index,NC0_B ) | cand[mode_index];
  code_ana[1] = shl( tindex1[mode_index],NC1_B ) | tindex2[mode_index];

  Lsp_get_quant(lspcb1, lspcb2, cand[mode_index],
      tindex1[mode_index], tindex2[mode_index],
      fg[mode_index], freq_prev, lspq, fg_sum[mode_index]) ;
}

void Lsp_pre_select(
  Word16 rbuf[],              /* (i) Q13 : target vetor             */
  Word16 lspcb1[][M],         /* (i) Q13 : first stage LSP codebook */
  Word16 *cand                /* (o)     : selected code            */
)
{
  Word16 i, j;
  Word16 tmp;                 /* Q13 */
  Word32 L_dmin;              /* Q26 */
  Word32 L_tmp;               /* Q26 */

  /* avoid the worst case. (all over flow) */

  *cand = 0;
  L_dmin = MAX_32;
  for ( i = 0 ; i < NC0 ; i++ ) {
    L_tmp = 0;
    for ( j = 0 ; j < M ; j++ ) {
      tmp = rbuf[j] - lspcb1[i][j];
      L_tmp += (Word32)tmp * (Word32)tmp;
    }
    L_tmp <<= 1;

    if (L_tmp < L_dmin) {
      L_dmin = L_tmp;
      *cand = i;
    }
  }
}

void Lsp_get_tdist(
  Word16 wegt[],        /* (i) norm: weight coef.                */
  Word16 buf[],         /* (i) Q13 : candidate LSP vector        */
  Word32 *L_tdist,      /* (o) Q27 : distortion                  */
  Word16 rbuf[],        /* (i) Q13 : target vector               */
  Word16 fg_sum[]       /* (i) Q15 : present MA prediction coef. */
)
{
  Word16 j;
  Word16 tmp, tmp2;     /* Q13 */
  Word32 L_acc;         /* Q25 */

  *L_tdist = 0;
  for ( j = 0 ; j < M ; j++ ) {
    /* tmp = (buf - rbuf)*fg_sum */
    //tmp = sub( buf[j], rbuf[j] );
    //tmp = mult( tmp, fg_sum[j] );
    tmp = mult( sub( buf[j], rbuf[j] ), fg_sum[j] );

    /* *L_tdist += wegt * tmp * tmp */
    //L_acc = L_mult( wegt[j], tmp );
    L_acc = (Word32)wegt[j] * (Word32)tmp;
    tmp2 = extract_h( L_shl( L_acc, 5 ) );
    //*L_tdist = L_mac( *L_tdist, tmp2, tmp );
    *L_tdist += (Word32)tmp2 * (Word32)tmp;
  }
  *L_tdist <<=1;
}


#if 0
void Lsp_last_select(
  Word32 L_tdist[],     /* (i) Q27 : distortion         */
  Word16 *mode_index    /* (o)     : the selected mode  */
)
{
    Word32 L_temp;
  *mode_index = 0;
  L_temp =L_sub(L_tdist[1] ,L_tdist[0]);
  if (  L_temp<0L){
    *mode_index = 1;

}
#endif

void Get_wegt(
  Word16 flsp[],    /* (i) Q13 : M LSP parameters  */
  Word16 wegt[]     /* (o) Q11->norm : M weighting coefficients */
)
{
  Word16 i;
  Word16 tmp;
  Word32 L_acc;
  Word16 sft;

  tmp = flsp[1] - (PI04+8192);           /* 8192:1.0(Q13) */
  wegt[0] = 2048;                        /* 2048:1.0(Q11) */
  if (tmp < 0)
  {
    L_acc = (Word32)tmp*(Word32)tmp; /* L_acc in Q27 */
    tmp = (Word16)(L_acc >> 13);     /* tmp in Q13 */

    L_acc = (Word32)tmp * (Word32)CONST10;    /* L_acc in Q25 */
    tmp = (Word16)(L_acc >> 13);              /* tmp in Q11 */

    wegt[0] += tmp;                 /* wegt in Q11 */
  }

  for ( i = 1 ; i < M - 1 ; i++ ) {
      tmp = flsp[i+1] - flsp[i-1] - 8192;
      wegt[i] = 2048; /* 2048:1.0(Q11) */
      if (tmp < 0) {
        L_acc = (Word32)tmp*(Word32)tmp; /* L_acc in Q27 */
        tmp = (Word16)(L_acc >> 13);     /* tmp in Q13 */

        L_acc = (Word32)tmp * (Word32)CONST10;    /* L_acc in Q25 */
        tmp = (Word16)(L_acc >> 13);              /* tmp in Q11 */

        wegt[i] += tmp;                 /* wegt in Q11 */
      }
    }
  /* case M-1 */
  tmp = (PI92-8192) - flsp[M-2];
  wegt[M-1] = 2048;                        /* 2048:1.0(Q11) */
  if (tmp < 0)
  {
    L_acc = (Word32)tmp*(Word32)tmp; /* L_acc in Q27 */
    tmp = (Word16)(L_acc >> 13);     /* tmp in Q13 */

    L_acc = (Word32)tmp * (Word32)CONST10;    /* L_acc in Q25 */
    tmp = (Word16)(L_acc >> 13);              /* tmp in Q11 */

    wegt[M-1] += tmp;                 /* wegt in Q11 */
  }

  /* */
  L_acc = (Word32)wegt[4] * (Word32)CONST12;             /* L_acc in Q26 */
  wegt[4] = (Word16)(L_acc >> 14);       /* wegt in Q11 */

  L_acc = (Word32)wegt[5] * (Word32)CONST12;             /* L_acc in Q26 */
  wegt[5] = (Word16)(L_acc >> 14);       /* wegt in Q11 */


  /* wegt: Q11 -> normalized */
  tmp = 0;
  for ( i = 0; i < M; i++ ) {
    if ( wegt[i] > tmp) {
      tmp = wegt[i];
    }
  }

  sft = norm_s_g729(tmp);
  for ( i = 0; i < M; i++ ) {
    wegt[i] = shl(wegt[i], sft);                  /* wegt in Q(11+sft) */
  }
}

