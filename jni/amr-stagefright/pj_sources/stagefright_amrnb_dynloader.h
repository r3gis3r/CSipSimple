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

#include "opencore-amrnb/interf_dec.h"
#include "opencore-amrnb/interf_enc.h"
#include <pjmedia-codec/types.h>

/************* AMR defs ***************/
#define AMR_TX_WMF  0
#define AMR_TX_IF2  1
#define AMR_TX_ETS  2

enum Frame_Type_3GPP
{
    AMR_475 = 0,
    AMR_515,
    AMR_59,
    AMR_67,
    AMR_74,
    AMR_795,
    AMR_102,
    AMR_122,
    AMR_SID,
    GSM_EFR_SID,
    TDMA_EFR_SID,
    PDC_EFR_SID,
    FOR_FUTURE_USE1,
    FOR_FUTURE_USE2,
    FOR_FUTURE_USE3,
    AMR_NO_DATA
};


typedef enum {
    /*
* One word (2-byte) to indicate type of frame type.
* One word (2-byte) to indicate frame type.
* One word (2-byte) to indicate mode.
* N words (2-byte) containing N bits (bit 0 = 0xff81, bit 1 = 0x007f).
*/
    ETS = 0, /* Both AMR-Narrowband and AMR-Wideband */
    /*
* One word (2-byte) for sync word (good frames: 0x6b21, bad frames: 0x6b20)
* One word (2-byte) for frame length N.
* N words (2-byte) containing N bits (bit 0 = 0x007f, bit 1 = 0x0081).
*/
    ITU, /* AMR-Wideband */
    /*
* AMR-WB MIME/storage format, see RFC 3267 (sections 5.1 and 5.3) for details
*/
    MIME_IETF,

    WMF, /* AMR-Narrowband */

    IF2 /* AMR-Narrowband */

} bitstream_format;


/************************************************************/

struct stagefright_dynloader_amrnb_enc_handle {
    void* libEncode;

    pj_int16_t (* AMREncodeInit) (
            void **pEncStructure,
            void **pSidSyncStructure,
            int dtx_enable);

    pj_int16_t (* AMREncodeReset) (
            void *pEncStructure,
            void *pSidSyncStructure);

    void (* AMREncodeExit) (
            void **pEncStructure,
            void **pSidSyncStructure);

    pj_int16_t (* AMREncode) (
        void *pEncState,
        void *pSidSyncState,
        enum Mode mode,
        pj_int16_t *pEncInput,
        pj_uint8_t *pEncOutput,
        enum Frame_Type_3GPP *p3gpp_frame_type,
        pj_int16_t output_format
    );
};

struct stagefright_dynloader_amrnb_dec_handle {
    void* libDecode;

    /*
    * This function allocates memory for filter structure and initializes state
    * memory used by the GSM AMR decoder. This function returns zero. It will
    * return negative one if there is an error.
    */
    pj_int16_t (*GSMInitDecode) (
        void **state_data,
        char *id);
    /*
    * AMRDecode steps into the part of the library that decodes the raw data
    * speech bits for the decoding process. It returns the address offset of
    * the next frame to be decoded.
    */
    pj_int16_t (*AMRDecode) (
        void *state_data,
        enum Frame_Type_3GPP frame_type,
        pj_uint8_t *speech_bits_ptr,
        pj_int16_t *raw_pcm_buffer,
        pj_int16_t input_format
    );

    /*
    * This function resets the state memory used by the GSM AMR decoder. This
    * function returns zero. It will return negative one if there is an error.
    */
    pj_int16_t (* Speech_Decode_Frame_reset)(void *state_data);
    /*
    * This function frees up the memory used for the state memory of the
    * GSM AMR decoder.
    */
    void (* GSMDecodeFrameExit)(void **state_data);
};

pj_bool_t stagefright_dlsym_amrnb();

#endif /* AMR_STAGEFRIGHT_DYNLOADER_H_ */
