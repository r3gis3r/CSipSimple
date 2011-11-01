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

#ifndef __G729A_H__
#define __G729A_H__

#ifdef __cplusplus
extern "C" {
#endif

  /**
   * g729a_dec_mem_size:
   *
   * parameters:
   *    None
   *
   * return:
   *    Memory block size in bytes for decoder.
   */
Word32 g729a_dec_mem_size ();

  /**
   * g729a_dec_init:
   * Initializes resources needed for a new instance of the decoder.
   *
   * parameters:
   *    decState : Pre-allocated memory block of size defined by a call to
   *                <ref>g729a_dec_mem_size</ref>
   *
   * return:
   *    0, if an error occurs
   *    1, otherwise.
   */
Flag   g729a_dec_init     (void *decState);

  /**
   * g729a_dec_process
   * Decodes one frame of g729a encoded bitstream data.
   *
   * parameters:
   *    decState  :
   *    bitstream : Buffer containing one frame of g729a encoded bitstream data.
   *                (10 bytes)
   *    pcm       : Buffer containing one frame of pcm data. (80 bytes)
   *    badFrame  : set to 1 to indicate a bad frame to the decoder, 0 otherwise.
   *
   * return:
   *
   */
#if defined(CONTROL_OPT) && (CONTROL_OPT == 1)
void   g729a_dec_process  (void *decState, Word16 *bitstream, Word16 *pcm,
                           Flag badFrame);
#else
void   g729a_dec_process  (void *decState, UWord8 *bitstream, Word16 *pcm,
                           Flag badFrame);
#endif

  /**
   * g729a_dec_deinit
   *
   * parameters:
   *    decState :
   *
   * return:
   *    None
   */
void   g729a_dec_deinit   (void *decState);

  /**
   * g729a_enc_mem_size:
   *
   * parameters:
   *    None
   *
   * return:
   *    Memory block size in bytes for decoder.
   */
Word32 g729a_enc_mem_size ();

  /**
   * g729a_enc_init:
   * Initializes resources needed for a new instance of the encoder.
   *
   * parameters:
   *    encState : Pre-allocated memory block of size defined by a call to
   *               <ref>g729a_enc_mem_size</ref>
   *
   * return:
   *    0, if an error occurs
   *    1, otherwise.
   */
Flag   g729a_enc_init     (void *encState);

  /**
   * g729a_enc_process
   * Encode one frame of 16-bit linear PCM data.
   *
   * parameters:
   *    encState  :
   *    pcm       : Buffer containing one frame of pcm data. (80 bytes)
   *    bitstream : Buffer containing one frame of g729a encoded bitstream data.
   *                (10 bytes)
   *
   * return:
   *    None
   */
#if defined(CONTROL_OPT) && (CONTROL_OPT == 1)
void   g729a_enc_process  (void *encState, Word16 *pcm, Word16 *bitstream);
#else
void   g729a_enc_process  (void *encState, Word16 *pcm, UWord8 *bitstream);
#endif

  /**
   * g729a_enc_deinit
   *
   * parameters:
   *    encState :
   *
   * return:
   *    None
   */
void   g729a_enc_deinit   (void *encState);


#ifdef __cplusplus
  }
#endif


#endif /* __G729A_H__ */
