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

#include "ld8a.h"
#include "tab_ld8a.h"

static void Gbk_presel(
   Word16 best_gain[],     /* (i) [0] Q9 : unquantized pitch gain     */
                           /* (i) [1] Q2 : unquantized code gain      */
   Word16 *cand1,          /* (o)    : index of best 1st stage vector */
   Word16 *cand2,          /* (o)    : index of best 2nd stage vector */
   Word16 gcode0           /* (i) Q4 : presearch for gain codebook    */
);


/*---------------------------------------------------------------------------*
 * Function  Qua_gain                                                        *
 * ~~~~~~~~~~~~~~~~~~                                                        *
 * Inputs:                                                                   *
 *   code[]     :Innovative codebook.                                        *
 *   g_coeff[]  :Correlations compute for pitch.                             *
 *   L_subfr    :Subframe length.                                            *
 *                                                                           *
 * Outputs:                                                                  *
 *   gain_pit   :Quantized pitch gain.                                       *
 *   gain_cod   :Quantized code gain.                                        *
 *                                                                           *
 * Return:                                                                   *
 *   Index of quantization.                                                  *
 *                                                                           *
 *--------------------------------------------------------------------------*/
Word16 Qua_gain(
   Word16 code[],       /* (i) Q13 :Innovative vector.             */
   Word16 g_coeff[],    /* (i)     :Correlations <xn y1> -2<y1 y1> */
                        /*            <y2,y2>, -2<xn,y2>, 2<y1,y2> */
   Word16 exp_coeff[],  /* (i)     :Q-Format g_coeff[]             */
   Word16 L_subfr,      /* (i)     :Subframe length.               */
   Word16 *gain_pit,    /* (o) Q14 :Pitch gain.                    */
   Word16 *gain_cod,    /* (o) Q1  :Code gain.                     */
   Word16 tameflag      /* (i)     : set to 1 if taming is needed  */
)
{
   Word16  i, j, index1, index2;
   Word16  cand1, cand2;
   Word16  exp, gcode0, exp_gcode0, gcode0_org, e_min ;
   Word16  nume, denom, inv_denom;
   Word16  exp1,exp2,exp_nume,exp_denom,exp_inv_denom,sft,tmp;
   Word16  g_pitch, g2_pitch, g_code, g2_code, g_pit_cod;
   Word16  coeff[5], coeff_lsf[5];
   Word16  exp_min[5];
   Word32  L_gbk12;
   Word32  L_tmp, L_dist_min, L_tmp1, L_tmp2, L_acc, L_accb;
   Word16  best_gain[2];

        /* Gain predictor, Past quantized energies = -14.0 in Q10 */

 static Word16 past_qua_en[4] = { -14336, -14336, -14336, -14336 };

  /*---------------------------------------------------*
   *-  energy due to innovation                       -*
   *-  predicted energy                               -*
   *-  predicted codebook gain => gcode0[exp_gcode0]  -*
   *---------------------------------------------------*/

   Gain_predict( past_qua_en, code, L_subfr, &gcode0, &exp_gcode0 );

  /*-----------------------------------------------------------------*
   *  pre-selection                                                  *
   *-----------------------------------------------------------------*/
  /*-----------------------------------------------------------------*
   *  calculate best gain                                            *
   *                                                                 *
   *  tmp = -1./(4.*coeff[0]*coeff[2]-coeff[4]*coeff[4]) ;           *
   *  best_gain[0] = (2.*coeff[2]*coeff[1]-coeff[3]*coeff[4])*tmp ;  *
   *  best_gain[1] = (2.*coeff[0]*coeff[3]-coeff[1]*coeff[4])*tmp ;  *
   *  gbk_presel(best_gain,&cand1,&cand2,gcode0) ;                   *
   *                                                                 *
   *-----------------------------------------------------------------*/

  /*-----------------------------------------------------------------*
   *  tmp = -1./(4.*coeff[0]*coeff[2]-coeff[4]*coeff[4]) ;           *
   *-----------------------------------------------------------------*/
   L_tmp1 = L_mult( g_coeff[0], g_coeff[2] );
   exp1   = add( add( exp_coeff[0], exp_coeff[2] ), 1-2 );
   L_tmp2 = L_mult( g_coeff[4], g_coeff[4] );
   exp2   = add( add( exp_coeff[4], exp_coeff[4] ), 1 );

   //if( sub(exp1, exp2)>0 ){
   if( exp1 > exp2 ){
      L_tmp = L_sub( L_shr( L_tmp1, sub(exp1,exp2) ), L_tmp2 );
      exp = exp2;
   }
   else{
      L_tmp = L_sub( L_tmp1, L_shr( L_tmp2, sub(exp2,exp1) ) );
      exp = exp1;
   }
   sft = norm_l_g729( L_tmp );
   denom = extract_h( L_shl(L_tmp, sft) );
   exp_denom = sub( add( exp, sft ), 16 );

   inv_denom = div_s_g729(16384,denom);
   inv_denom = negate( inv_denom );
   exp_inv_denom = sub( 14+15, exp_denom );

  /*-----------------------------------------------------------------*
   *  best_gain[0] = (2.*coeff[2]*coeff[1]-coeff[3]*coeff[4])*tmp ;  *
   *-----------------------------------------------------------------*/
   L_tmp1 = L_mult( g_coeff[2], g_coeff[1] );
   exp1   = add( exp_coeff[2], exp_coeff[1] );
   L_tmp2 = L_mult( g_coeff[3], g_coeff[4] );
   exp2   = add( add( exp_coeff[3], exp_coeff[4] ), 1 );

   //if( sub(exp1, exp2)>0 ){
   if (exp1 > exp2){
      L_tmp = L_sub( L_shr( L_tmp1, add(sub(exp1,exp2),1 )), L_shr( L_tmp2,1 ) );
      exp = sub(exp2,1);
   }
   else{
      L_tmp = L_sub( L_shr( L_tmp1,1 ), L_shr( L_tmp2, add(sub(exp2,exp1),1 )) );
      exp = sub(exp1,1);
   }
   sft = norm_l_g729( L_tmp );
   nume = extract_h( L_shl(L_tmp, sft) );
   exp_nume = sub( add( exp, sft ), 16 );

   sft = sub( add( exp_nume, exp_inv_denom ), (9+16-1) );
   L_acc = L_shr( L_mult( nume,inv_denom ), sft );
   best_gain[0] = extract_h( L_acc );             /*-- best_gain[0]:Q9 --*/

   if (tameflag == 1){
     //if(sub(best_gain[0], GPCLIP2) > 0) best_gain[0] = GPCLIP2;
     if(best_gain[0] > GPCLIP2) best_gain[0] = GPCLIP2;
   }

  /*-----------------------------------------------------------------*
   *  best_gain[1] = (2.*coeff[0]*coeff[3]-coeff[1]*coeff[4])*tmp ;  *
   *-----------------------------------------------------------------*/
   L_tmp1 = L_mult( g_coeff[0], g_coeff[3] );
   exp1   = add( exp_coeff[0], exp_coeff[3] ) ;
   L_tmp2 = L_mult( g_coeff[1], g_coeff[4] );
   exp2   = add( add( exp_coeff[1], exp_coeff[4] ), 1 );

   //if( sub(exp1, exp2)>0 ){
   if( exp1 > exp2 ){
      L_tmp = L_sub( L_shr( L_tmp1, add(sub(exp1,exp2),1) ), L_shr( L_tmp2,1 ) );
      exp = sub(exp2,1);
      //exp = exp2--;
   }
   else{
      L_tmp = L_sub( L_shr( L_tmp1,1 ), L_shr( L_tmp2, add(sub(exp2,exp1),1) ) );
      exp = sub(exp1,1);
      //exp = exp1--;
   }
   sft = norm_l_g729( L_tmp );
   nume = extract_h( L_shl(L_tmp, sft) );
   exp_nume = sub( add( exp, sft ), 16 );

   sft = sub( add( exp_nume, exp_inv_denom ), (2+16-1) );
   L_acc = L_shr( L_mult( nume,inv_denom ), sft );
   best_gain[1] = extract_h( L_acc );             /*-- best_gain[1]:Q2 --*/

   /*--- Change Q-format of gcode0 ( Q[exp_gcode0] -> Q4 ) ---*/
   //if( sub(exp_gcode0,4) >= 0 ){
   if (exp_gcode0 >=4) {
      gcode0_org = shr_g729( gcode0, sub(exp_gcode0,4) );
   }
   else{
      L_acc = L_deposit_l_g729( gcode0 );
      L_acc = L_shl( L_acc, sub( (4+16), exp_gcode0 ) );
      gcode0_org = extract_h( L_acc );              /*-- gcode0_org:Q4 --*/
   }

  /*----------------------------------------------*
   *   - presearch for gain codebook -            *
   *----------------------------------------------*/

   Gbk_presel(best_gain, &cand1, &cand2, gcode0_org );

/*---------------------------------------------------------------------------*
 *                                                                           *
 * Find the best quantizer.                                                  *
 *                                                                           *
 *  dist_min = MAX_32;                                                       *
 *  for ( i=0 ; i<NCAN1 ; i++ ){                                             *
 *    for ( j=0 ; j<NCAN2 ; j++ ){                                           *
 *      g_pitch = gbk1[cand1+i][0] + gbk2[cand2+j][0];                       *
 *      g_code = gcode0 * (gbk1[cand1+i][1] + gbk2[cand2+j][1]);             *
 *      dist = g_pitch*g_pitch * coeff[0]                                    *
 *           + g_pitch         * coeff[1]                                    *
 *           + g_code*g_code   * coeff[2]                                    *
 *           + g_code          * coeff[3]                                    *
 *           + g_pitch*g_code  * coeff[4] ;                                  *
 *                                                                           *
 *      if (dist < dist_min){                                                *
 *        dist_min = dist;                                                   *
 *        indice1 = cand1 + i ;                                              *
 *        indice2 = cand2 + j ;                                              *
 *      }                                                                    *
 *    }                                                                      *
 *  }                                                                        *
 *                                                                           *
 * g_pitch         = Q13                                                     *
 * g_pitch*g_pitch = Q11:(13+13+1-16)                                        *
 * g_code          = Q[exp_gcode0-3]:(exp_gcode0+(13-1)+1-16)                *
 * g_code*g_code   = Q[2*exp_gcode0-21]:(exp_gcode0-3+exp_gcode0-3+1-16)     *
 * g_pitch*g_code  = Q[exp_gcode0-5]:(13+exp_gcode0-3+1-16)                  *
 *                                                                           *
 * term 0: g_pitch*g_pitch*coeff[0] ;exp_min0 = 13             +exp_coeff[0] *
 * term 1: g_pitch        *coeff[1] ;exp_min1 = 14             +exp_coeff[1] *
 * term 2: g_code*g_code  *coeff[2] ;exp_min2 = 2*exp_gcode0-21+exp_coeff[2] *
 * term 3: g_code         *coeff[3] ;exp_min3 = exp_gcode0  - 3+exp_coeff[3] *
 * term 4: g_pitch*g_code *coeff[4] ;exp_min4 = exp_gcode0  - 4+exp_coeff[4] *
 *                                                                           *
 *---------------------------------------------------------------------------*/

   exp_min[0] = add( exp_coeff[0], 13 );
   exp_min[1] = add( exp_coeff[1], 14 );
   exp_min[2] = add( exp_coeff[2], sub( shl( exp_gcode0, 1 ), 21 ) );
   exp_min[3] = add( exp_coeff[3], sub( exp_gcode0, 3 ) );
   exp_min[4] = add( exp_coeff[4], sub( exp_gcode0, 4 ) );

   e_min = exp_min[0];
   for(i=1; i<5; i++){
      //if( sub(exp_min[i], e_min) < 0 ){
     if (exp_min[i] < e_min) {
         e_min = exp_min[i];
      }
   }

   /* align coeff[] and save in special 32 bit double precision */

   for(i=0; i<5; i++){
     j = sub( exp_min[i], e_min );
     L_tmp = (Word32)g_coeff[i] << 16;
     L_tmp = L_shr( L_tmp, j );          /* L_tmp:Q[exp_g_coeff[i]+16-j] */
     L_Extract( L_tmp, &coeff[i], &coeff_lsf[i] );          /* DPF */
   }

   /* Codebook search */

   L_dist_min = MAX_32;

   /* initialization used only to suppress Microsoft Visual C++  warnings */
   index1 = cand1;
   index2 = cand2;

if(tameflag == 1){
   for(i=0; i<NCAN1; i++){
      for(j=0; j<NCAN2; j++){
         g_pitch = add( gbk1[cand1+i][0], gbk2[cand2+j][0] );     /* Q14 */
         if(g_pitch < GP0999) {
         L_acc = L_deposit_l_g729( gbk1[cand1+i][1] );
         L_accb = L_deposit_l_g729( gbk2[cand2+j][1] );                /* Q13 */
         L_tmp = L_add( L_acc,L_accb );
         tmp = extract_l( L_shr( L_tmp,1 ) );                     /* Q12 */

         g_code   = mult( gcode0, tmp );         /*  Q[exp_gcode0+12-15] */
         g2_pitch = mult(g_pitch, g_pitch);                       /* Q13 */
         g2_code  = mult(g_code,  g_code);       /* Q[2*exp_gcode0-6-15] */
         g_pit_cod= mult(g_code,  g_pitch);      /* Q[exp_gcode0-3+14-15] */

         L_tmp = Mpy_32_16(coeff[0], coeff_lsf[0], g2_pitch);
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[1], coeff_lsf[1], g_pitch) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[2], coeff_lsf[2], g2_code) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[3], coeff_lsf[3], g_code) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[4], coeff_lsf[4], g_pit_cod) );
         L_tmp += Mpy_32_16(coeff[1], coeff_lsf[1], g_pitch);
         L_tmp += Mpy_32_16(coeff[2], coeff_lsf[2], g2_code);
         L_tmp += Mpy_32_16(coeff[3], coeff_lsf[3], g_code);
         L_tmp += Mpy_32_16(coeff[4], coeff_lsf[4], g_pit_cod);

         //L_temp = L_sub(L_tmp, L_dist_min);

         //if( L_temp < 0L ){
         if( L_tmp < L_dist_min ){
            L_dist_min = L_tmp;
            index1 = add(cand1,i);
            index2 = add(cand2,j);
         }
        }
      }
   }

}
else{
   for(i=0; i<NCAN1; i++){
      for(j=0; j<NCAN2; j++){
         g_pitch = add( gbk1[cand1+i][0], gbk2[cand2+j][0] );     /* Q14 */
         L_acc = L_deposit_l_g729( gbk1[cand1+i][1] );
         L_accb = L_deposit_l_g729( gbk2[cand2+j][1] );                /* Q13 */
         L_tmp = L_add( L_acc,L_accb );
         tmp = extract_l( L_shr( L_tmp,1 ) );                     /* Q12 */

         g_code   = mult( gcode0, tmp );         /*  Q[exp_gcode0+12-15] */
         g2_pitch = mult(g_pitch, g_pitch);                       /* Q13 */
         g2_code  = mult(g_code,  g_code);       /* Q[2*exp_gcode0-6-15] */
         g_pit_cod= mult(g_code,  g_pitch);      /* Q[exp_gcode0-3+14-15] */

         L_tmp = Mpy_32_16(coeff[0], coeff_lsf[0], g2_pitch);
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[1], coeff_lsf[1], g_pitch) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[2], coeff_lsf[2], g2_code) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[3], coeff_lsf[3], g_code) );
         //L_tmp = L_add(L_tmp, Mpy_32_16(coeff[4], coeff_lsf[4], g_pit_cod) );
         L_tmp += Mpy_32_16(coeff[1], coeff_lsf[1], g_pitch);
         L_tmp += Mpy_32_16(coeff[2], coeff_lsf[2], g2_code);
         L_tmp += Mpy_32_16(coeff[3], coeff_lsf[3], g_code);
         L_tmp += Mpy_32_16(coeff[4], coeff_lsf[4], g_pit_cod);

         if( L_tmp < L_dist_min ){
            L_dist_min = L_tmp;
            index1 = add(cand1,i);
            index2 = add(cand2,j);
         }

      }
   }
}
   /* Read the quantized gain */

  /*-----------------------------------------------------------------*
   * *gain_pit = gbk1[indice1][0] + gbk2[indice2][0];                *
   *-----------------------------------------------------------------*/
   *gain_pit = add( gbk1[index1][0], gbk2[index2][0] );      /* Q14 */

  /*-----------------------------------------------------------------*
   * *gain_code = (gbk1[indice1][1]+gbk2[indice2][1]) * gcode0;      *
   *-----------------------------------------------------------------*/
   L_gbk12 = (Word32)gbk1[index1][1] + (Word32)gbk2[index2][1]; /* Q13 */
   tmp = extract_l( L_shr( L_gbk12,1 ) );                     /* Q12 */
   L_acc = L_mult(tmp, gcode0);                /* Q[exp_gcode0+12+1] */

   L_acc = L_shl(L_acc, add( negate(exp_gcode0),(-12-1+1+16) ));
   *gain_cod = extract_h( L_acc );                             /* Q1 */

  /*----------------------------------------------*
   * update table of past quantized energies      *
   *----------------------------------------------*/
   Gain_update( past_qua_en, L_gbk12 );

   return( add( map1[index1]*(Word16)NCODE2, map2[index2] ) );

}
/*---------------------------------------------------------------------------*
 * Function Gbk_presel                                                       *
 * ~~~~~~~~~~~~~~~~~~~                                                       *
 *   - presearch for gain codebook -                                         *
 *---------------------------------------------------------------------------*/
static void Gbk_presel(
   Word16 best_gain[],     /* (i) [0] Q9 : unquantized pitch gain     */
                           /* (i) [1] Q2 : unquantized code gain      */
   Word16 *cand1,          /* (o)    : index of best 1st stage vector */
   Word16 *cand2,          /* (o)    : index of best 2nd stage vector */
   Word16 gcode0           /* (i) Q4 : presearch for gain codebook    */
)
{
   Word16    acc_h;
   Word16    sft_x,sft_y;
   Word32    L_acc,L_preg,L_cfbg,L_tmp,L_tmp_x,L_tmp_y;
   Word32 L_temp;

 /*--------------------------------------------------------------------------*
   x = (best_gain[1]-(coef[0][0]*best_gain[0]+coef[1][1])*gcode0) * inv_coef;
  *--------------------------------------------------------------------------*/
   L_cfbg = L_mult( coef[0][0], best_gain[0] );        /* L_cfbg:Q20 -> !!y */
   L_acc = L_shr( L_coef[1][1], 15 );                  /* L_acc:Q20     */
   L_acc = L_add( L_cfbg , L_acc );
   acc_h = extract_h( L_acc );                         /* acc_h:Q4      */
   L_preg = L_mult( acc_h, gcode0 );                   /* L_preg:Q9     */
   L_acc = L_shl( L_deposit_l_g729( best_gain[1] ), 7 );    /* L_acc:Q9      */
   L_acc = L_sub( L_acc, L_preg );
   acc_h = extract_h( L_shl( L_acc,2 ) );              /* L_acc_h:Q[-5] */
   L_tmp_x = L_mult( acc_h, INV_COEF );                /* L_tmp_x:Q15   */

 /*--------------------------------------------------------------------------*
   y = (coef[1][0]*(-coef[0][1]+best_gain[0]*coef[0][0])*gcode0
                                      -coef[0][0]*best_gain[1]) * inv_coef;
  *--------------------------------------------------------------------------*/
   L_acc = L_shr( L_coef[0][1], 10 );                  /* L_acc:Q20   */
   L_acc = L_sub( L_cfbg, L_acc );                     /* !!x -> L_cfbg:Q20 */
   acc_h = extract_h( L_acc );                         /* acc_h:Q4    */
   acc_h = mult( acc_h, gcode0 );                      /* acc_h:Q[-7] */
   L_tmp = L_mult( acc_h, coef[1][0] );                /* L_tmp:Q10   */

   L_preg = L_mult( coef[0][0], best_gain[1] );        /* L_preg:Q13  */
   L_acc = L_sub( L_tmp, L_shr(L_preg,3) );            /* L_acc:Q10   */

   acc_h = extract_h( L_shl( L_acc,2 ) );              /* acc_h:Q[-4] */
   L_tmp_y = L_mult( acc_h, INV_COEF );                /* L_tmp_y:Q16 */

   sft_y = (14+4+1)-16;         /* (Q[thr1]+Q[gcode0]+1)-Q[L_tmp_y] */
   sft_x = (15+4+1)-15;         /* (Q[thr2]+Q[gcode0]+1)-Q[L_tmp_x] */

   if(gcode0>0){
      /*-- pre select codebook #1 --*/
      *cand1 = 0 ;
      do{
         L_temp = L_sub( L_tmp_y, L_shr(L_mult(thr1[*cand1],gcode0),sft_y));
         if(L_temp >0L  ){
        //(*cand1) =add(*cand1,1);
           *cand1 += 1;
     }
         else               break ;
      //} while(sub((*cand1),(NCODE1-NCAN1))<0) ;
      } while ((*cand1) < (NCODE1-NCAN1));
      /*-- pre select codebook #2 --*/
      *cand2 = 0 ;
      do{
        L_temp = L_sub( L_tmp_x , L_shr(L_mult(thr2[*cand2],gcode0),sft_x));
         if( L_temp >0L) {
        //(*cand2) =add(*cand2,1);
           *cand2 += 1;
     }
         else               break ;
      //} while(sub((*cand2),(NCODE2-NCAN2))<0) ;
      } while((*cand2) < (NCODE2-NCAN2)) ;
   }
   else{
      /*-- pre select codebook #1 --*/
      *cand1 = 0 ;
      do{
        L_temp = L_sub(L_tmp_y ,L_shr(L_mult(thr1[*cand1],gcode0),sft_y));
         if( L_temp <0L){
        //(*cand1) =add(*cand1,1);
           *cand1 += 1;
     }
         else               break ;
      //} while(sub((*cand1),(NCODE1-NCAN1))) ;
      } while (*cand1 != (NCODE1-NCAN1)) ;
      /*-- pre select codebook #2 --*/
      *cand2 = 0 ;
      do{
         L_temp =L_sub(L_tmp_x ,L_shr(L_mult(thr2[*cand2],gcode0),sft_x));
         if( L_temp <0L){
        //(*cand2) =add(*cand2,1);
           *cand2 += 1;
     }
         else               break ;
      //} while(sub( (*cand2),(NCODE2-NCAN2))) ;
      } while(*cand2 != (NCODE2-NCAN2)) ;
   }

   return ;
}

