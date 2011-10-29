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


#include "pjsua_jni_addons.h"


#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
#include "zrtp_android.h"
#endif

#if PJ_ANDROID_DEVICE==1
#include "android_dev.h"
#endif

#if PJ_ANDROID_DEVICE==2
#include "opensl_dev.h"
#endif

#define THIS_FILE		"pjsua_jni_addons.c"

#define USE_TCP_HACK 0

/**
 * Get nbr of codecs
 */
PJ_DECL(int) codecs_get_nbr() {
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);
	pj_status_t status = pjsua_enum_codecs(c, &count);
	if (status == PJ_SUCCESS) {
		return count;
	}
	return 0;
}

/**
 * Get codec id
 */
PJ_DECL(pj_str_t) codecs_get_id(int codec_id) {
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);

	pjsua_enum_codecs(c, &count);
	if (codec_id < count) {
		return c[codec_id].codec_id;
	}
	return pj_str((char *)"INVALID/8000/1");
}

/**
 * Get call infos
 */
PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media, const char *indent){
	char some_buf[1024 * 3];
    pjsua_call_dump(call_id, with_media, some_buf,
		    sizeof(some_buf), indent);
    return pj_str(some_buf);
}

/**
 * Can we use TLS ?
 */
PJ_DECL(pj_bool_t) can_use_tls(){
	return PJ_HAS_SSL_SOCK;
}

/**
 * Can we use SRTP ?
 */
PJ_DECL(pj_bool_t) can_use_srtp(){
	return PJMEDIA_HAS_SRTP;
}



/**
 * Send dtmf with info method
 */
PJ_DECL(pj_status_t) send_dtmf_info(int current_call, pj_str_t digits){
	/* Send DTMF with INFO */
	if (current_call == -1) {
		PJ_LOG(3,(THIS_FILE, "No current call"));
		return PJ_EINVAL;
	} else {
		const pj_str_t SIP_INFO = pj_str((char *)"INFO");
		int call = current_call;
		int i;
		pj_status_t status = PJ_EINVAL;
		pjsua_msg_data msg_data;
		PJ_LOG(4,(THIS_FILE, "SEND DTMF : %.*s", digits.slen, digits.ptr));

		for (i=0; i<digits.slen; ++i) {
			char body[80];

			pjsua_msg_data_init(&msg_data);
			msg_data.content_type = pj_str((char *)"application/dtmf-relay");

			pj_ansi_snprintf(body, sizeof(body),
					 "Signal=%c\r\n"
					 "Duration=160",
					 digits.ptr[i]);
			msg_data.msg_body = pj_str(body);
			PJ_LOG(4,(THIS_FILE, "Send %.*s", msg_data.msg_body.slen, msg_data.msg_body.ptr));

			status = pjsua_call_send_request(current_call, &SIP_INFO,
							 &msg_data);
			if (status != PJ_SUCCESS) {
				PJ_LOG(2,(THIS_FILE, "Failed %d", status));
			break;
			}
		}
		return status;
	}
}

/**
 * Create ipv6 transport
 */
PJ_DECL(pj_status_t) media_transports_create_ipv6(pjsua_transport_config rtp_cfg) {
	/*
    pjsua_media_transport tp[PJSUA_MAX_CALLS];
    pj_status_t status;
    int port = rtp_cfg.port;
    unsigned i;

    //TODO : here should be get from config
    for (i=0; i<PJSUA_MAX_CALLS; ++i) {
	enum { MAX_RETRY = 10 };
	pj_sock_t sock[2];
	pjmedia_sock_info si;
	unsigned j;

	// Get rid of uninitialized var compiler warning with MSVC
	status = PJ_SUCCESS;

	for (j=0; j<MAX_RETRY; ++j) {
	    unsigned k;

	    for (k=0; k<2; ++k) {
		pj_sockaddr bound_addr;

		status = pj_sock_socket(pj_AF_INET6(), pj_SOCK_DGRAM(), 0, &sock[k]);
		if (status != PJ_SUCCESS)
		    break;

		status = pj_sockaddr_init(pj_AF_INET6(), &bound_addr,
					  &rtp_cfg.bound_addr,
					  (unsigned short)(port+k));
		if (status != PJ_SUCCESS)
		    break;

		status = pj_sock_bind(sock[k], &bound_addr,
				      pj_sockaddr_get_len(&bound_addr));
		if (status != PJ_SUCCESS)
		    break;
	    }
	    if (status != PJ_SUCCESS) {
		if (k==1)
		    pj_sock_close(sock[0]);

		if (port != 0)
		    port += 10;
		else
		    break;

		continue;
	    }

	    pj_bzero(&si, sizeof(si));
	    si.rtp_sock = sock[0];
	    si.rtcp_sock = sock[1];

	    pj_sockaddr_init(pj_AF_INET6(), &si.rtp_addr_name,
			     &rtp_cfg.public_addr,
			     (unsigned short)(port));
	    pj_sockaddr_init(pj_AF_INET6(), &si.rtcp_addr_name,
			     &rtp_cfg.public_addr,
			     (unsigned short)(port+1));

	    status = pjmedia_transport_udp_attach(pjsua_get_pjmedia_endpt(),
						  NULL,
						  &si,
						  0,
						  &tp[i].transport);
	    if (port != 0)
		port += 10;
	    else
		break;

	    if (status == PJ_SUCCESS)
		break;
	}

	if (status != PJ_SUCCESS) {
	    pjsua_perror(THIS_FILE, "Error creating IPv6 UDP media transport",
			 status);
	    for (j=0; j<i; ++j) {
		pjmedia_transport_close(tp[j].transport);
	    }
	    return status;
	}
    }

    return pjsua_media_transports_attach(tp, i, PJ_TRUE);
    */
	return PJ_SUCCESS;
}



/**
 * Is call using a secure RTP method (SRTP/ZRTP -- TODO)
 */
PJ_DECL(pj_bool_t) is_call_secure(pjsua_call_id call_id){

    pjsua_call *call;
    pjsip_dialog *dlg;
    pj_status_t status;
    unsigned i;
    pjmedia_transport_info tp_info;
    pj_bool_t result = PJ_FALSE;

	PJ_ASSERT_RETURN(call_id>=0 && call_id<(int)pjsua_var.ua_cfg.max_calls,
		 PJ_EINVAL);


	status = acquire_call("is_call_secure()", call_id, &call, &dlg);
	if (status != PJ_SUCCESS) {
		return result;
	}

    for (i=0; i<call->med_cnt; ++i) {
		pjsua_call_media *call_med = &call->media[i];
		if(pjsua_call_has_media(call_id)){

			/* Get and ICE SRTP status */
			if (call_med->tp) {
			    pjmedia_transport_info tp_info;

			    pjmedia_transport_info_init(&tp_info);
			    pjmedia_transport_get_info(call_med->tp, &tp_info);
			    if (tp_info.specific_info_cnt > 0) {
					unsigned j;
					for (j = 0; j < tp_info.specific_info_cnt; ++j) {
						if (tp_info.spc_info[j].type == PJMEDIA_TRANSPORT_TYPE_SRTP){
							pjmedia_srtp_info *srtp_info =
									(pjmedia_srtp_info*) tp_info.spc_info[j].buffer;
							if(srtp_info->active){
								result = PJ_TRUE;
							}
						}
					}
			    }
			}
		}
    }

	pjsip_dlg_dec_lock(dlg);

	return result;
}

#if USE_TCP_HACK==1
static pj_bool_t on_rx_request_tcp_hack(pjsip_rx_data *rdata) {
	 PJ_LOG(3,(THIS_FILE, "CB TCP HACK"));
	if (strstr(pj_strbuf(&rdata->msg_info.msg->line.req.method.name), "INVITE")) {
		 PJ_LOG(3,(THIS_FILE, "We have an invite here"));

	}

	return PJ_FALSE;

}
#endif





static char errmsg[PJ_ERR_MSG_SIZE];
//Get error message
PJ_DECL(pj_str_t) get_error_message(int status) {

    return pj_strerror(status, errmsg, sizeof(errmsg));
    /* pj_str(errmsg);
    PJ_LOG(3,(THIS_FILE, "Error for %d msg %s", status, result));
    return result;*/
}


PJ_DECL(void) csipsimple_config_default(csipsimple_config *css_cfg){
	css_cfg->use_compact_form_sdp = PJ_FALSE;
	css_cfg->use_compact_form_headers = PJ_FALSE;
	css_cfg->use_no_update = PJ_FALSE;
	css_cfg->use_zrtp = PJ_FALSE;

}

//Wrap start & stop
PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
				pjsua_logging_config *log_cfg,
				pjsua_media_config *media_cfg,
				csipsimple_config *css_cfg){
	pj_status_t result;

	// Finalize configuration
	log_cfg->cb = &pj_android_log_msg;
	if(css_cfg->turn_username.slen){
		media_cfg->turn_auth_cred.type = PJ_STUN_AUTH_CRED_STATIC;
		media_cfg->turn_auth_cred.data.static_cred.realm = pj_str("*");
		media_cfg->turn_auth_cred.data.static_cred.username = css_cfg->turn_username;

		if (css_cfg->turn_password.slen) {
			 media_cfg->turn_auth_cred.data.static_cred.data_type = PJ_STUN_PASSWD_PLAIN;
			 media_cfg->turn_auth_cred.data.static_cred.data = css_cfg->turn_password;
		}
	}

	// Static cfg
	extern pj_bool_t pjsip_use_compact_form;
	extern pj_bool_t pjsip_include_allow_hdr_in_dlg;
	extern pj_bool_t pjmedia_add_rtpmap_for_static_pt;
	extern pj_bool_t pjsua_no_update;


	pjsua_no_update = css_cfg->use_no_update ? PJ_TRUE : PJ_FALSE;

	pjsip_use_compact_form = css_cfg->use_compact_form_headers ? PJ_TRUE : PJ_FALSE;
	/* do not transmit Allow header */
	pjsip_include_allow_hdr_in_dlg = css_cfg->use_compact_form_headers ? PJ_FALSE : PJ_TRUE;
	/* Do not include rtpmap for static payload types (<96) */
	pjmedia_add_rtpmap_for_static_pt = css_cfg->use_compact_form_sdp ? PJ_FALSE : PJ_TRUE;


#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
	if(css_cfg->use_zrtp){
		ua_cfg->cb.on_create_media_transport = &on_zrtp_transport_created;
	}
#endif
	result = (pj_status_t) pjsua_init(ua_cfg, log_cfg, media_cfg);
	if(result == PJ_SUCCESS){
		init_ringback_tone();
#if PJMEDIA_AUDIO_DEV_HAS_ANDROID
#if PJ_ANDROID_DEVICE==1
		pjmedia_aud_register_factory(&pjmedia_android_factory);
#endif
#if PJ_ANDROID_DEVICE==2
		pjmedia_aud_register_factory(&pjmedia_opensl_factory);
#endif
#endif

#if USE_TCP_HACK==1
	    // Registering module for tcp hack
	    static pjsip_module tcp_hack_mod; // cannot be a stack variable

	    memset(&tcp_hack_mod, 0, sizeof(tcp_hack_mod));
	    tcp_hack_mod.id = -1;
	    tcp_hack_mod.priority = PJSIP_MOD_PRIORITY_UA_PROXY_LAYER - 1;
	    tcp_hack_mod.on_rx_response = &on_rx_request_tcp_hack;
	    tcp_hack_mod.name = pj_str("TCP-Hack");

	    result = pjsip_endpt_register_module(pjsip_ua_get_endpt(pjsip_ua_instance()), &tcp_hack_mod);
#endif


	}


	return result;
}

PJ_DECL(pj_status_t) csipsimple_destroy(void){
	destroy_ringback_tone();
	return (pj_status_t) pjsua_destroy();
}


void update_active_calls(const pj_str_t *new_ip_addr) {
	pjsip_tpselector tp_sel;
	pjsua_init_tpselector(0, &tp_sel); // << 0 is hard coded here for active transportId.  could be passed in if needed.
	int ndx;
	for (ndx = 0; ndx < pjsua_var.ua_cfg.max_calls; ++ndx) {
		pjsua_call *call = &pjsua_var.calls[ndx];
		if (!call->inv || call->inv->state != PJSIP_INV_STATE_CONFIRMED){
			continue;
		}

		// -- TODO : we should do something here about transport,
		// but something that actually restart media transport for this call
		// cause copying ip addr somewhere is not valid for stun and nat cases
		//transport_set_sdp_addr_from_string(call->med_orig, new_ip_addr);
		//transport_set_sdp_addr_from_string(call->med_tp,   new_ip_addr);

		if (call->local_hold) {
			pjsua_call_set_hold(ndx, NULL);
		} else {
			pjsua_call_reinvite(ndx, PJ_TRUE, NULL);
		}
	}
}

PJ_DECL(pj_status_t) update_transport(const pj_str_t *new_ip_addr) {
	PJSUA_LOCK();

	PJ_LOG(4, (THIS_FILE,"update_transport to addr = %s", new_ip_addr->ptr));
	// No need ot check thread cause csipsimple use handler thread

	/*
	pjsua_transport_config cfg;
	pjsua_transport_config_default(&cfg);
	cfg.port = 0;

	pjsua_media_transports_create(&cfg);
	*/
	update_active_calls(new_ip_addr);

	PJSUA_UNLOCK();
	return PJ_SUCCESS;
}



// Android app glue

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "libpjsip", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "libpjsip", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "libpjsip", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "libpjsip", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "libpjsip", __VA_ARGS__)


static void pj_android_log_msg(int level, const char *data, int len) {
	if (level <= 1) {
		LOGE("%s", data);
	} else if (level == 2) {
		LOGW("%s", data);
	} else if (level == 3) {
		LOGI("%s", data);
	} else if (level == 4) {
		LOGD("%s", data);
	} else if (level >= 5) {
		LOGV("%s", data);
	}
}


//---------------------
// RINGBACK MANAGEMENT-
// --------------------
/* Ringtones		    US	       UK  */
#define RINGBACK_FREQ1	    440	    /* 400 */
#define RINGBACK_FREQ2	    480	    /* 450 */
#define RINGBACK_ON	    2000    /* 400 */
#define RINGBACK_OFF	    4000    /* 200 */
#define RINGBACK_CNT	    1	    /* 2   */
#define RINGBACK_INTERVAL   4000    /* 2000 */

static struct app_config {
	pj_pool_t		   *pool;
    int			    ringback_slot;
    int			    ringback_cnt;
    pjmedia_port	   *ringback_port;
    pj_bool_t ringback_on;
} app_config;

void ringback_start(){
	if (app_config.ringback_on) {
		//Already ringing back
		return;
	}
	app_config.ringback_on = PJ_TRUE;
    if (++app_config.ringback_cnt==1 && app_config.ringback_slot!=PJSUA_INVALID_ID){
    	pjsua_conf_connect(app_config.ringback_slot, 0);
    }
}

void ring_stop(pjsua_call_id call_id) {
    if (app_config.ringback_on) {
    	app_config.ringback_on = PJ_FALSE;

		pj_assert(app_config.ringback_cnt>0);
		if (--app_config.ringback_cnt == 0 &&
			app_config.ringback_slot!=PJSUA_INVALID_ID)  {
			pjsua_conf_disconnect(app_config.ringback_slot, 0);
			pjmedia_tonegen_rewind(app_config.ringback_port);
		}
    }
}

void init_ringback_tone(){
	pj_status_t status;
	pj_str_t name;
	pjmedia_tone_desc tone[RINGBACK_CNT];
	unsigned i;

	app_config.pool = pjsua_pool_create("pjsua-jni", 1000, 1000);
	app_config.ringback_slot=PJSUA_INVALID_ID;
	app_config.ringback_on = PJ_FALSE;
	app_config.ringback_cnt = 0;

	//Ringback
	name = pj_str((char *)"ringback");
	status = pjmedia_tonegen_create2(app_config.pool, &name,
					 16000,
					 1,
					 320,
					 16, PJMEDIA_TONEGEN_LOOP,
					 &app_config.ringback_port);
	if (status != PJ_SUCCESS){
		goto on_error;
	}

	pj_bzero(&tone, sizeof(tone));
	for (i=0; i<RINGBACK_CNT; ++i) {
		tone[i].freq1 = RINGBACK_FREQ1;
		tone[i].freq2 = RINGBACK_FREQ2;
		tone[i].on_msec = RINGBACK_ON;
		tone[i].off_msec = RINGBACK_OFF;
	}
	tone[RINGBACK_CNT-1].off_msec = RINGBACK_INTERVAL;
	pjmedia_tonegen_play(app_config.ringback_port, RINGBACK_CNT, tone, PJMEDIA_TONEGEN_LOOP);
	status = pjsua_conf_add_port(app_config.pool, app_config.ringback_port,
					 &app_config.ringback_slot);
	if (status != PJ_SUCCESS){
		goto on_error;
	}
	return;

	on_error :{
		pj_pool_release(app_config.pool);
	}
}

void destroy_ringback_tone(){
	/* Close ringback port */
	if (app_config.ringback_port &&
	app_config.ringback_slot != PJSUA_INVALID_ID){
		pjsua_conf_remove_port(app_config.ringback_slot);
		app_config.ringback_slot = PJSUA_INVALID_ID;
		pjmedia_port_destroy(app_config.ringback_port);
		app_config.ringback_port = NULL;
	}

    if (app_config.pool) {
	pj_pool_release(app_config.pool);
	app_config.pool = NULL;
    }
}

void app_on_call_state(pjsua_call_id call_id, pjsip_event *e) {
	pjsua_call_info call_info;
	pjsua_call_get_info(call_id, &call_info);

	if (call_info.state == PJSIP_INV_STATE_DISCONNECTED) {
		/* Stop all ringback for this call */
		ring_stop(call_id);
		PJ_LOG(3,(THIS_FILE, "Call %d is DISCONNECTED [reason=%d (%s)]",
						call_id,
						call_info.last_status,
						call_info.last_status_text.ptr));
	} else {
		if (call_info.state == PJSIP_INV_STATE_EARLY) {
			int code;
			pj_str_t reason;
			pjsip_msg *msg;

			/* This can only occur because of TX or RX message */
			pj_assert(e->type == PJSIP_EVENT_TSX_STATE);

			if (e->body.tsx_state.type == PJSIP_EVENT_RX_MSG) {
				msg = e->body.tsx_state.src.rdata->msg_info.msg;
			} else {
				msg = e->body.tsx_state.src.tdata->msg;
			}

			code = msg->line.status.code;
			reason = msg->line.status.reason;

			/* Start ringback for 180 for UAC unless there's SDP in 180 */
			if (call_info.role == PJSIP_ROLE_UAC && code == 180 && msg->body
					== NULL && call_info.media_status == PJSUA_CALL_MEDIA_NONE) {
				ringback_start();
			}

			PJ_LOG(3,(THIS_FILE, "Call %d state changed to %s (%d %.*s)",
							call_id, call_info.state_text.ptr,
							code, (int)reason.slen, reason.ptr));
		} else {
			PJ_LOG(3,(THIS_FILE, "Call %d state changed to %s",
							call_id,
							call_info.state_text.ptr));
		}
	}
}


