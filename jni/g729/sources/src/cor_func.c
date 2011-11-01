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

/* Functions Corr_xy2() and Cor_h_x()   */

#include "typedef.h"
#include "basic_op.h"
#include "ld8a.h"

/*---------------------------------------------------------------------------*
 * Function corr_xy2()                                                       *
 * ~~~~~~~~~~~~~~~~~~~                                                       *
 * Find the correlations between the target xn[], the filtered adaptive      *
 * codebook excitation y1[], and the filtered 1st codebook innovation y2[].  *
 *   g_coeff[2]:exp_g_coeff[2] = <y2,y2>                                     *
 *   g_coeff[3]:exp_g_coeff[3] = -2<xn,y2>                                   *
 *   g_coeff[4]:exp_g_coeff[4] = 2<y1,y2>                                    *
 *---------------------------------------------------------------------------*/

void Corr_xy2(
      Word16 xn[],           /* (i) Q0  :Target vector.                  */
      Word16 y1[],           /* (i) Q0  :Adaptive codebook.              */
      Word16 y2[],           /* (i) Q12 :Filtered innovative vector.     */
      Word16 g_coeff[],      /* (o) Q[exp]:Correlations between xn,y1,y2 */
      Word16 exp_g_coeff[]   /* (o)       :Q-format of g_coeff[]         */
)
{
      Word16   i,exp;

      Word32   scaled_y2; /* Q9 */
      Word32   L_accy2y2, L_accxny2, L_accy1y2;

      L_accy2y2 = L_accxny2 = L_accy1y2 = 0;
      for(i=0; i<L_SUBFR; i++)
      {
        // Scale down y2[] from Q12 to Q9 to avoid overflow
        scaled_y2 = y2[i] >> 3;
        // Compute scalar product <y2[],y2[]>
        L_accy2y2 += scaled_y2 * scaled_y2;
        // Compute scalar product <xn[],y2[]>
        L_accxny2 += (Word32)xn[i] * scaled_y2;
        // Compute scalar product <y1[],y2[]>
        L_accy1y2 += (Word32)y1[i] * scaled_y2;
      }
      L_accy2y2 <<= 1; L_accy2y2 +=1; /* Avoid case of all zeros */
      L_accxny2 <<= 1; L_accxny2 +=1;
      L_accy1y2 <<= 1; L_accy1y2 +=1;

      exp            = norm_l_g729(L_accy2y2);
      g_coeff[2]     = g_round( L_accy2y2 << exp );
      exp_g_coeff[2] = exp + 3; //add(exp, 19-16);               /* Q[19+exp-16] */

      exp            = norm_l_g729(L_accxny2);
      g_coeff[3]     = negate(g_round( L_accxny2 << exp ));
      exp_g_coeff[3] = sub(add(exp, 10-16), 1);                  /* Q[10+exp-16] */

      exp            = norm_l_g729(L_accy1y2);
      g_coeff[4]     = g_round( L_accy1y2 << exp );
      exp_g_coeff[4] = sub(add(exp, 10-16), 1);                  /* Q[10+exp-16] */
}


/*--------------------------------------------------------------------------*
 *  Function  Cor_h_X()                                                     *
 *  ~~~~~~~~~~~~~~~~~~~                                                     *
 * Compute correlations of input response h[] with the target vector X[].   *
 *--------------------------------------------------------------------------*/

void Cor_h_X(
     Word16 h[],        /* (i) Q12 :Impulse response of filters      */
     Word16 X[],        /* (i)     :Target vector                    */
     Word16 D[]         /* (o)     :Correlations between h[] and D[] */
                        /*          Normalized to 13 bits            */
)
{
   Word16 i, j;
   Word32 s, max;
   Word32 y32[L_SUBFR];

   /* first keep the result on 32 bits and find absolute maximum */

   max = 0;

   for (i = 0; i < L_SUBFR; i++)
   {
     s = 0;
     for (j = i; j <  L_SUBFR; j++)
       s += (Word32)X[j] * h[j-i];
     s <<= 1;
     y32[i] = s;

     if (s < 0) s = -s;
     if(s > max) max = s;
   }

   /* Find the number of right shifts to do on y32[]  */
   /* so that maximum is on 13 bits                   */

   j = norm_l_g729(max);
   if( j > 16) {
    j = 16;
   }

   j = 18 - j;

   for(i=0; i<L_SUBFR; i++)
     D[i] = (Word16)(y32[i] >> j);
}

