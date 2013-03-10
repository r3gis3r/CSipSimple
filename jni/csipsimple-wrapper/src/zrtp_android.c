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
#include "zrtp_android.h"
#include "csipsimple_internal.h"

/*
 * ZRTP stuff
 */

#define THIS_FILE		"zrtp_android.c"
#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0



#include "transport_zrtp.h"
#include "libzrtpcpp/ZrtpCWrapper.h"

const char* InfoCodes[] =
{
    "EMPTY",
    "Hello received, preparing a Commit",
    "Commit: Generated a public DH key",
    "Responder: Commit received, preparing DHPart1",
    "DH1Part: Generated a public DH key",
    "Initiator: DHPart1 received, preparing DHPart2",
    "Responder: DHPart2 received, preparing Confirm1",
    "Initiator: Confirm1 received, preparing Confirm2",
    "Responder: Confirm2 received, preparing Conf2Ack",
    "At least one retained secrets matches - security OK",
    "Entered secure state",
    "No more security for this session"
};

/**
 * Sub-codes for Warning
 */
const char* WarningCodes [] =
{
    "EMPTY",
    "Commit contains an AES256 cipher but does not offer a Diffie-Helman 4096",
    "Received a GoClear message",
    "Hello offers an AES256 cipher but does not offer a Diffie-Helman 4096",
    "No retained shared secrets available - must verify SAS",
    "Internal ZRTP packet checksum mismatch - packet dropped",
    "Dropping packet because SRTP authentication failed!",
    "Dropping packet because SRTP replay check failed!",
    "Valid retained shared secrets availabe but no matches found - must verify SAS"
};

/**
 * Sub-codes for Severe
 */
const char* SevereCodes[] =
{
    "EMPTY",
    "Hash HMAC check of Hello failed!",
    "Hash HMAC check of Commit failed!",
    "Hash HMAC check of DHPart1 failed!",
    "Hash HMAC check of DHPart2 failed!",
    "Cannot send data - connection or peer down?",
    "Internal protocol error occured!",
    "Cannot start a timer - internal resources exhausted?",
    "Too much retries during ZRTP negotiation - connection or peer down?"
};



typedef struct zrtp_cb_user_data {
	pjsua_call_id call_id;
	pjmedia_transport *zrtp_tp;
	pj_str_t sas;
	pj_str_t cipher;
	int sas_verified;
} zrtp_cb_user_data;

static void zrtpShowSas(void* data, char* sas, int verified){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
	PJ_LOG(4, (THIS_FILE, "Show sas : %s in ctxt %x", sas, zrtp_data));
	pj_strdup2_with_null(css_var.pool, &zrtp_data->sas, sas);
	zrtp_data->sas_verified = verified;
	on_zrtp_show_sas_wrapper(zrtp_data->call_id, sas, verified);
}

static void zrtpSecureOn(void* data, char* cipher){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
	pj_strdup2_with_null(css_var.pool, &zrtp_data->cipher, cipher);

	on_zrtp_update_transport_wrapper(zrtp_data->call_id);
}

static void zrtpSecureOff(void* data){
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
	on_zrtp_update_transport_wrapper(zrtp_data->call_id);
}

static void confirmGoClear(void* data)
{
    PJ_LOG(3,(THIS_FILE, "GoClear?"));
}
static void showMessage(void* data, int32_t sev, int32_t subCode)
{
	zrtp_cb_user_data* zrtp_data = (zrtp_cb_user_data*) data;
    switch (sev)
    {
    case zrtp_Info:
        PJ_LOG(3,(THIS_FILE, "ZRTP info message: %s", InfoCodes[subCode]));
        if(subCode == zrtp_InfoSecureStateOn
        		|| subCode == zrtp_InfoSecureStateOff){
        	if(zrtp_data != NULL){
        		on_zrtp_update_transport_wrapper(zrtp_data->call_id);
        	}else{
        		PJ_LOG(1, (THIS_FILE, "Got a message without associated call_id"));
        	}
        }
        break;

    case zrtp_Warning:
        PJ_LOG(3,(THIS_FILE, "ZRTP warning message: %s", WarningCodes[subCode]));
        break;

    case zrtp_Severe:
        PJ_LOG(3,(THIS_FILE, "ZRTP severe message: %s", SevereCodes[subCode]));
        break;

    case zrtp_ZrtpError:
        PJ_LOG(1,(THIS_FILE, "ZRTP Error: subcode: %d", subCode));
        break;
    }
}
static void zrtpNegotiationFailed(void* data, int32_t severity, int32_t subCode)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP failed: %d, subcode: %d", severity, subCode));
}
static void zrtpNotSuppOther(void* data)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP not supported by other peer"));
}
static void zrtpAskEnrollment(void* data, int32_t info)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - Ask PBX enrollment"));
}
static void zrtpInformEnrollment(void* data, int32_t info)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - Inform PBX enrollement"));
}
static void signSAS(void* data, uint8_t* sas)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - sign SAS"));
}
static int32_t checkSASSignature(void* data, uint8_t* sas)
{
    PJ_LOG(3,(THIS_FILE, "ZRTP - check SAS signature"));
    return 0;
}




/* Initialize the ZRTP transport and the user callbacks */
pjmedia_transport* on_zrtp_transport_created(pjsua_call_id call_id,
	unsigned media_idx,
	pjmedia_transport *base_tp,
	unsigned flags) {
        pjsua_call *call;
		pjmedia_transport *zrtp_tp = NULL;
		pj_status_t status;
		pjmedia_endpt* endpt = pjsua_get_pjmedia_endpt();

		// For now, do zrtp only on audio stream
        call = &pjsua_var.calls[call_id];
        if (media_idx < call->med_prov_cnt) {
            pjsua_call_media *call_med = &call->media_prov[media_idx];
            if (call_med->tp && call_med->type != PJMEDIA_TYPE_AUDIO) {
                PJ_LOG(2, (THIS_FILE, "ZRTP transport not yet supported for : %d", call_med->type));
                return base_tp;
            }
        }

	    // Create zrtp transport adapter
		status = pjmedia_transport_zrtp_create(endpt, NULL, base_tp,
											   &zrtp_tp, (flags & PJSUA_MED_TP_CLOSE_MEMBER));

		if(status == PJ_SUCCESS){
			PJ_LOG(4,(THIS_FILE, "ZRTP transport created"));
			// TODO : we should use our own pool
			// Build callback data ponter
			zrtp_cb_user_data* zrtp_cb_data = PJ_POOL_ZALLOC_T(css_var.pool, zrtp_cb_user_data);
			zrtp_cb_data->zrtp_tp = zrtp_tp;
			zrtp_cb_data->call_id = call_id;
			zrtp_cb_data->cipher = pj_str("");
			zrtp_cb_data->sas = pj_str("");
			zrtp_cb_data->sas_verified = PJ_FALSE;


			// Build callback struct
			zrtp_UserCallbacks* zrtp_cbs = PJ_POOL_ZALLOC_T(css_var.pool, zrtp_UserCallbacks);
			zrtp_cbs->zrtp_secureOn = &zrtpSecureOn;
			zrtp_cbs->zrtp_secureOff = &zrtpSecureOff;
			zrtp_cbs->zrtp_showSAS = &zrtpShowSas;
			zrtp_cbs->zrtp_confirmGoClear = &confirmGoClear;
			zrtp_cbs->zrtp_showMessage = &showMessage;
			zrtp_cbs->zrtp_zrtpNegotiationFailed = &zrtpNegotiationFailed;
			zrtp_cbs->zrtp_zrtpNotSuppOther = &zrtpNotSuppOther;
			zrtp_cbs->zrtp_zrtpAskEnrollment = &zrtpAskEnrollment;
			zrtp_cbs->zrtp_zrtpInformEnrollment = &zrtpInformEnrollment;
			zrtp_cbs->zrtp_signSAS = &signSAS;
			zrtp_cbs->zrtp_checkSASSignature = &checkSASSignature;
			zrtp_cbs->userData = zrtp_cb_data;

			pjmedia_transport_zrtp_setUserCallback(zrtp_tp, zrtp_cbs);


			/*
			* Initialize the transport. Just the filename of the ZID file that holds
			* our partners ZID, shared data etc. If the files does not exists it will
			* be created an initialized. The ZRTP configuration is not yet implemented
			* thus the parameter is NULL.
			*/
			pjmedia_transport_zrtp_initialize(zrtp_tp, css_var.zid_file, PJ_TRUE);
#if 0
			// This is a crappy hack for buggy versions of sip servers that does not correctly manage hello
			ZrtpContext* zrtpContext = pjmedia_transport_zrtp_getZrtpContext(zrtp_tp);
			zrtp_setMandatoryOnly(zrtpContext);
#endif

			return zrtp_tp;
		} else {
			PJ_LOG(1, (THIS_FILE, "ZRTP transport problem : %d", status));
			return base_tp;
		}
}

struct jzrtp_allContext {
	ZrtpContext* zrtpContext;
	zrtp_cb_user_data* cbUserData;
};

struct jzrtp_allContext jzrtp_getContext(pjsua_call_id call_id) {

	pjsua_call *call;
	pj_status_t status;
	unsigned i;
	pjmedia_transport_info tp_info;

	struct jzrtp_allContext result;
	result.cbUserData = NULL;
	result.zrtpContext = NULL;

	PJ_ASSERT_RETURN(call_id>=0 && call_id<(int)pjsua_var.ua_cfg.max_calls,
			NULL);


	if (pjsua_call_has_media(call_id)) {
		call = &pjsua_var.calls[call_id];
		for (i = 0; i < call->med_cnt; ++i) {
			pjsua_call_media *call_med = &call->media[i];
			if (call_med->tp && call_med->type == PJMEDIA_TYPE_AUDIO) {
				pjmedia_transport_info tp_info;

				pjmedia_transport_info_init(&tp_info);
				pjmedia_transport_get_info(call_med->tp, &tp_info);
				if (tp_info.specific_info_cnt > 0) {
					unsigned j;
					for (j = 0; j < tp_info.specific_info_cnt; ++j) {
						if (tp_info.spc_info[j].type
								== PJMEDIA_TRANSPORT_TYPE_ZRTP) {
							result.zrtpContext = pjmedia_transport_zrtp_getZrtpContext(call_med->tp);
							result.cbUserData = (zrtp_cb_user_data*) pjmedia_transport_zrtp_getUserData(call_med->tp);
						}
					}
				}
			}
		}
	}
	return result;
}

PJ_DECL(void) jzrtp_SASVerified(pjsua_call_id call_id) {
	struct jzrtp_allContext ac = jzrtp_getContext(call_id);
	if(ac.cbUserData != NULL){
		ac.cbUserData->sas_verified = 1;
	}
	if(ac.zrtpContext != NULL){
		zrtp_SASVerified(ac.zrtpContext);
	}else{
		PJ_LOG(1, (THIS_FILE, "jzrtp_SASVerified: No ZRTP context for call %d", call_id));
	}
}

PJ_DECL(void) jzrtp_SASRevoked(pjsua_call_id call_id) {
	struct jzrtp_allContext ac = jzrtp_getContext(call_id);
	if(ac.cbUserData != NULL){
		ac.cbUserData->sas_verified = 0;
	}
	if(ac.zrtpContext != NULL){
		zrtp_resetSASVerified(ac.zrtpContext);
	}else{
		PJ_LOG(1, (THIS_FILE, "jzrtp_SASRevoked: No ZRTP context for call %d", call_id));
	}
}

zrtp_state_info jzrtp_getInfoFromContext(struct jzrtp_allContext ac){
	zrtp_state_info info;
	info.sas.slen = 0;
	info.sas.ptr = "";
	info.sas_verified = PJ_FALSE;
	info.cipher.slen = 0;
	info.cipher.ptr = "";
	info.secure = PJ_FALSE;
	info.call_id = PJSUA_INVALID_ID;
	PJ_LOG(4, (THIS_FILE, "jzrtp_getInfoFromContext : user data %x", ac.cbUserData));
	if(ac.zrtpContext != NULL){
		int32_t state = zrtp_inState(ac.zrtpContext, SecureState);
		info.secure = state ? PJ_TRUE : PJ_FALSE;
		if(ac.cbUserData){
			info.sas_verified = ac.cbUserData->sas_verified;
			info.call_id = ac.cbUserData->call_id;
			pj_strassign(&info.sas, &ac.cbUserData->sas);
			pj_strassign(&info.cipher, &ac.cbUserData->cipher);
		}

	}
	return info;
}

PJ_DECL(zrtp_state_info) jzrtp_getInfoFromCall(pjsua_call_id call_id){
	PJSUA_LOCK();
	struct jzrtp_allContext ctxt = jzrtp_getContext(call_id);
	zrtp_state_info info = jzrtp_getInfoFromContext(ctxt);
	PJSUA_UNLOCK();
	return info;
}


zrtp_state_info jzrtp_getInfoFromTransport(pjmedia_transport* tp){
	PJSUA_LOCK();
	struct jzrtp_allContext ctxt;
	ctxt.zrtpContext = pjmedia_transport_zrtp_getZrtpContext(tp);
	ctxt.cbUserData = (zrtp_cb_user_data*) pjmedia_transport_zrtp_getUserData(tp);
	zrtp_state_info info =  jzrtp_getInfoFromContext(ctxt);
	PJSUA_UNLOCK();
	return info;
}



#else
PJ_DECL(void) jzrtp_SASVerified(pjsua_call_id call_id) {
	//TODO : log
}
PJ_DECL(void) jzrtp_SASRevoked(pjsua_call_id call_id) {
	//TODO : log
}
PJ_DECL(zrtp_state_info) jzrtp_getInfoFromCall(pjsua_call_id call_id){
    zrtp_state_info state;
    state.call_id = call_id;
    state.secure = PJ_FALSE;
    state.sas.slen = 0;
    state.cipher.slen = 0;
    state.sas_verified = PJ_FALSE;
    return state;
}
#endif
