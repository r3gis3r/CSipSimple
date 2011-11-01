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

/*---------------------------------------------------------------------------*
 * Function  Dec_gain                                                        *
 * ~~~~~~~~~~~~~~~~~~                                                        *
 * Decode the pitch and codebook gains                                       *
 *                                                                           *
 *---------------------------------------------------------------------------*
 * input arguments:                                                          *
 *                                                                           *
 *   index      :Quantization index                                          *
 *   code[]     :Innovative code vector                                      *
 *   L_subfr    :Subframe size                                               *
 *   bfi        :Bad frame indicator                                         *
 *                                                                           *
 * output arguments:                                                         *
 *                                                                           *
 *   gain_pit   :Quantized pitch gain                                        *
 *   gain_cod   :Quantized codebook gain                                     *
 *                                                                           *
 *---------------------------------------------------------------------------*/
void Dec_gain(
   Word16 index,        /* (i)     :Index of quantization.         */
   Word16 code[],       /* (i) Q13 :Innovative vector.             */
   Word16 L_subfr,      /* (i)     :Subframe length.               */
   Word16 bfi,          /* (i)     :Bad frame indicator            */
   Word16 *gain_pit,    /* (o) Q14 :Pitch gain.                    */
   Word16 *gain_cod     /* (o) Q1  :Code gain.                     */
)
{
   Word16  index1, index2, tmp;
   Word16  gcode0, exp_gcode0;
   Word32  L_gbk12, L_acc;
   void    Gain_predict( Word16 past_qua_en[], Word16 code[], Word16 L_subfr,
                        Word16 *gcode0, Word16 *exp_gcode0 );
   void    Gain_update( Word16 past_qua_en[], Word32 L_gbk12 );
   void    Gain_update_erasure( Word16 past_qua_en[] );

        /* Gain predictor, Past quantized energies = -14.0 in Q10 */

   static Word16 past_qua_en[4] = { -14336, -14336, -14336, -14336 };


   /*-------------- Case of erasure. ---------------*/

   if(bfi != 0){
      *gain_pit = (Word16)((Word32)*gain_pit * 29491L >> 15);      /* *0.9 in Q15 */
      if (*gain_pit > 29491) *gain_pit = 29491;
      *gain_cod = (Word16)((Word32)*gain_cod * 32111L >> 15);      /* *0.98 in Q15 */

     /*----------------------------------------------*
      * update table of past quantized energies      *
      *                              (frame erasure) *
      *----------------------------------------------*/
      Gain_update_erasure(past_qua_en);

      return;
   }

   /*-------------- Decode pitch gain ---------------*/

   index1 = imap1[ index >> NCODE2_B ] ;
   index2 = imap2[ index & (NCODE2-1) ] ;
   *gain_pit = gbk1[index1][0] + gbk2[index2][0];

   /*-------------- Decode codebook gain ---------------*/

  /*---------------------------------------------------*
   *-  energy due to innovation                       -*
   *-  predicted energy                               -*
   *-  predicted codebook gain => gcode0[exp_gcode0]  -*
   *---------------------------------------------------*/

   Gain_predict( past_qua_en, code, L_subfr, &gcode0, &exp_gcode0 );

  /*-----------------------------------------------------------------*
   * *gain_code = (gbk1[indice1][1]+gbk2[indice2][1]) * gcode0;      *
   *-----------------------------------------------------------------*/

   L_gbk12 = (Word32)gbk1[index1][1] + (Word32)gbk2[index2][1]; /* Q13 */
   tmp = (Word16)(L_gbk12 >> 1);  /* Q12 */
   L_acc = tmp * gcode0 << 1;             /* Q[exp_gcode0+12+1] */

   L_acc = L_shl(L_acc, add( negate(exp_gcode0),(-12-1+1+16) ));
   *gain_cod = (Word16)(L_acc >> 16);                          /* Q1 */

  /*----------------------------------------------*
   * update table of past quantized energies      *
   *----------------------------------------------*/
   Gain_update( past_qua_en, L_gbk12 );
}

