/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of pjsip_android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef AMR_STAGEFRIGHT_DYNLOADER_H_
#define AMR_STAGEFRIGHT_DYNLOADER_H_

#include <pjmedia-codec/types.h>

/************* AMR defs ***************/

/************************************************************/

struct stagefright_dynloader_amrwb_enc_handle {
    void* libEncode;

    VO_S32 VO_API (*voGetAMRWBEncAPI)(
            VO_AUDIO_CODECAPI *pEncHandle);
};

struct stagefright_dynloader_amrwb_dec_handle {
    void* libDecode;

    void (*pvDecoder_AmrWb_Init)(
            void **spd_state,
            void *st,
            pj_int16_t ** ScratchMem);

    pj_int32_t (*pvDecoder_AmrWb)(
            pj_int16_t mode, /* input : used mode             */
            pj_int16_t prms[], /* input : parameter vector      */
            pj_int16_t synth16k[], /* output: synthesis speech      */
            pj_int16_t * frame_length, /* output:  lenght of the frame  */
            void *spd_state, /* i/o   : State structure       */
            pj_int16_t frame_type, /* input : received frame type   */
            pj_int16_t ScratchMem[]);

    void (*pvDecoder_AmrWb_Reset)(
            void *st,
            pj_int16_t reset_all);

    pj_int32_t (*pvDecoder_AmrWbMemRequirements)();

    void (*mime_unsorting)(pj_uint8_t packet[],
            pj_int16_t compressed_data[],
            pj_int16_t *frame_type,
            pj_int16_t *mode,
            pj_uint8_t q,
            void *st);

};

pj_bool_t stagefright_dlsym_amrwb();

#endif /* AMR_STAGEFRIGHT_DYNLOADER_H_ */
