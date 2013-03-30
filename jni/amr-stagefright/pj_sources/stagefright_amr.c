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
#include <pjmedia-codec/opencore_amr.h>
#include "stagefright_amr.h"
#include "stagefright_amrnb_dynloader.h"
#include "stagefright_amrwb_dynloader.h"

static int has_amr_nb = -1;
static int has_amr_wb = -1;



PJ_DEF(pj_status_t) pjmedia_codec_opencore_stagefright_init(pjmedia_endpt* endpt){
    if(has_amr_nb == -1){
        has_amr_nb = (int) stagefright_dlsym_amrnb();
    }
    if(has_amr_wb == -1){
        has_amr_wb = (int) stagefright_dlsym_amrwb();
    }
    int options = 0;
    if(!has_amr_nb){
        options |= PJMEDIA_AMR_NO_NB;
    }
    if(!has_amr_wb){
        options |= PJMEDIA_AMR_NO_WB;
    }
    pjmedia_codec_opencore_amr_init(endpt, options);
}
