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

#ifndef __G729A_ENCODER_H__
#define __G729A_ENCODER_H__

#include "typedef.h"
#include "ld8a.h"

typedef struct g729a_pre_process_state
{
  Word16 y1_hi, y1_lo;
  Word16 y2_hi, y2_lo;
  Word16 x1, x2;
  //static Word32 y1, y2;
} g729a_pre_process_state;

typedef struct g729a_encoder_state
{
  /* Speech vector */
  Word16 old_speech[L_TOTAL];
  Word16 *speech, *p_window;
  Word16 *new_speech;                    /* Global variable */

  /* Weighted speech vector */
  Word16 old_wsp[L_FRAME+PIT_MAX];
  Word16 *wsp;

  /* Excitation vector */
  Word16 old_exc[L_FRAME+PIT_MAX+L_INTERPOL];
  Word16 *exc;

  /* Lsp (Line spectral pairs) */
  Word16 lsp_old[M]/*={
        30000, 26000, 21000, 15000, 8000, 0, -8000,-15000,-21000,-26000}*/;
  Word16 lsp_old_q[M];

  /* Filter's memory */
  Word16  mem_w0[M], mem_w[M], mem_zero[M];
  Word16  sharp;

  /* Sub state */
  // qua_lsp.c
  Word16 freq_prev[MA_NP][M];    /* Q13:previous LSP vector       */
  // taming.c
  Word32 L_exc_err[4];
} g729a_encoder_state;


typedef struct g729a_encode_frame_state
{
  g729a_encoder_state     encoderState;
  g729a_pre_process_state preProcessState;
} g729a_encode_frame_state;

#ifdef __cplusplus
extern "C" {
#endif

void Init_Coder_ld8a  (g729a_encoder_state *state);
void Coder_ld8a       (g729a_encoder_state *state,
                       Word16 ana[]);

void Init_Pre_Process(g729a_pre_process_state *state);
void Pre_Process     (g729a_pre_process_state *state,
                      Word16 sigin[],
                      Word16 sigout[],
                      Word16 lg);


void   Init_exc_err(g729a_encoder_state *state);
Word16 test_err    (g729a_encoder_state *state,
                    Word16 T0,
                    Word16 T0_frac);
void update_exc_err(g729a_encoder_state *state,
                    Word16 gain_pit,
                    Word16 T0);

void Lsp_encw_reset(g729a_encoder_state *state);
void Qua_lsp       (g729a_encoder_state *state,
                    Word16 lsp[],
                    Word16 lsp_q[],
                    Word16 ana[]);
  
#ifdef __cplusplus
  }
#endif


#endif /* __G729A_ENCODER_H__ */
