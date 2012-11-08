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
#include "android_logger.h"
#include "csipsimple_internal.h"

#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
#include "zrtp_android.h"
#include "transport_zrtp.h"
#endif

#include "android_dev.h"
#if PJMEDIA_HAS_VIDEO
#include "webrtc_android_render_dev.h"
#endif

#include <dlfcn.h>
#include "pj_loader.h"

#define THIS_FILE		"pjsua_jni_addons.c"


/* CSS application instance. */
struct css_data css_var;


/**
 * Get call infos
 */PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media,
		const char *indent) {
	char some_buf[1024 * 3];
	pj_status_t status = pjsua_call_dump(call_id, with_media, some_buf, sizeof(some_buf), indent);
	if(status != PJ_SUCCESS){
		return pj_strerror(status, some_buf, sizeof(some_buf));
	}
	return pj_str(some_buf);
}

/**
 * Send dtmf with info method
 */PJ_DECL(pj_status_t) send_dtmf_info(int current_call, pj_str_t digits) {
	/* Send DTMF with INFO */
	if (current_call == -1) {
		PJ_LOG(3, (THIS_FILE, "No current call"));
		return PJ_EINVAL;
	} else {
		const pj_str_t SIP_INFO = pj_str((char *) "INFO");
		int call = current_call;
		int i;
		pj_status_t status = PJ_EINVAL;
		pjsua_msg_data msg_data;
		PJ_LOG(4, (THIS_FILE, "SEND DTMF : %.*s", digits.slen, digits.ptr));

		for (i = 0; i < digits.slen; ++i) {
			char body[80];

			pjsua_msg_data_init(&msg_data);
			msg_data.content_type = pj_str((char *) "application/dtmf-relay");

			pj_ansi_snprintf(body, sizeof(body), "Signal=%c\r\n"
					"Duration=160", digits.ptr[i]);
			msg_data.msg_body = pj_str(body);
			PJ_LOG(
					4,
					(THIS_FILE, "Send %.*s", msg_data.msg_body.slen, msg_data.msg_body.ptr));

			status = pjsua_call_send_request(current_call, &SIP_INFO,
					&msg_data);
			if (status != PJ_SUCCESS) {
				PJ_LOG(2, (THIS_FILE, "Failed %d", status));
				break;
			}
		}
		return status;
	}
}

/**
 * Is call using a secure RTP method (SRTP/ZRTP)
 */
PJ_DECL(pj_str_t) call_secure_info(pjsua_call_id call_id) {

	pjsua_call *call;
	pj_status_t status;
	unsigned i;
	pjmedia_transport_info tp_info;

	pj_str_t result = pj_str("");

	PJ_ASSERT_RETURN(call_id>=0 && call_id<(int)pjsua_var.ua_cfg.max_calls,
			result);

	PJSUA_LOCK();

	if (pjsua_call_has_media(call_id)) {
		call = &pjsua_var.calls[call_id];
		for (i = 0; i < call->med_cnt; ++i) {
			pjsua_call_media *call_med = &call->media[i];
			PJ_LOG(4, (THIS_FILE, "Get secure for media type %d", call_med->type));
			if (call_med->tp && call_med->type == PJMEDIA_TYPE_AUDIO) {
				pjmedia_transport_info tp_info;

				pjmedia_transport_info_init(&tp_info);
				pjmedia_transport_get_info(call_med->tp, &tp_info);
				if (tp_info.specific_info_cnt > 0) {
					unsigned j;
					for (j = 0; j < tp_info.specific_info_cnt; ++j) {
						if (tp_info.spc_info[j].type
								== PJMEDIA_TRANSPORT_TYPE_SRTP) {
							pjmedia_srtp_info *srtp_info =
									(pjmedia_srtp_info*) tp_info.spc_info[j].buffer;
							if (srtp_info->active) {
								result = pj_str("SRTP");
								break;
							}
						}

#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
						else if (tp_info.spc_info[j].type
								== PJMEDIA_TRANSPORT_TYPE_ZRTP) {
							zrtp_state_info info = jzrtp_getInfoFromTransport(call_med->tp);
							if(info.secure){
								char msg[512];
								PJ_LOG(4, (THIS_FILE, "ZRTP :: V %d", info.sas_verified));
								PJ_LOG(4, (THIS_FILE, "ZRTP :: S L %d", info.sas.slen));
								PJ_LOG(4, (THIS_FILE, "ZRTP :: C L %d", info.cipher.slen));

								pj_ansi_snprintf(msg, sizeof(msg), "ZRTP - %s\n%.*s\n%.*s",
										info.sas_verified ? "Verified": "Not verified",
										info.sas.slen, info.sas.ptr,
										info.cipher.slen, info.cipher.ptr);

								pj_strdup2_with_null(css_var.pool, &result, msg);
								break;
							}
						}
#endif
					}
				}
			}
		}
	}

	PJSUA_UNLOCK();

	return result;
}

// ZRTP and other media dispatcher
pjmedia_transport* on_transport_created_wrapper(pjsua_call_id call_id,
	unsigned media_idx,
	pjmedia_transport *base_tp,
	unsigned flags) {
	pj_status_t status = PJ_SUCCESS;
	pjsua_call_info call_info;
	void* acc_user_data;
	int acc_use_zrtp = -1;

	// By default, use default global def
	pj_bool_t use_zrtp = css_var.default_use_zrtp;
    status = pjsua_call_get_info(call_id, &call_info);
	if(status == PJ_SUCCESS && pjsua_acc_is_valid (call_info.acc_id)){
		acc_user_data = pjsua_acc_get_user_data(call_info.acc_id);
		if(acc_user_data != NULL){
			acc_use_zrtp = ((csipsimple_acc_config *) acc_user_data)->use_zrtp;
			if(acc_use_zrtp >= 0){
				use_zrtp = (acc_use_zrtp == 1) ? PJ_TRUE : PJ_FALSE;
			}
		}
	}
#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
	if(use_zrtp){
		PJ_LOG(4, (THIS_FILE, "Dispatch transport creation on ZRTP one"));
		return on_zrtp_transport_created(call_id, media_idx, base_tp, flags);
	}
#endif


	return base_tp;
}


// ---- VIDEO STUFF ---- //
pj_status_t vid_set_stream_window(pjsua_call_media* call_med, pjmedia_dir dir, void* window){
	pj_status_t status = PJ_ENOTFOUND;
	pjsua_vid_win *w = NULL;
	pjsua_vid_win_id wid;
	pjmedia_vid_dev_stream *dev;


	// We are looking for a rendering video dev
	if (call_med->type == PJMEDIA_TYPE_VIDEO
			&& (call_med->dir & dir)) {

		const char* dirName = (dir == PJMEDIA_DIR_RENDER) ? "render" : "capture";
		PJ_LOG(4, (THIS_FILE, "Has video %s media...", dirName));

		wid = (dir == PJMEDIA_DIR_RENDER) ? call_med->strm.v.rdr_win_id : call_med->strm.v.cap_win_id;
		w = &pjsua_var.win[wid];
		// Make sure we have a render dev
		if (w) {
			dev = pjmedia_vid_port_get_stream( (dir == PJMEDIA_DIR_RENDER) ? w->vp_rend : w->vp_cap);
			if (dev) {
				status = pjmedia_vid_dev_stream_set_cap(dev,
						PJMEDIA_VID_DEV_CAP_OUTPUT_WINDOW,
						(void*) window);
				PJ_LOG(4, (THIS_FILE, "Set %s window >> %x - %x", dirName, dev, window));
			}
		}
	}

	return status;
}

PJ_DECL(pj_status_t) vid_set_android_window(pjsua_call_id call_id,
		jobject window) {
	pj_status_t status = PJ_ENOTFOUND;
	pjsua_call *call;
	int i;

	if( !(call_id>=0 && call_id<(int)pjsua_var.ua_cfg.max_calls) ){
			return PJ_ENOTFOUND;
	}

	PJ_LOG(4, (THIS_FILE, "Setup android window for call %d", call_id));

	PJSUA_LOCK();
	// Retrieve the stream
	if (pjsua_call_has_media(call_id)) {
		call = &pjsua_var.calls[call_id];
		for (i = 0; i < call->med_cnt; ++i) {
			pjsua_call_media *call_med = &call->media[i];

			vid_set_stream_window(call_med, PJMEDIA_DIR_RENDER, window);
			vid_set_stream_window(call_med, PJMEDIA_DIR_CAPTURE, window);
			status = PJ_SUCCESS;
		}
	}

	PJSUA_UNLOCK();
	return status;
}

PJ_DECL(pj_status_t) set_turn_credentials(const pj_str_t username, const pj_str_t password, const pj_str_t realm, pj_stun_auth_cred *turn_auth_cred) {

	PJ_ASSERT_RETURN(turn_auth_cred, PJ_EINVAL);

	/* Create memory pool for application. */
	if(css_var.pool == NULL){
		css_var.pool = pjsua_pool_create("css", 1000, 1000);
		PJ_ASSERT_RETURN(css_var.pool, PJ_ENOMEM);
	}

	if (username.slen) {
		turn_auth_cred->type = PJ_STUN_AUTH_CRED_STATIC;
		pj_strdup_with_null(css_var.pool,
					&turn_auth_cred->data.static_cred.username,
					&username);
	} else {
		turn_auth_cred->data.static_cred.username.slen = 0;
	}

	if(password.slen) {
		turn_auth_cred->data.static_cred.data_type = PJ_STUN_PASSWD_PLAIN;
		pj_strdup_with_null(css_var.pool,
				&turn_auth_cred->data.static_cred.data,
				&password);
	}else{
		turn_auth_cred->data.static_cred.data.slen = 0;
	}

	if(realm.slen) {

	} else {
		turn_auth_cred->data.static_cred.realm = pj_str("*");
	}

	return PJ_SUCCESS;
}

static char errmsg[PJ_ERR_MSG_SIZE];
//Get error message
PJ_DECL(pj_str_t) get_error_message(int status) {
	return pj_strerror(status, errmsg, sizeof(errmsg));
}

// External value
#define DEFAULT_TCP_KA 180
#define DEFAULT_TLS_KA 180

int css_tcp_keep_alive_interval = DEFAULT_TCP_KA;
int css_tls_keep_alive_interval = DEFAULT_TLS_KA;

PJ_DECL(void) csipsimple_config_default(csipsimple_config *css_cfg) {
	css_cfg->use_compact_form_sdp = PJ_FALSE;
	css_cfg->use_compact_form_headers = PJ_FALSE;
	css_cfg->add_bandwidth_tias_in_sdp = PJ_FALSE;
	css_cfg->use_no_update = PJ_FALSE;
	css_cfg->use_zrtp = PJ_FALSE;
	css_cfg->extra_aud_codecs_cnt = 0;
	css_cfg->extra_vid_codecs_cnt = 0;
	css_cfg->audio_implementation.init_factory_name = pj_str("");
	css_cfg->audio_implementation.shared_lib_path = pj_str("");
	css_cfg->tcp_keep_alive_interval = DEFAULT_TCP_KA;
	css_cfg->tls_keep_alive_interval = DEFAULT_TLS_KA;
	css_cfg->tsx_t1_timeout = PJSIP_T1_TIMEOUT;
	css_cfg->tsx_t2_timeout = PJSIP_T2_TIMEOUT;
	css_cfg->tsx_t4_timeout = PJSIP_T4_TIMEOUT;
	css_cfg->tsx_td_timeout = PJSIP_TD_TIMEOUT;
	css_cfg->disable_tcp_switch = PJ_TRUE;
	css_cfg->use_noise_suppressor = PJ_FALSE;
}

PJ_DECL(void*) get_library_factory(dynamic_factory *impl) {
	char lib_path[512];
	char init_name[512];
	FILE* file;
	pj_ansi_snprintf(lib_path, sizeof(lib_path), "%.*s",
			impl->shared_lib_path.slen, impl->shared_lib_path.ptr);
	pj_ansi_snprintf(init_name, sizeof(init_name), "%.*s",
			impl->init_factory_name.slen, impl->init_factory_name.ptr);

	void* handle = dlopen(lib_path, RTLD_LAZY);
	if (handle != NULL) {
		void* func_ptr = dlsym(handle, init_name);
		if(func_ptr == NULL){
			PJ_LOG(2, (THIS_FILE, "Invalid factory name : %s", init_name));
		}
		return func_ptr;
	} else {
		PJ_LOG(1, (THIS_FILE, "Cannot open : %s %s", lib_path, dlerror()));
	}
	return NULL;
}

//Wrap start & stop
PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
		pjsua_logging_config *log_cfg, pjsua_media_config *media_cfg,
		csipsimple_config *css_cfg, jobject context) {
	pj_status_t result;
	unsigned i;

	/* Create memory pool for application. */
	if(css_var.pool == NULL){
		css_var.pool = pjsua_pool_create("css", 1000, 1000);
		PJ_ASSERT_RETURN(css_var.pool, PJ_ENOMEM);
	}
	// Finalize configuration
	log_cfg->cb = &pj_android_log_msg;

	// Static cfg
	extern pj_bool_t pjsip_use_compact_form;
	extern pj_bool_t pjsip_include_allow_hdr_in_dlg;
	extern pj_bool_t pjmedia_add_rtpmap_for_static_pt;
	extern pj_bool_t pjmedia_add_bandwidth_tias_in_sdp;
	extern pj_bool_t pjsua_no_update;
	extern pj_bool_t pjmedia_webrtc_use_ns;

	pjsua_no_update = css_cfg->use_no_update ? PJ_TRUE : PJ_FALSE;

	pjsip_use_compact_form =
			css_cfg->use_compact_form_headers ? PJ_TRUE : PJ_FALSE;
	/* do not transmit Allow header */
	pjsip_include_allow_hdr_in_dlg =
			css_cfg->use_compact_form_headers ? PJ_FALSE : PJ_TRUE;
	/* Do not include rtpmap for static payload types (<96) */
	pjmedia_add_rtpmap_for_static_pt =
			css_cfg->use_compact_form_sdp ? PJ_FALSE : PJ_TRUE;
	/* Do not enable bandwidth information inclusion in sdp */
	pjmedia_add_bandwidth_tias_in_sdp =
			css_cfg->add_bandwidth_tias_in_sdp ? PJ_TRUE : PJ_FALSE;
	/* Use noise suppressor ? */
	pjmedia_webrtc_use_ns =
			css_cfg->use_noise_suppressor ? PJ_TRUE : PJ_FALSE;

	css_tcp_keep_alive_interval = css_cfg->tcp_keep_alive_interval;
	css_tls_keep_alive_interval = css_cfg->tls_keep_alive_interval;

	// Transaction timeouts
	pjsip_sip_cfg_var.tsx.t1 = css_cfg->tsx_t1_timeout;
	pjsip_sip_cfg_var.tsx.t2 = css_cfg->tsx_t2_timeout;
	pjsip_sip_cfg_var.tsx.t4 = css_cfg->tsx_t4_timeout;
	pjsip_sip_cfg_var.tsx.td = css_cfg->tsx_td_timeout;
	pjsip_sip_cfg_var.endpt.disable_tcp_switch = css_cfg->disable_tcp_switch;

	// Call recorder
	for(i = 0; i < PJ_ARRAY_SIZE(css_var.call_recorder_ids); i++){
		css_var.call_recorder_ids[i] = PJSUA_INVALID_ID;
		css_var.call_stereo_recoders[i].file_port = NULL;
		css_var.call_stereo_recoders[i].splitcomb_port = NULL;
		css_var.call_stereo_recoders[i].master_stereo_port = NULL;
		css_var.call_stereo_recoders[i].pool = NULL;
		css_var.call_stereo_recoders[i].splitcomb_chan0_port = NULL;
		css_var.call_stereo_recoders[i].splitcomb_chan0_slot = PJSUA_INVALID_ID;
		css_var.call_stereo_recoders[i].splitcomb_chan1_port = NULL;
		css_var.call_stereo_recoders[i].splitcomb_chan1_slot = PJSUA_INVALID_ID;

	}

	// Audio codec cfg
	css_var.extra_aud_codecs_cnt = css_cfg->extra_aud_codecs_cnt;
	for (i = 0; i < css_cfg->extra_aud_codecs_cnt; i++) {
		dynamic_factory *css_codec = &css_var.extra_aud_codecs[i];
		dynamic_factory *cfg_codec = &css_cfg->extra_aud_codecs[i];

		pj_strdup_with_null(css_var.pool, &css_codec->shared_lib_path,
				&cfg_codec->shared_lib_path);
		pj_strdup_with_null(css_var.pool, &css_codec->init_factory_name,
				&cfg_codec->init_factory_name);
	}

	// Video codec cfg -- For now only destroy is useful but for future
	// hopefully vid codec mgr will behaves as audio does
	// Also in this case destroy will become obsolete
	css_var.extra_vid_codecs_cnt = css_cfg->extra_vid_codecs_cnt;
	for (i = 0; i < css_cfg->extra_vid_codecs_cnt; i++) {
		dynamic_factory *css_codec = &css_var.extra_vid_codecs[i];
		dynamic_factory *cfg_codec = &css_cfg->extra_vid_codecs[i];

		pj_strdup_with_null(css_var.pool, &css_codec->shared_lib_path,
				&cfg_codec->shared_lib_path);
		pj_strdup_with_null(css_var.pool, &css_codec->init_factory_name,
				&cfg_codec->init_factory_name);


		css_codec = &css_var.extra_vid_codecs_destroy[i];
		cfg_codec = &css_cfg->extra_vid_codecs_destroy[i];

		pj_strdup_with_null(css_var.pool, &css_codec->shared_lib_path,
				&cfg_codec->shared_lib_path);
		pj_strdup_with_null(css_var.pool, &css_codec->init_factory_name,
				&cfg_codec->init_factory_name);

	}

	// ZRTP cfg
	css_var.default_use_zrtp = css_cfg->use_zrtp;
	ua_cfg->cb.on_create_media_transport = &on_transport_created_wrapper;

#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
	pj_ansi_snprintf(css_var.zid_file, sizeof(css_var.zid_file),
			"%.*s/simple.zid", css_cfg->storage_folder.slen,
			css_cfg->storage_folder.ptr);
#endif

	JNIEnv *jni_env = 0;
	ATTACH_JVM(jni_env);
	css_var.context = (*jni_env)->NewGlobalRef(jni_env, context);
	DETACH_JVM(jni_env);

	result = (pj_status_t) pjsua_init(ua_cfg, log_cfg, media_cfg);
	if (result == PJ_SUCCESS) {
		init_ringback_tone();

		// Init audio device
		pj_status_t added_audio = PJ_ENOTFOUND;
		if (css_cfg->audio_implementation.init_factory_name.slen > 0) {
			pjmedia_aud_dev_factory* (*init_factory)(
					pj_pool_factory *pf) = get_library_factory(&css_cfg->audio_implementation);
			if(init_factory != NULL) {
				pjmedia_aud_register_factory(init_factory);
				added_audio = PJ_SUCCESS;
				PJ_LOG(4, (THIS_FILE, "Loaded audio dev"));
			}
		}

		// Fallback to default audio dev if no one found
		if (added_audio != PJ_SUCCESS) {
			pjmedia_aud_register_factory(&pjmedia_android_factory);
		}

		// Init video device
#if PJMEDIA_HAS_VIDEO
		// load renderer
		if (css_cfg->video_render_implementation.init_factory_name.slen > 0) {
			pjmedia_vid_dev_factory* (*init_factory)(
					pj_pool_factory *pf) = get_library_factory(&css_cfg->video_render_implementation);
			if(init_factory != NULL) {
				pjmedia_vid_register_factory(init_factory, NULL);
				PJ_LOG(4, (THIS_FILE, "Loaded video render dev"));
			}
		}
		// load capture
		if (css_cfg->video_capture_implementation.init_factory_name.slen > 0) {
			pjmedia_vid_dev_factory* (*init_factory)(
								pj_pool_factory *pf) = get_library_factory(&css_cfg->video_capture_implementation);
			if(init_factory != NULL) {
				pjmedia_vid_register_factory(init_factory, NULL);
				PJ_LOG(4, (THIS_FILE, "Loaded video capture dev"));
			}
		}

		// Load ffmpeg converter
		pjmedia_converter_mgr* cvrt_mgr = pjmedia_converter_mgr_instance();
		if(css_cfg->vid_converter.init_factory_name.slen > 0){
			pj_status_t (*init_factory)(pjmedia_converter_mgr* cvrt_mgr) = get_library_factory(&css_cfg->vid_converter);
			if(init_factory != NULL) {
				init_factory(cvrt_mgr);
				PJ_LOG(4, (THIS_FILE, "Loaded video converter"));
			}
		}


		// Load video codecs
		pjmedia_vid_codec_mgr* vid_mgr = pjmedia_vid_codec_mgr_instance();

		for (i = 0; i < css_var.extra_vid_codecs_cnt; i++) {
			dynamic_factory *codec = &css_var.extra_vid_codecs[i];
			pj_status_t (*init_factory)(pjmedia_vid_codec_mgr *mgr,
                    pj_pool_factory *pf) = get_library_factory(codec);
			if(init_factory != NULL){
				pj_status_t status = init_factory(vid_mgr, &pjsua_var.cp.factory);
				if(status != PJ_SUCCESS) {
					PJ_LOG(2, (THIS_FILE,"Error loading dynamic codec plugin"));
				}
	    	}
		}

#endif
		}

	return result;
}

PJ_DECL(pj_status_t) csipsimple_destroy(unsigned flags) {
	destroy_ringback_tone();

#if PJMEDIA_HAS_VIDEO
	unsigned i;
	for (i = 0; i < css_var.extra_vid_codecs_cnt; i++) {
		dynamic_factory *codec = &css_var.extra_vid_codecs_destroy[i];
		pj_status_t (*destroy_factory)() = get_library_factory(codec);
		if(destroy_factory != NULL){
			pj_status_t status = destroy_factory();
			if(status != PJ_SUCCESS) {
				PJ_LOG(2, (THIS_FILE,"Error loading dynamic codec plugin"));
			}
    	}
	}
#endif

	if (css_var.pool) {
		pj_pool_release(css_var.pool);
		css_var.pool = NULL;
	}
	if(css_var.context){
		JNIEnv *jni_env = 0;
		ATTACH_JVM(jni_env);
		(*jni_env)->DeleteGlobalRef(jni_env, css_var.context);
		DETACH_JVM(jni_env);
	}
	return (pj_status_t) pjsua_destroy2(flags);
}


PJ_DECL(void) csipsimple_acc_config_default(csipsimple_acc_config* css_acc_cfg){
	css_acc_cfg->use_zrtp = -1;
	css_acc_cfg->p_preferred_identity.slen = 0;
}

PJ_DECL(pj_status_t) csipsimple_set_acc_user_data(pjsua_acc_config* acc_cfg, csipsimple_acc_config* css_acc_cfg){

	csipsimple_acc_config *additional_acc_cfg = PJ_POOL_ZALLOC_T(css_var.pool, csipsimple_acc_config);
	pj_memcpy(additional_acc_cfg, css_acc_cfg, sizeof(csipsimple_acc_config));
	pj_strdup(css_var.pool, &additional_acc_cfg->p_preferred_identity, &css_acc_cfg->p_preferred_identity);
	acc_cfg->user_data = additional_acc_cfg;

	return PJ_SUCCESS;
}

PJ_DECL(pj_status_t) csipsimple_init_acc_msg_data(pj_pool_t* pool, pjsua_acc_id acc_id, pjsua_msg_data* msg_data){
	csipsimple_acc_config *additional_acc_cfg = NULL;
	// P-Asserted-Identity header
	pj_str_t hp_preferred_identity_name = { "P-Preferred-Identity", 20 };

	// Sanity check
	PJ_ASSERT_RETURN(msg_data != NULL, PJ_EINVAL);


	// Get acc infos
	if(pjsua_acc_is_valid(acc_id)){
		additional_acc_cfg = (csipsimple_acc_config *) pjsua_acc_get_user_data(acc_id);
	}

	// Process additionnal config for this account
	if(additional_acc_cfg != NULL){
		if(additional_acc_cfg->p_preferred_identity.slen > 0){
			// Create new P-Asserted-Identity hdr if necessary
			pjsip_generic_string_hdr* hdr = pjsip_generic_string_hdr_create(pool,
					&hp_preferred_identity_name, &additional_acc_cfg->p_preferred_identity);
			// Push it to msg data
			pj_list_push_back(&msg_data->hdr_list, hdr);
		}
	}

	return PJ_SUCCESS;
}

PJ_DECL(pj_status_t) csipsimple_msg_data_add_string_hdr(pj_pool_t* pool, pjsua_msg_data* msg_data, pj_str_t* hdr_name, pj_str_t* hdr_value){

    // Sanity check
    PJ_ASSERT_RETURN(msg_data != NULL && hdr_name != NULL && hdr_value != NULL, PJ_EINVAL);
    if(hdr_name->slen <= 2 || hdr_value->slen <= 0){
        return PJ_EINVAL;
    }
    // Ensure it's a X- prefixed header. This is to avoid crappy usage/override of specified headers
    // That should be implemented properly elsewhere.
    if(hdr_name->ptr[0] != 'X' || hdr_name->ptr[1] != '-'){
        return PJ_EINVAL;
    }
    pjsip_generic_string_hdr* hdr = pjsip_generic_string_hdr_create(pool,
                        hdr_name, hdr_value);
    // Push it to msg data
    pj_list_push_back(&msg_data->hdr_list, hdr);
}

static void update_active_calls(const pj_str_t *new_ip_addr) {
	pjsip_tpselector tp_sel;
	pjsua_init_tpselector(0, &tp_sel); // << 0 is hard coded here for active transportId.  could be passed in if needed.
	int ndx;
	for (ndx = 0; ndx < pjsua_var.ua_cfg.max_calls; ++ndx) {
		pjsua_call *call = &pjsua_var.calls[ndx];
		if (!call->inv || call->inv->state != PJSIP_INV_STATE_CONFIRMED) {
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


PJ_DECL(pj_str_t) get_rx_data_header(const pj_str_t name, pjsip_rx_data* data){
	pjsip_generic_string_hdr *hdr =
			(pjsip_generic_string_hdr*) pjsip_msg_find_hdr_by_name(data->msg_info.msg, &name, NULL);
	if (hdr && hdr->hvalue.ptr) {
		return hdr->hvalue;
	}
	return pj_str("");
}

/**
 * On call state used to automatically ringback.
 */
PJ_DECL(void) css_on_call_state(pjsua_call_id call_id, pjsip_event *e) {
	pjsua_call_info call_info;
	pjsua_call_get_info(call_id, &call_info);

	if (call_info.state == PJSIP_INV_STATE_DISCONNECTED) {
		/* Stop all ringback for this call */
		ring_stop(call_id);
		PJ_LOG(
				3,
				(THIS_FILE, "Call %d is DISCONNECTED [reason=%d (%s)]", call_id, call_info.last_status, call_info.last_status_text.ptr));
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
			if (call_info.role == PJSIP_ROLE_UAC && code == 180
					&& msg->body == NULL
					&& call_info.media_status == PJSUA_CALL_MEDIA_NONE) {
				ringback_start();
			}

			PJ_LOG(
					3,
					(THIS_FILE, "Call %d state changed to %s (%d %.*s)", call_id, call_info.state_text.ptr, code, (int)reason.slen, reason.ptr));
		} else {
			PJ_LOG(
					3,
					(THIS_FILE, "Call %d state changed to %s", call_id, call_info.state_text.ptr));
		}
	}
}

/**
 * On call media state used to automatically ringback.
 */
PJ_DECL(void) css_on_call_media_state(pjsua_call_id call_id){
	ring_stop(call_id);
}
