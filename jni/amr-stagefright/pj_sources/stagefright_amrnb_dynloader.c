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
#include <dlfcn.h>
#include "opencore-amrnb/interf_enc.h"
#include "opencore-amrnb/interf_dec.h"
#include "stagefright_amrnb_dynloader.h"


// Private AMR WRAPPER

static struct stagefright_dynloader_amrnb_enc_handle amrnb_enc_handle;
static struct stagefright_dynloader_amrnb_dec_handle amrnb_dec_handle;


static char* stagefright_amrnb_enc_candidates_libs[3] = {
        "libstagefright_soft_amrnbenc.so",
        "libstagefright.so",
        "libomx_amrenc_sharedlibrary.so"
};

static char* stagefright_amrnb_dec_candidates_libs[3] = {
        "libstagefright_soft_amrdec.so",
        "libstagefright.so",
        "libomx_amrdec_sharedlibrary.so"
};

pj_bool_t stagefright_dlsym_amrnb(){

    int candidate;
    void* dyn_lib = NULL;
    pj_bool_t has_amrnb_enc = PJ_FALSE;
    pj_bool_t has_amrnb_dec = PJ_FALSE;
    stagefright_dlclose_amrnb();
    amrnb_enc_handle.libEncode = NULL;
    amrnb_dec_handle.libDecode = NULL;

    /*Resolve AMR NB encoder*/
    for(candidate=0; candidate < PJ_ARRAY_SIZE(stagefright_amrnb_enc_candidates_libs); candidate++){
        amrnb_enc_handle.libEncode = dlopen(stagefright_amrnb_enc_candidates_libs[candidate], RTLD_LAZY);
        if(amrnb_enc_handle.libEncode != NULL){
            amrnb_enc_handle.AMREncodeInit = dlsym(amrnb_enc_handle.libEncode, "AMREncodeInit");
            amrnb_enc_handle.AMREncodeReset = dlsym(amrnb_enc_handle.libEncode, "AMREncodeReset");
            amrnb_enc_handle.AMREncodeExit = dlsym(amrnb_enc_handle.libEncode, "AMREncodeExit");
            amrnb_enc_handle.AMREncode = dlsym(amrnb_enc_handle.libEncode, "AMREncode");
        }
        if(amrnb_enc_handle.libEncode != NULL &&
                amrnb_enc_handle.AMREncodeInit != NULL &&
                amrnb_enc_handle.AMREncodeExit != NULL &&
                amrnb_enc_handle.AMREncode != NULL){
            has_amrnb_enc = PJ_TRUE;
            break;
        }
    }

    /*Resolve AMR NB decoder*/
    for(candidate=0; candidate < PJ_ARRAY_SIZE(stagefright_amrnb_dec_candidates_libs); candidate++){
        amrnb_dec_handle.libDecode = dlopen(stagefright_amrnb_dec_candidates_libs[candidate], RTLD_LAZY);
        if(amrnb_dec_handle.libDecode != NULL){
            amrnb_dec_handle.GSMInitDecode = dlsym(amrnb_dec_handle.libDecode, "GSMInitDecode");
            amrnb_dec_handle.AMRDecode = dlsym(amrnb_dec_handle.libDecode, "AMRDecode");
            amrnb_dec_handle.Speech_Decode_Frame_reset = dlsym(amrnb_dec_handle.libDecode, "Speech_Decode_Frame_reset");
            amrnb_dec_handle.GSMDecodeFrameExit = dlsym(amrnb_dec_handle.libDecode, "GSMDecodeFrameExit");
        }
        if(amrnb_dec_handle.libDecode != NULL &&
                amrnb_dec_handle.GSMInitDecode != NULL &&
                amrnb_dec_handle.AMRDecode != NULL &&
                amrnb_dec_handle.Speech_Decode_Frame_reset != NULL &&
                amrnb_dec_handle.GSMDecodeFrameExit != NULL){
            has_amrnb_dec = PJ_TRUE;
            break;
        }
    }
    return has_amrnb_enc && has_amrnb_dec;
}

pj_status_t stagefright_dlclose_amrnb(){
    if(amrnb_enc_handle.libEncode != NULL){
        dlclose(amrnb_enc_handle.libEncode);
        amrnb_enc_handle.libEncode = NULL;
    }
    amrnb_enc_handle.AMREncodeReset = NULL;
    amrnb_enc_handle.AMREncodeExit = NULL;
    amrnb_enc_handle.AMREncode = NULL;
    amrnb_enc_handle.AMREncodeInit = NULL;

    if(amrnb_dec_handle.libDecode != NULL){
        dlclose(amrnb_dec_handle.libDecode );
        amrnb_dec_handle.libDecode  = NULL;
    }
    amrnb_dec_handle.GSMInitDecode = NULL;
    amrnb_dec_handle.AMRDecode = NULL;
    amrnb_dec_handle.GSMDecodeFrameExit = NULL;
    amrnb_dec_handle.Speech_Decode_Frame_reset = NULL;

    return PJ_SUCCESS;
}

/* Implement opencore interface */

/* AMR NB ENCODER */
struct encoder_state {
    void* encCtx;
    void* pidSyncCtx;
};

void* Encoder_Interface_init(int dtx) {
    struct encoder_state* state = (struct encoder_state*) malloc(sizeof(struct encoder_state));
    amrnb_enc_handle.AMREncodeInit(&state->encCtx, &state->pidSyncCtx, dtx);
    return state;
}

void Encoder_Interface_exit(void* s) {
    struct encoder_state* state = (struct encoder_state*) s;
    amrnb_enc_handle.AMREncodeExit(&state->encCtx, &state->pidSyncCtx);
    free(state);
}

int Encoder_Interface_Encode(void* s, enum Mode mode, const short* speech, unsigned char* out, int forceSpeech) {
    struct encoder_state* state = (struct encoder_state*) s;
    enum Frame_Type_3GPP frame_type = (enum Frame_Type_3GPP) mode;
    int ret = amrnb_enc_handle.AMREncode(state->encCtx, state->pidSyncCtx, mode, (pj_uint16_t*) speech, out, &frame_type, AMR_TX_WMF);
    out[0] = ((frame_type & 0x0f) << 3) | 0x04;
    return ret;
}

/* AMR NB DECODER */
void* Decoder_Interface_init() {
    void* ptr = NULL;
    amrnb_dec_handle.GSMInitDecode(&ptr, (pj_uint8_t*)"Decoder");
    return ptr;
}

void Decoder_Interface_exit(void* state) {
    amrnb_dec_handle.GSMDecodeFrameExit(&state);
}

void Decoder_Interface_Decode(void* state, const unsigned char* in, short* out, int bfi) {
    unsigned char type = (in[0] >> 3) & 0x0f;
    in++;
    amrnb_dec_handle.AMRDecode(state, (enum Frame_Type_3GPP) type, (pj_uint8_t*) in, out, MIME_IETF);
}
