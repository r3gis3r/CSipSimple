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
#include <pjsua-lib/pjsua_internal.h>
#include "pjsip_sipclf.h"

#define THIS_FILE "pjsip_sipclf.c"
#define TIMESTAMP_SEC_LEN 10
#define TIMESTAMP_MSEC_LEN 3

/* TODO : pass a max len for the line2 buffer */
static pj_status_t sipclf_add_timestamp(pj_str_t* line2, pj_time_val ts)
{
    char* line2ptr = line2->ptr;
    line2ptr += line2->slen;

    // sec since epoch
    pj_utoa_pad(ts.sec, line2ptr, TIMESTAMP_SEC_LEN, '0');
    line2->slen += TIMESTAMP_SEC_LEN;
    line2ptr += TIMESTAMP_SEC_LEN;
    // dot
    pj_strcat2(line2, ".");
    line2ptr++;
    // ms
    pj_utoa_pad(ts.msec, line2ptr, TIMESTAMP_MSEC_LEN, '0');
    line2->slen += TIMESTAMP_MSEC_LEN;
    line2ptr += TIMESTAMP_MSEC_LEN;

    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_index(pj_str_t* line1, int value)
{
    value += 61; /*the headers index length + \n*/
    pj_val_to_hex_digit( (value & 0xFF00) >> 8, line1->ptr + line1->slen);
    pj_val_to_hex_digit( (value & 0x00FF),          line1->ptr + line1->slen + 2);
    line1->slen += 4;
}

static pj_status_t sipclf_set_length(char* buf, int len)
{
    pj_val_to_hex_digit( (len & 0xFF0000) >> 16, buf);
    pj_val_to_hex_digit( (len & 0x00FF00) >> 8,    buf + 2);
    pj_val_to_hex_digit( (len & 0x0000FF),             buf + 4);
}

static pj_status_t sipclf_add_cseq(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_cseq_hdr *cseq;
    char buf[128];
    cseq = (const pjsip_cseq_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_CSEQ, NULL);
    pj_ansi_snprintf(buf, sizeof(buf), "%d %.*s", cseq->cseq, cseq->method.name.slen, cseq->method.name.ptr);
    pj_strcat2(line2, buf);
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_status_code(pj_str_t* line2, pjsip_msg* msg)
{
    if(msg->type == PJSIP_RESPONSE_MSG) {
        char buf[128];
        pj_ansi_snprintf(buf, sizeof(buf), "%d", msg->line.status.code);
        pj_strcat2(line2, buf);
    } else {
        pj_strcat2(line2, "-");
    }
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_request_uri(pj_str_t* line2, pjsip_msg* msg)
{
    int len = 0;
    if(msg->line.req.uri != NULL ){
        len = pjsip_uri_print(PJSIP_URI_IN_REQ_URI,
                                    msg->line.req.uri,
                                    (line2->ptr + line2->slen),
                                   512);
    }
    line2->slen += len;
    if(len == 0){
        pj_strcat2(line2, "-");
    }
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_tx_destination(pj_str_t* line2, pjsip_tx_data *tdata)
{
    char buf[128];
    pj_ansi_snprintf(buf, sizeof(buf), "%s:%d",
            tdata->tp_info.dst_name,
            tdata->tp_info.dst_port);
    pj_strcat2(line2, buf);
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_tx_source(pj_str_t* line2, pjsip_tx_data *tdata)
{
    char buf[128];
    pj_sockaddr_print( &tdata->tp_info.transport->local_addr,
                     buf, sizeof(buf),
                     1);
    pj_strcat2(line2, buf);
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_rx_destination(pj_str_t* line2, pjsip_rx_data *rdata)
{
    char buf[128];
    pj_sockaddr_print( &rdata->tp_info.transport->local_addr,
                     buf, sizeof(buf),
                     1);
    pj_strcat2(line2, buf);
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_rx_source(pj_str_t* line2, pjsip_rx_data *rdata)
{
    char buf[128];
    pj_sockaddr_print( &rdata->pkt_info.src_addr,
                     buf, sizeof(buf),
                     1);
    pj_strcat2(line2, buf);
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_fromto_url(pj_str_t* line2, const pjsip_fromto_hdr* fromto_hdr)
{
    int len = 0;
    if(fromto_hdr != NULL) {
        /*Use req uri context that does not add the display name and brackets stuff */
        len = pjsip_uri_print(PJSIP_URI_IN_REQ_URI,
                                    fromto_hdr->uri,
                                    (line2->ptr + line2->slen),
                                   512);
    }
    line2->slen += len;
    if(len == 0) {
        pj_strcat2(line2, "-");
    }
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_fromto_tag(pj_str_t* line2, const pjsip_fromto_hdr* fromto_hdr)
{
    if(fromto_hdr != NULL && fromto_hdr->tag.slen > 0) {
        pj_strcat(line2, &fromto_hdr->tag);
    } else {
        pj_strcat2(line2, "-");
    }
    return PJ_SUCCESS;
}

static pj_status_t sipclf_add_to_uri(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_fromto_hdr *to = (const pjsip_fromto_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_TO, NULL);
    return sipclf_add_fromto_url(line2, to);
}

static pj_status_t sipclf_add_to_tag(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_to_hdr *to = (const pjsip_to_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_TO, NULL);
    return sipclf_add_fromto_tag(line2, to);
}

static sipclf_add_from_uri(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_to_hdr *from = (const pjsip_to_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_FROM, NULL);
    return sipclf_add_fromto_url(line2, from);
}

static sipclf_add_from_tag(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_from_hdr *from = (const pjsip_from_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_FROM, NULL);
    return sipclf_add_fromto_tag(line2, from);
}

static sipclf_add_call_id(pj_str_t* line2, pjsip_msg* msg)
{
    const pjsip_call_info_hdr *call_id = (const pjsip_call_info_hdr*) pjsip_msg_find_hdr(msg, PJSIP_H_CALL_ID, NULL);
    if(call_id != NULL && call_id->hvalue.slen > 0) {
        pj_strcat(line2, &call_id->hvalue);
    } else {
        pj_strcat2(line2, "-");
    }
    return PJ_SUCCESS;
}


/* Notification on incoming messages */
static pj_bool_t logging_on_rx_msg(pjsip_rx_data *rdata)
{

    /* Actually first line should not be bigger than 60 bytes as per RFC def */
    char line1buf[100];
    /* TODO : Create a dedicated pool? */
    char line2buf[1024];
    pj_str_t line1 = {line1buf, 0};
    pj_str_t line2 = {line2buf, 0};

    /* Write in the first line */
    /* Version A */
    pj_strcat2(&line1, "A");
    /* Length of the record will be changed at the end */
    pj_strcat2(&line1, "000000");
    pj_strcat2(&line1, "\x2c");


    /* Start to write in the second line */
    pj_strcat2(&line2, "\x0a");
    sipclf_add_timestamp(&line2, rdata->pkt_info.timestamp);
    pj_strcat2(&line2, "\x09");

    /*  byte 1 -   R = Request /  r = Response */
    pj_strcat2(&line2,
                            (rdata->msg_info.msg->type == PJSIP_REQUEST_MSG) ? "R" : "r");

    /* byte 2 -   Retransmission Flag O = Original transmission /  D = Duplicate transmission / S = Server is stateless [i.e., retransmissions are not detected]*/
    /* TODO : implement that exactly */
    pj_strcat2(&line2, "S");

    /* byte 3 -   Sent/Received Flag S = Sent message / R = Received message */
    pj_strcat2(&line2, "R");

    /* byte 4 -   Transport Flag U = UDP /  T = TCP / S = SCTP */
    pj_strcat2(&line2,
                            (rdata->tp_info.transport->flag & PJSIP_TRANSPORT_RELIABLE) ? "T" : "U");

    /* byte 5 -   Encryption Flag E = Encrypted message (TLS, DTLS, etc.) / U = Unencrypted message */
    pj_strcat2(&line2,
                            (rdata->tp_info.transport->flag & PJSIP_TRANSPORT_SECURE) ? "E" : "U");

    pj_strcat2(&line2, "\x09");

    /* Mandatory fields */
    sipclf_add_index(&line1, line2.slen);
    sipclf_add_cseq(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_status_code(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_request_uri(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_rx_destination(&line2, rdata);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_rx_source(&line2, rdata);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_to_uri(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_to_tag(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_from_uri(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_from_tag(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_call_id(&line2, rdata->msg_info.msg);
    pj_strcat2(&line2, "\x09");

    /* Server-Txn */
    {
        char buf[64];
        pj_ansi_snprintf(buf, sizeof(buf), "%p", rdata);

        sipclf_add_index(&line1, line2.slen);
        pj_strcat2(&line2, buf);
        pj_strcat2(&line2, "\x09");

        /* Client-Txn */
        sipclf_add_index(&line1, line2.slen);
        pj_strcat2(&line2, buf);
        pj_strcat2(&line2, "\x0a");
    }

    /**
     * Optionnals -- we have nothing here for now
     * TODO : add body etc
     **/
    sipclf_add_index(&line1, line2.slen);


    sipclf_set_length((line1.ptr + 1),  line1.slen + line2.slen);


    PJ_LOG(4,(THIS_FILE, "%.*s%.*s", line1.slen, line1.ptr, line2.slen, line2.ptr));

    /* Always return false, otherwise messages will not get processed! */
    return PJ_FALSE;
}

/* Notification on outgoing messages */
static pj_status_t logging_on_tx_msg(pjsip_tx_data *tdata)
{
    /* Actually first line should not be bigger than 60 bytes as per RFC def */
    char line1buf[100];
    /* TODO : Create a dedicated pool? */
    char line2buf[1024];
    pj_str_t line1 = {line1buf, 0};
    pj_str_t line2 = {line2buf, 0};

    /* Write in the first line */
    /* Version A */
    pj_strcat2(&line1, "A");
    /* Length of the record will be changed at the end */
    pj_strcat2(&line1, "000000");
    pj_strcat2(&line1, "\x2c");


    /* Start to write in the second line */
    pj_strcat2(&line2, "\x0a");
    /* TODO : should use some information from tdata? */
    pj_time_val now;
    pj_gettimeofday(&now);
    sipclf_add_timestamp(&line2, now);
    pj_strcat2(&line2, "\x09");

    /*  byte 1 -   R = Request /  r = Response */
    pj_strcat2(&line2,
                            (tdata->msg->type == PJSIP_REQUEST_MSG) ? "R" : "r");

    /* byte 2 -   Retransmission Flag O = Original transmission /  D = Duplicate transmission / S = Server is stateless [i.e., retransmissions are not detected]*/
    /* TODO : implement that exactly */
    pj_strcat2(&line2, "S");

    /* byte 3 -   Sent/Received Flag S = Sent message / R = Received message */
    pj_strcat2(&line2, "S");

    /* byte 4 -   Transport Flag U = UDP /  T = TCP / S = SCTP */
    pj_strcat2(&line2,
                            (tdata->tp_info.transport->flag & PJSIP_TRANSPORT_RELIABLE) ? "T" : "U");

    /* byte 5 -   Encryption Flag E = Encrypted message (TLS, DTLS, etc.) / U = Unencrypted message */
    pj_strcat2(&line2,
                            (tdata->tp_info.transport->flag & PJSIP_TRANSPORT_SECURE) ? "E" : "U");

    pj_strcat2(&line2, "\x09");

    /* Mandatory fields */
    sipclf_add_index(&line1, line2.slen);
    sipclf_add_cseq(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_status_code(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_request_uri(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_tx_destination(&line2, tdata);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_tx_source(&line2, tdata);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_to_uri(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_to_tag(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_from_uri(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_from_tag(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    sipclf_add_index(&line1, line2.slen);
    sipclf_add_call_id(&line2, tdata->msg);
    pj_strcat2(&line2, "\x09");

    /* Server-Txn */
    sipclf_add_index(&line1, line2.slen);
    pj_strcat2(&line2, tdata->obj_name);
    pj_strcat2(&line2, "\x09");

    /* Client-Txn */
    sipclf_add_index(&line1, line2.slen);
    pj_strcat2(&line2, tdata->obj_name);
    pj_strcat2(&line2, "\x0a");

    /**
     * Optionnals -- we have nothing here for now
     * TODO : add body etc
     **/
    sipclf_add_index(&line1, line2.slen);


    sipclf_set_length((line1.ptr + 1),  line1.slen + line2.slen);


    PJ_LOG(4,(THIS_FILE, "%.*s%.*s", line1.slen, line1.ptr, line2.slen, line2.ptr));

    /* Always return success, otherwise message will not get sent! */
    return PJ_SUCCESS;
}

/* The module instance. */
static pjsip_module pjsua_sipclf_logger =
{
    NULL, NULL,             /* prev, next.      */
    { "mod-sipclf", 13 },        /* Name.        */
    -1,                 /* Id           */
    PJSIP_MOD_PRIORITY_TRANSPORT_LAYER-1,/* Priority            */
    NULL,               /* load()       */
    NULL,               /* start()      */
    NULL,               /* stop()       */
    NULL,               /* unload()     */
    &logging_on_rx_msg,         /* on_rx_request()  */
    &logging_on_rx_msg,         /* on_rx_response() */
    &logging_on_tx_msg,         /* on_tx_request.   */
    &logging_on_tx_msg,         /* on_tx_response() */
    NULL,               /* on_tsx_state()   */

};



PJ_DECL(pj_status_t) sipclf_mod_init() {
    return pjsip_endpt_register_module(pjsua_get_pjsip_endpt(),
                        &pjsua_sipclf_logger);
}
