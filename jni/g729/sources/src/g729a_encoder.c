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

#include "typedef.h"
#include "g729a_encoder.h"

Word32 g729a_enc_mem_size ()
{
  return sizeof(g729a_encode_frame_state);
}

Flag   g729a_enc_init     (void *encState)
{
  g729a_encode_frame_state *state;
  if (encState == NULL)
    return 0;

  state = (g729a_encode_frame_state *)encState;

  Init_Pre_Process(&state->preProcessState);
  Init_Coder_ld8a(&state->encoderState);

  return 1;
}

#if defined(CONTROL_OPT) && (CONTROL_OPT == 1)
void   g729a_enc_process  (void *encState, Word16 *pcm, Word16 *bitstream)
#else
void   g729a_enc_process  (void *encState, Word16 *pcm, UWord8 *bitstream)
#endif
{
  g729a_encode_frame_state *state = (g729a_encode_frame_state *)encState;
  Word16 prm[PRM_SIZE];          /* Analysis parameters.                  */

  Pre_Process(&state->preProcessState, pcm, state->encoderState.new_speech, L_FRAME);

  Coder_ld8a(&state->encoderState, prm);

  prm2bits_ld8k( prm, bitstream);
}

void   g729a_enc_deinit   (void *encState)
{
  if (encState == NULL)
    return;

  bzero(encState,sizeof(g729a_encode_frame_state));
}
