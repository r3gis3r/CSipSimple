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
#include "pjsip_mod_earlylock.h"

#define THIS_FILE "pjsip_mod_earlylock.cpp"

static EarlyLockCallback* registeredCallbackObject = NULL;

extern "C" {
void mod_earlylock_set_callback(EarlyLockCallback* cb)
{
    registeredCallbackObject = cb;
}

static pj_bool_t mod_earlylock_on_rx_request(pjsip_rx_data* rdata)
{
    if(registeredCallbackObject == NULL){
        return PJ_FALSE;
    }
    PJ_LOG(4, (THIS_FILE, "mod_earlylock_on_rx_request"));
    if(rdata == NULL || rdata->msg_info.cseq == NULL || rdata->msg_info.msg == NULL){
        return PJ_FALSE;
    }

    if (rdata->msg_info.cseq->method.id == PJSIP_INVITE_METHOD) {
        registeredCallbackObject->on_create_early_lock();
    }
    return PJ_FALSE;
}

/* The module instance. */
static pjsip_module mod_earlylock_handler =
{
    NULL, NULL,             /* prev, next.      */
    { (char*)"mod-earlylock", 13 },  /* Name.        */
    -1,                 /* Id           */
    PJSIP_MOD_PRIORITY_TSX_LAYER - 1,  /* Priority         */
    NULL,               /* load()       */
    NULL,               /* start()      */
    NULL,               /* stop()       */
    NULL,               /* unload()     */
    &mod_earlylock_on_rx_request,               /* on_rx_request()  */
    NULL,               /* on_rx_response() */
    NULL,               /* on_tx_request.   */
    NULL,               /* on_tx_response() */
    NULL,               /* on_tsx_state()   */

};


PJ_DECL(pj_status_t) mod_earlylock_init() {
    return pjsip_endpt_register_module(pjsua_get_pjsip_endpt(),
                        &mod_earlylock_handler);
}

}

