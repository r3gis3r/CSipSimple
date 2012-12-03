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
#include <pjsip.h>
#include <pjsip_ua.h>
#include <pjlib-util.h>
#include <pjlib.h>
#include <pjlib.h>
#include <pjsua.h>
#include <pjsua-lib/pjsua_internal.h>
#include "pjsip_mobile_reg_handler.h"

#define THIS_FILE "mobile_reg_handler.cpp"

static MobileRegHandlerCallback* registeredCallbackObject = NULL;

extern "C" {
void mobile_reg_handler_set_callback(MobileRegHandlerCallback* cb)
{
    registeredCallbackObject = cb;
}

static pj_bool_t mod_reg_tracker_same_contact_hdr(const pjsip_contact_hdr* hdr1, const pjsip_contact_hdr* hdr2)
{
    const pjsip_uri* uri1 = NULL, *uri2 = NULL;
    if (hdr1 == NULL || hdr2 == NULL) {
        return PJ_FALSE;
    }
    if (hdr1->uri == NULL || hdr2->uri == NULL) {
        return PJ_FALSE;
    }
    uri1 = (const pjsip_uri*) pjsip_uri_get_uri(hdr1->uri);
    uri2 = (const pjsip_uri*) pjsip_uri_get_uri(hdr2->uri);
    return (pjsip_uri_cmp(PJSIP_URI_IN_CONTACT_HDR, uri1, uri2) == 0);
}

static pj_bool_t mod_reg_tracker_on_rx_response(pjsip_rx_data *rdata)
{
    pjsua_acc_id acc_id;
    if(registeredCallbackObject == NULL){
        return PJ_FALSE;
    }
    PJ_LOG(4, (THIS_FILE, "mod_reg_tracker_on_rx_response"));
    if(rdata == NULL || rdata->msg_info.cseq == NULL || rdata->msg_info.msg == NULL){
        return PJ_FALSE;
    }

    if (rdata->msg_info.cseq->method.id == PJSIP_REGISTER_METHOD &&
            PJSIP_IS_STATUS_IN_CLASS(rdata->msg_info.msg->line.status.code, 200)) {
        PJ_LOG(4, (THIS_FILE, "mod_reg_tracker_on_rx_response 2"));
        const pjsip_hdr *hdr;
        pj_pool_t* pool;
        const pj_str_t STR_CONTACT = { (char*)"Contact", 7 };
        const pjsip_msg *msg = rdata->msg_info.msg;
        pj_str_t our_contact;
        const pjsip_contact_hdr* our_contact_hdr;

        PJ_LOG(4, (THIS_FILE, "Hook a REGISTER RX response !!!"));

        /* Don't retrieve contact from rdata since some contacts here may not be ours */
        acc_id = pjsua_acc_find_for_incoming(rdata);
        if(acc_id == PJSUA_INVALID_ID){
            /* Ignore if not a pjsua account */
            return PJ_FALSE;
        }
        our_contact = pjsua_var.acc[acc_id].contact;
        if (our_contact.slen == 0) {
            PJ_LOG(4, (THIS_FILE, " Hook should clear contact"));
            registeredCallbackObject->on_save_contact(acc_id, pj_str((char*)""), 0);
            return PJ_FALSE;
        }

        pool = pjsua_pool_create("mobile_tmp", 512, 512);
        our_contact_hdr = (pjsip_contact_hdr*) pjsip_parse_hdr(pool,
                &STR_CONTACT, our_contact.ptr, our_contact.slen, NULL);
        if (our_contact_hdr == NULL) {
            pj_pool_release(pool);
            return PJ_FALSE;
        }

        /* Enumerate all Contact headers in the response */
        for (hdr = msg->hdr.next; hdr != &msg->hdr; hdr = hdr->next) {
            if (hdr->type == PJSIP_H_CONTACT) {
                pjsip_contact_hdr* contact_hdr = (pjsip_contact_hdr*) hdr;
                if(mod_reg_tracker_same_contact_hdr(contact_hdr, our_contact_hdr)){
                    /* Found the matching one ! */
                    PJ_LOG(4, (THIS_FILE, " Hook should save contact : %.*s > %d", our_contact.slen, our_contact.ptr, contact_hdr->expires));
                    registeredCallbackObject->on_save_contact(acc_id, our_contact, contact_hdr->expires);
                    break;
                }
            }
        }
        pj_pool_release(pool);
    }
    PJ_LOG(4, (THIS_FILE, "mod_reg_tracker_on_rx_response done"));
    return PJ_FALSE;
}


static pjsua_acc_id mod_reg_tracker_acc_find_for_outgoing(pjsip_tx_data *tdata)
{
    unsigned i;
    pjsip_to_hdr* to_hdr = (pjsip_to_hdr*) pjsip_msg_find_hdr(tdata->msg, PJSIP_H_TO, NULL);
    if (to_hdr != NULL) {
        const pjsip_sip_uri* sip_uri;
        if (!PJSIP_URI_SCHEME_IS_SIP(to_hdr->uri)
                && !PJSIP_URI_SCHEME_IS_SIPS(to_hdr->uri)) {
            return PJSUA_INVALID_ID;
        }
        /* TODO : shall we pjsua_lock? */
        sip_uri = (pjsip_sip_uri*) pjsip_uri_get_uri(to_hdr->uri);

        for (i = 0; i < pjsua_var.acc_cnt; ++i) {
            unsigned acc_id = pjsua_var.acc_ids[i];
            pjsua_acc *acc = &pjsua_var.acc[acc_id];
            if (acc->valid && pj_stricmp(&acc->user_part, &sip_uri->user) == 0
                    && pj_stricmp(&acc->srv_domain, &sip_uri->host) == 0) {
                /* Match ! */
                return acc_id;
            }
        }
    }
    return PJSUA_INVALID_ID;
}

static pj_bool_t mod_reg_tracker_on_tx_request(pjsip_tx_data* tdata)
{
    if(registeredCallbackObject == NULL){
        return PJ_FALSE;
    }
    if (tdata->msg->line.req.method.id == PJSIP_REGISTER_METHOD) {
        const pj_str_t STR_CONTACT = { (char*)"Contact", 7 };
        pj_bool_t has_old_contact = PJ_FALSE;
        pjsip_contact_hdr* old_contact_hdr;
        const pjsip_hdr *hdr;
        pjsip_uri *to_uri;
        pj_str_t old_contact;
        pjsua_acc_id acc_id = mod_reg_tracker_acc_find_for_outgoing(tdata);

        if(acc_id == PJSUA_INVALID_ID){
            return PJ_FALSE;
        }

        old_contact = registeredCallbackObject->on_restore_contact(acc_id);
        if(old_contact.slen <= 0){
            return PJ_FALSE;
        }
        old_contact_hdr = (pjsip_contact_hdr*) pjsip_parse_hdr(tdata->pool,
                &STR_CONTACT, old_contact.ptr, old_contact.slen, NULL);
        if (old_contact_hdr == NULL) {
            return PJ_FALSE;
        }

        /* Enumerate all Contact headers in the response */
        for (hdr = tdata->msg->hdr.next; hdr != &tdata->msg->hdr; hdr = hdr->next) {
            if (hdr->type == PJSIP_H_CONTACT) {
                pjsip_contact_hdr* contact_hdr = (pjsip_contact_hdr*) hdr;
                if (mod_reg_tracker_same_contact_hdr(contact_hdr,
                        old_contact_hdr)) {
                    /* Found the matching one ! */
                    has_old_contact = PJ_TRUE;
                    PJ_LOG(4, (THIS_FILE, "The register already has old contact in it, ignore"));
                    break;
                }
            }
        }

        if (!has_old_contact) {
            old_contact_hdr->expires = 0;
            PJ_LOG(4, (THIS_FILE, "Hook a RX request"));
            pj_list_push_back(&tdata->msg->hdr, old_contact_hdr);
        }
    }
    return PJ_FALSE;
}

/* The module instance. */
static pjsip_module mod_reg_tracker_handler =
{
    NULL, NULL,             /* prev, next.      */
    { (char*)"mod-reg-tracker", 15 },  /* Name.        */
    -1,                 /* Id           */
    PJSIP_MOD_PRIORITY_TSX_LAYER - 1,  /* Priority         */
    NULL,               /* load()       */
    NULL,               /* start()      */
    NULL,               /* stop()       */
    NULL,               /* unload()     */
    NULL,               /* on_rx_request()  */
    &mod_reg_tracker_on_rx_response,               /* on_rx_response() */
    &mod_reg_tracker_on_tx_request,               /* on_tx_request.   */
    NULL,               /* on_tx_response() */
    NULL,               /* on_tsx_state()   */

};


PJ_DECL(pj_status_t) mobile_reg_handler_init() {
    return pjsip_endpt_register_module(pjsua_get_pjsip_endpt(),
                        &mod_reg_tracker_handler);
}

}

