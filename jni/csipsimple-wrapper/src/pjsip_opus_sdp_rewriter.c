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
/**
 * This module aims to implements rfc6873
 *
 */
#include <pjsip.h>
#include <pjsip_ua.h>
#include <pjlib-util.h>
#include <pjlib.h>
#include <pjlib.h>
#include <pjsua.h>
#include <pjmedia/sdp.h>
#include <pjsua-lib/pjsua_internal.h>
#include "pjsip_opus_sdp_rewriter.h"

#define THIS_FILE "pjsip_opus_sdp_rewriter.c"

/* MIME constants. */
static const pj_str_t STR_MIME_APP     = { "application", 11 };
static const pj_str_t STR_MIME_SDP = { "sdp", 3 };
static const pj_str_t STR_AUDIO = { "audio", 5};
static const pj_str_t STR_RTPMAP = { "rtpmap", 6 };
static const pj_str_t STR_OPUS = { "opus", 4 };
static const pj_str_t STR_OPUS_RFC_FMT = { "opus/48000/2", 12 };

static unsigned pjopus_internal_clockrate;

static pj_bool_t opus_sdp_body_is_sdp(pjsip_msg_body* body){
    return (body && pj_stricmp(&body->content_type.type, &STR_MIME_APP)==0 &&
            pj_stricmp(&body->content_type.subtype, &STR_MIME_SDP)==0);
}

static pjmedia_sdp_attr* opus_sdp_get_opus_rtpmap_attr(pjmedia_sdp_session* sdp_session){

    unsigned media_idx;
    unsigned attr_idx;
    for(media_idx = 0; media_idx < sdp_session->media_count; media_idx++){
        pjmedia_sdp_media *media = sdp_session->media[media_idx];

        if(pj_stricmp(&media->desc.media, &STR_AUDIO)==0){
            for(attr_idx=0; attr_idx < media->attr_count; attr_idx++){
                if(pj_stricmp(&media->attr[attr_idx]->name, &STR_RTPMAP)==0){
                    char* found_opus = pj_stristr(&media->attr[attr_idx]->value, &STR_OPUS);
                    if(found_opus != NULL){
                        return media->attr[attr_idx];
                    }
                }
            }
        }
    }
    return NULL;
}

/* Notification on incoming messages */
static pj_bool_t opus_sdp_on_rx_msg(pjsip_rx_data *rdata)
{

    if(rdata && rdata->msg_info.msg){
        pjsip_msg_body* body = rdata->msg_info.msg->body;
        if(opus_sdp_body_is_sdp(body)){
            pj_str_t body_str = {body->data, body->len};
            char* found_opus = pj_stristr(&body_str, &STR_OPUS_RFC_FMT);
            if(found_opus != NULL){
                pj_str_t new_value;
                new_value.ptr = (char*) pj_pool_alloc(rdata->tp_info.pool, STR_OPUS_RFC_FMT.slen+1);
                new_value.slen = pj_ansi_snprintf(new_value.ptr, STR_OPUS_RFC_FMT.slen+1, "opus/%d/1", pjopus_internal_clockrate);
                while(new_value.slen < STR_OPUS_RFC_FMT.slen+1){
                    new_value.ptr[new_value.slen ++] = ' ';
                }
                pj_memcpy(found_opus, new_value.ptr, new_value.slen - 1);
            }
        }
    }

    /* Always return false, otherwise messages will not get processed! */
    return PJ_FALSE;
}

/* Notification on outgoing messages */
static pj_status_t opus_sdp_on_tx_msg(pjsip_tx_data *tdata)
{
    if(tdata && tdata->msg){
        pjsip_msg_body* body = tdata->msg->body;
        if (opus_sdp_body_is_sdp(body))
        {
            pjsip_msg_body* new_body = pjsip_msg_body_clone(tdata->pool, body);
            pjmedia_sdp_attr* opus_attr = opus_sdp_get_opus_rtpmap_attr((pjmedia_sdp_session*) new_body->data);
            if(opus_attr != NULL){
                pj_str_t new_value;
                char* found_opus = pj_stristr(&opus_attr->value, &STR_OPUS);
                new_value.ptr = (char*) pj_pool_alloc(tdata->pool, 20);
                new_value.slen = pj_ansi_snprintf(new_value.ptr, 20, "%.*s%.*s",
                                                                                                                found_opus - opus_attr->value.ptr, opus_attr->value.ptr,
                                                                                                                STR_OPUS_RFC_FMT.slen, STR_OPUS_RFC_FMT.ptr);
                opus_attr->value = new_value;
                tdata->msg->body = new_body;
            }
        }
    }

    /* Always return success, otherwise message will not get sent! */
    return PJ_SUCCESS;
}

/* The module instance. */
static pjsip_module pjsua_opus_sdp_rewriter =
{
    NULL, NULL,             /* prev, next.      */
    { "mod-opus-sdp-rewriter", 21 },        /* Name.        */
    -1,                 /* Id           */
    PJSIP_MOD_PRIORITY_TRANSPORT_LAYER+1,/* Priority            */
    NULL,               /* load()       */
    NULL,               /* start()      */
    NULL,               /* stop()       */
    NULL,               /* unload()     */
    &opus_sdp_on_rx_msg,         /* on_rx_request()  */
    &opus_sdp_on_rx_msg,         /* on_rx_response() */
    &opus_sdp_on_tx_msg,         /* on_tx_request.   */
    &opus_sdp_on_tx_msg,         /* on_tx_response() */
    NULL,               /* on_tsx_state()   */

};



PJ_DECL(pj_status_t) pjsip_opus_sdp_rewriter_init(unsigned target_clock_rate) {
    if(target_clock_rate > 0 && target_clock_rate <= 48000){
        pjopus_internal_clockrate = target_clock_rate;
    }else{
        pjopus_internal_clockrate = 16000;
    }
    if(target_clock_rate != 48000){
        return pjsip_endpt_register_module(pjsua_get_pjsip_endpt(),
                            &pjsua_opus_sdp_rewriter);
    }
    return PJ_SUCCESS;
}
