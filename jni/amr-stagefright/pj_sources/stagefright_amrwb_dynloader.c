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
#include "opencore-amrwb/dec_if.h"
#include "opencore-amrwb/pvamrwbdecoder_api.h"
#include "vo-amrwbenc/enc_if.h"
#include "vo-amrwbenc/voAMRWB.h"
#include "vo-amrwbenc/cmnMemory.h"
#include "stagefright_amrwb_dynloader.h"
#include "homing_amr_wb_dec.h"


// Private AMR WRAPPER

static struct stagefright_dynloader_amrwb_enc_handle amrwb_enc_handle;
static struct stagefright_dynloader_amrwb_dec_handle amrwb_dec_handle;


static char* stagefright_amrwb_enc_candidates_libs[4] = {
        "libstagefright_soft_amrwbenc.so",
        "libstagefright_soft_amrdec.so",
        "libstagefright.so",
        "libomx_amrenc_sharedlibrary.so"
};

static char* stagefright_amrwb_dec_candidates_libs[3] = {
        "libstagefright_soft_amrdec.so",
        "libstagefright.so",
        "libomx_amrdec_sharedlibrary.so"
};

pj_bool_t stagefright_dlsym_amrwb(){

    int candidate;
    void* dyn_lib = NULL;
    pj_bool_t has_amrwb_enc = PJ_FALSE;
    pj_bool_t has_amrwb_dec = PJ_FALSE;
    stagefright_dlclose_amrwb();
    amrwb_enc_handle.libEncode = NULL;
    amrwb_dec_handle.libDecode = NULL;

    /*Resolve AMR WB encoder*/
    for(candidate=0; candidate < PJ_ARRAY_SIZE(stagefright_amrwb_enc_candidates_libs); candidate++){
        amrwb_enc_handle.libEncode = dlopen(stagefright_amrwb_enc_candidates_libs[candidate], RTLD_LAZY);
        if(amrwb_enc_handle.libEncode != NULL){
            amrwb_enc_handle.voGetAMRWBEncAPI = dlsym(amrwb_enc_handle.libEncode, "voGetAMRWBEncAPI");
        }
        if(amrwb_enc_handle.libEncode != NULL &&
                amrwb_enc_handle.voGetAMRWBEncAPI != NULL){
            has_amrwb_enc = PJ_TRUE;
            break;
        }
    }

    /*Resolve AMR NB decoder*/
    for(candidate=0; candidate < PJ_ARRAY_SIZE(stagefright_amrwb_dec_candidates_libs); candidate++){
        amrwb_dec_handle.libDecode = dlopen(stagefright_amrwb_dec_candidates_libs[candidate], RTLD_LAZY);
        if(amrwb_dec_handle.libDecode != NULL){
            amrwb_dec_handle.pvDecoder_AmrWb = dlsym(amrwb_dec_handle.libDecode, "pvDecoder_AmrWb");
            amrwb_dec_handle.pvDecoder_AmrWb_Init = dlsym(amrwb_dec_handle.libDecode, "pvDecoder_AmrWb_Init");
            amrwb_dec_handle.pvDecoder_AmrWbMemRequirements = dlsym(amrwb_dec_handle.libDecode, "pvDecoder_AmrWbMemRequirements");
            amrwb_dec_handle.pvDecoder_AmrWb_Reset = dlsym(amrwb_dec_handle.libDecode, "pvDecoder_AmrWb_Reset");
            amrwb_dec_handle.mime_unsorting = dlsym(amrwb_dec_handle.libDecode, "mime_unsorting");
        }
        if(amrwb_dec_handle.libDecode != NULL &&
                amrwb_dec_handle.pvDecoder_AmrWb != NULL &&
                amrwb_dec_handle.pvDecoder_AmrWb_Init != NULL &&
                amrwb_dec_handle.mime_unsorting != NULL &&
                amrwb_dec_handle.pvDecoder_AmrWbMemRequirements != NULL){
            has_amrwb_dec = PJ_TRUE;
            break;
        }
    }
    return has_amrwb_enc && has_amrwb_dec;
}

pj_status_t stagefright_dlclose_amrwb(){
    if(amrwb_enc_handle.libEncode != NULL){
        dlclose(amrwb_enc_handle.libEncode);
        amrwb_enc_handle.libEncode = NULL;
    }
    amrwb_enc_handle.voGetAMRWBEncAPI = NULL;

    if(amrwb_dec_handle.libDecode != NULL){
        dlclose(amrwb_dec_handle.libDecode );
        amrwb_dec_handle.libDecode  = NULL;
    }
    amrwb_dec_handle.pvDecoder_AmrWb = NULL;
    amrwb_dec_handle.pvDecoder_AmrWb_Init = NULL;
    amrwb_dec_handle.pvDecoder_AmrWbMemRequirements = NULL;
    amrwb_dec_handle.pvDecoder_AmrWb_Reset = NULL;
    return PJ_SUCCESS;
}

/* Implement opencore interface */

/*AMR WB ENCODER */

struct encoder_state {
    VO_AUDIO_CODECAPI audioApi;
    VO_HANDLE handle;
    VO_MEM_OPERATOR memOperator;
    VO_CODEC_INIT_USERDATA userData;
};

void* E_IF_init(void) {
    struct encoder_state* state = (struct encoder_state*) malloc(sizeof(struct encoder_state));
    int frameType = VOAMRWB_RFC3267;
    amrwb_enc_handle.voGetAMRWBEncAPI(&state->audioApi);
    state->memOperator.Alloc = cmnMemAlloc;
    state->memOperator.Copy = cmnMemCopy;
    state->memOperator.Free = cmnMemFree;
    state->memOperator.Set = cmnMemSet;
    state->memOperator.Check = cmnMemCheck;
    state->userData.memflag = VO_IMF_USERMEMOPERATOR;
    state->userData.memData = (VO_PTR)&state->memOperator;
    state->audioApi.Init(&state->handle, VO_AUDIO_CodingAMRWB, &state->userData);
    state->audioApi.SetParam(state->handle, VO_PID_AMRWB_FRAMETYPE, &frameType);
    return state;
}

void E_IF_exit(void* s) {
    struct encoder_state* state = (struct encoder_state*) s;
    state->audioApi.Uninit(state->handle);
    free(state);
}

int E_IF_encode(void* s, int mode, const short* speech, unsigned char* out, int dtx) {
    VO_CODECBUFFER inData, outData;
    VO_AUDIO_OUTPUTINFO outFormat;
    struct encoder_state* state = (struct encoder_state*) s;

    state->audioApi.SetParam(state->handle, VO_PID_AMRWB_MODE, &mode);
    state->audioApi.SetParam(state->handle, VO_PID_AMRWB_DTX, &dtx);
    inData.Buffer = (unsigned char*) speech;
    inData.Length = 640;
    outData.Buffer = out;
    state->audioApi.SetInputData(state->handle, &inData);
    state->audioApi.GetOutputData(state->handle, &outData, &outFormat);
    return outData.Length;
}

/*AMR WB DECODER*/

#define RX_SPEECH_GOOD 0
#define RX_SPEECH_PROBABLY_DEGRADED 1
#define RX_SPEECH_LOST 2
#define RX_SPEECH_BAD 3
#define RX_SID_FIRST 4
#define RX_SID_UPDATE 5
#define RX_SID_BAD 6
#define RX_NO_DATA 7
typedef struct
{
    pj_int16_t prev_ft;
    pj_int16_t prev_mode;
} RX_State;

struct state {
    void *st; /* State structure */
    unsigned char *pt_st;
    pj_int16_t *ScratchMem;

    pj_uint8_t* iInputBuf;
    pj_int16_t* iInputSampleBuf;
    pj_int16_t* iOutputBuf;

    pj_uint8_t quality;
    pj_int16_t mode;
    pj_int16_t mode_old;
    pj_int16_t frame_type;

    pj_int16_t reset_flag;
    pj_int16_t reset_flag_old;
    pj_int16_t status;
    RX_State rx_state;
};


void* D_IF_init(void){
    struct state* state = (struct state*) malloc(sizeof(struct state));
    memset(state, 0, sizeof(*state));

    state->iInputSampleBuf = (pj_int16_t*) malloc(sizeof(pj_int16_t)*KAMRWB_NB_BITS_MAX);
    state->reset_flag = 0;
    state->reset_flag_old = 1;
    state->mode_old = 0;
    state->rx_state.prev_ft = RX_SPEECH_GOOD;
    state->rx_state.prev_mode = 0;
    state->pt_st = (unsigned char*) malloc(amrwb_dec_handle.pvDecoder_AmrWbMemRequirements());

    amrwb_dec_handle.pvDecoder_AmrWb_Init(&state->st, state->pt_st, &state->ScratchMem);
    return state;
}

void D_IF_decode(void* s, const unsigned char* in, short* out, int bfi){
    struct state* state = (struct state*) s;
    pj_int16_t i;
    state->mode = (in[0] >> 3) & 0x0f;
    in++;

    state->quality = 1; /* ? */
    amrwb_dec_handle.mime_unsorting((pj_uint8_t*) in, state->iInputSampleBuf, &state->frame_type, &state->mode, state->quality, &state->rx_state);

    if ((state->frame_type == RX_NO_DATA) | (state->frame_type == RX_SPEECH_LOST)) {
    state->mode = state->mode_old;
    state->reset_flag = 0;
    } else {
    state->mode_old = state->mode;

    /* if homed: check if this frame is another homing frame */
    if (state->reset_flag_old == 1) {
    /* only check until end of first subframe */
    state->reset_flag = pvDecoder_AmrWb_homing_frame_test_first(state->iInputSampleBuf, state->mode);
    }
    }

    /* produce encoder homing frame if homed & input=decoder homing frame */
    if ((state->reset_flag != 0) && (state->reset_flag_old != 0)) {
    /* set homing sequence ( no need to decode anything */

    for (i = 0; i < AMR_WB_PCM_FRAME; i++) {
        out[i] = 0x0008; /* homing frame pattern                       */
    }
    } else {
    pj_int16_t frameLength;
    state->status = amrwb_dec_handle.pvDecoder_AmrWb(state->mode,
    state->iInputSampleBuf,
    out,
    &frameLength,
    state->st,
    state->frame_type,
    state->ScratchMem);
    }

    for (i = 0; i < AMR_WB_PCM_FRAME; i++) { /* Delete the 2 LSBs (14-bit output) */
    out[i] &= 0xfffC;
    }

    /* if not homed: check whether current frame is a homing frame */
    if (state->reset_flag_old == 0) {
    /* check whole frame */
    state->reset_flag = pvDecoder_AmrWb_homing_frame_test(state->iInputSampleBuf, state->mode);
    }
    /* reset decoder if current frame is a homing frame */
    if (state->reset_flag != 0) {
        amrwb_dec_handle.pvDecoder_AmrWb_Reset(state->st, 1);
    }
    state->reset_flag_old = state->reset_flag;
}

void D_IF_exit(void* s){
    struct state* state = (struct state*) s;
    free(state->pt_st);
    free(state->iInputSampleBuf);
    free(state);
}

