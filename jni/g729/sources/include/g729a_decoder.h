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

#ifndef __G729A_DECODER_H__
#define __G729A_DECODER_H__

#include "typedef.h"
#include "ld8a.h"

typedef struct g729a_decoder_state
{
  /* Excitation vector */
  Word16 old_exc[L_FRAME+PIT_MAX+L_INTERPOL];
  Word16 *exc;

  /* Lsp (Line spectral pairs) */
  Word16 lsp_old[M];

  /* Filter's memory */
  Word16 mem_syn[M];

  Word16 sharp;           /* pitch sharpening of previous frame */
  Word16 old_T0;          /* integer delay of previous frame    */
  Word16 gain_code;       /* Code gain                          */
  Word16 gain_pitch;      /* Pitch gain                         */

  /* Sub state */
  //lspdec.c
  Word16 freq_prev[MA_NP][M];   /* Q13 */
  Word16 prev_ma;                  /* previous MA prediction coef.*/
  Word16 prev_lsp[M];              /* previous LSP vector         */
} g729a_decoder_state;

typedef struct g729a_post_filter_state
{
  /* inverse filtered synthesis (with A(z/GAMMA2_PST))   */
  Word16 res2_buf[PIT_MAX+L_SUBFR];
  Word16 *res2;
  Word16 scal_res2_buf[PIT_MAX+L_SUBFR];
  Word16 *scal_res2;

  /* memory of filter 1/A(z/GAMMA1_PST) */
  Word16 mem_syn_pst[M];
} g729a_post_filter_state;

typedef struct g729a_post_process_state
{
  Word16 y1_hi, y1_lo;
  Word16 y2_hi, y2_lo;
  //Word32 y1, y2;
  Word16 x1, x2;
} g729a_post_process_state;

typedef struct g729a_decode_frame_state
{
  Word16 synth_buf[L_FRAME+M];
  Word16 *synth;

  g729a_decoder_state      decoderState;
  g729a_post_filter_state  postFilterState;
  g729a_post_process_state postProcessState;
} g729a_decode_frame_state;


#ifdef __cplusplus
extern "C" {
#endif

void Init_Decod_ld8a(g729a_decoder_state *state);

void Decod_ld8a(
    g729a_decoder_state *state,
    Word16  parm[],      /* (i)   : vector of synthesis parameters
                                    parm[0] = bad frame indicator (bfi)  */
    Word16  synth[],     /* (o)   : synthesis speech                     */
    Word16  A_t[],       /* (o)   : decoded LP filter in 2 subframes     */
    Word16  *T2,         /* (o)   : decoded pitch lag in 2 subframes     */
    Word16 bad_lsf       /* (i)   : bad LSF indicator                    */
  );

void Init_Post_Filter(g729a_post_filter_state *state);

void Post_Filter(
    g729a_post_filter_state *state,
    Word16 *syn,       /* in/out: synthesis speech (postfiltered is output)    */
    Word16 *Az_4,       /* input : interpolated LPC parameters in all subframes */
    Word16 *T            /* input : decoded pitch lags in all subframes          */
  );

void Init_Post_Process(g729a_post_process_state *state);

void Post_Process(
 g729a_post_process_state *state,
 Word16 sigin[],    /* input signal */
 Word16 sigout[],   /* output signal */
 Word16 lg          /* Length of signal    */
);


void D_lsp(
  g729a_decoder_state *state,
  Word16 prm[],          /* (i)     : indexes of the selected LSP */
  Word16 lsp_q[],        /* (o) Q15 : Quantized LSP parameters    */
  Word16 erase           /* (i)     : frame erase information     */
);

void Lsp_decw_reset(g729a_decoder_state *state);


#ifdef __cplusplus
  }
#endif


#endif /* __G729A_DECODER_H__ */
