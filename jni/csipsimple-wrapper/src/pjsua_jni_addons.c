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
 * Get nbr of codecs
 */PJ_DECL(int) codecs_get_nbr() {
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
 */PJ_DECL(pj_str_t) codecs_get_id(int codec_id) {
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);

	pjsua_enum_codecs(c, &count);
	if (codec_id < count) {
		return c[codec_id].codec_id;
	}
	return pj_str((char *) "INVALID/8000/1");
}



/**
 * Get nbr of codecs
 */
PJ_DECL(int) codecs_vid_get_nbr() {
#if PJMEDIA_HAS_VIDEO
 	pjsua_codec_info c[32];
 	unsigned i, count = PJ_ARRAY_SIZE(c);
 	pj_status_t status = pjsua_vid_enum_codecs(c, &count);
 	if (status == PJ_SUCCESS) {
 		return count;
 	}
#endif
 	return 0;
 }

/**
 * Get codec id
 */
 PJ_DECL(pj_str_t) codecs_vid_get_id(int codec_id) {
#if PJMEDIA_HAS_VIDEO
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);

	pjsua_vid_enum_codecs(c, &count);
	if (codec_id < count) {
		return c[codec_id].codec_id;
	}
#endif
	return pj_str((char *) "INVALID/8000/1");
}

/**
 * Get call infos
 */PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media,
		const char *indent) {
	char some_buf[1024 * 3];
	pjsua_call_dump(call_id, with_media, some_buf, sizeof(some_buf), indent);
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
 * Is call using a secure RTP method (SRTP/ZRTP -- TODO)
 */
PJ_DECL(pj_str_t) call_secure_info(pjsua_call_id call_id) {

	pjsua_call *call;
	pjsip_dialog *dlg;
	pj_status_t status;
	unsigned i;
	pjmedia_transport_info tp_info;

	pj_str_t result = pj_str("");
	PJ_LOG(3, (THIS_FILE, "Get call secure info..."));

	PJ_ASSERT_RETURN(call_id>=0 && call_id<(int)pjsua_var.ua_cfg.max_calls,
			result);

	/* Use PJSUA_LOCK() instead of acquire_call():
	 *  https://trac.pjsip.org/repos/ticket/1371
	 */
	PJSUA_LOCK();

	if (pjsua_call_has_media(call_id)) {
		call = &pjsua_var.calls[call_id];
		for (i = 0; i < call->med_cnt; ++i) {
			pjsua_call_media *call_med = &call->media[i];
			PJ_LOG(4, (THIS_FILE, "Get secure for media type %d", call_med->type));
			/* Get and ICE SRTP status */
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
							pjmedia_zrtp_info *zrtp_info =
									(pjmedia_zrtp_info*) tp_info.spc_info[j].buffer;

							//	if(zrtp_info->active){
							result = jzrtp_getInfo(call_med->tp);
							break;
							//	}
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
		}
	}

	PJSUA_UNLOCK();
	return status;
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
	css_cfg->use_no_update = PJ_FALSE;
	css_cfg->use_zrtp = PJ_FALSE;
	css_cfg->extra_aud_codecs_cnt = 0;
	css_cfg->extra_vid_codecs_cnt = 0;
	css_cfg->audio_implementation.init_factory_name = pj_str("");
	css_cfg->audio_implementation.shared_lib_path = pj_str("");
	css_cfg->tcp_keep_alive_interval = DEFAULT_TCP_KA;
	css_cfg->tls_keep_alive_interval = DEFAULT_TLS_KA;
}

static void* get_library_factory(dynamic_factory *impl) {
	char lib_path[512];
	char init_name[512];
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
		PJ_LOG(1, (THIS_FILE, "Not found lib : %s", lib_path));
	}
	return NULL;
}

//Wrap start & stop
PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
		pjsua_logging_config *log_cfg, pjsua_media_config *media_cfg,
		csipsimple_config *css_cfg, jobject context) {
	pj_status_t result;

	/* Create memory pool for application. */
	css_var.pool = pjsua_pool_create("css", 1000, 1000);
	PJ_ASSERT_RETURN(css_var.pool, PJ_ENOMEM);

	// Finalize configuration
	log_cfg->cb = &pj_android_log_msg;
	if (css_cfg->turn_username.slen) {
		media_cfg->turn_auth_cred.type = PJ_STUN_AUTH_CRED_STATIC;
		media_cfg->turn_auth_cred.data.static_cred.realm = pj_str("*");
		pj_strdup_with_null(css_var.pool,
				&media_cfg->turn_auth_cred.data.static_cred.username,
				&css_cfg->turn_username);

		if (css_cfg->turn_password.slen) {
			media_cfg->turn_auth_cred.data.static_cred.data_type =
					PJ_STUN_PASSWD_PLAIN;
			pj_strdup_with_null(css_var.pool,
					&media_cfg->turn_auth_cred.data.static_cred.data,
					&css_cfg->turn_password);
		}
	}

	// Static cfg
	extern pj_bool_t pjsip_use_compact_form;
	extern pj_bool_t pjsip_include_allow_hdr_in_dlg;
	extern pj_bool_t pjmedia_add_rtpmap_for_static_pt;
	extern pj_bool_t pjsua_no_update;

	pjsua_no_update = css_cfg->use_no_update ? PJ_TRUE : PJ_FALSE;

	pjsip_use_compact_form =
			css_cfg->use_compact_form_headers ? PJ_TRUE : PJ_FALSE;
	/* do not transmit Allow header */
	pjsip_include_allow_hdr_in_dlg =
			css_cfg->use_compact_form_headers ? PJ_FALSE : PJ_TRUE;
	/* Do not include rtpmap for static payload types (<96) */
	pjmedia_add_rtpmap_for_static_pt =
			css_cfg->use_compact_form_sdp ? PJ_FALSE : PJ_TRUE;

	css_tcp_keep_alive_interval = css_cfg->tcp_keep_alive_interval;
	css_tls_keep_alive_interval = css_cfg->tls_keep_alive_interval;

	// Audio codec cfg
	css_var.extra_aud_codecs_cnt = css_cfg->extra_aud_codecs_cnt;
	unsigned i;
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
#if defined(PJMEDIA_HAS_ZRTP) && PJMEDIA_HAS_ZRTP!=0
	if (css_cfg->use_zrtp) {
		ua_cfg->cb.on_create_media_transport = &on_zrtp_transport_created;
	}

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
				pjmedia_vid_register_factory(init_factory);
				PJ_LOG(4, (THIS_FILE, "Loaded video render dev"));
			}
		}
		// load capture
		if (css_cfg->video_capture_implementation.init_factory_name.slen > 0) {
			pjmedia_vid_dev_factory* (*init_factory)(
								pj_pool_factory *pf) = get_library_factory(&css_cfg->video_capture_implementation);
			if(init_factory != NULL) {
				pjmedia_vid_register_factory(init_factory);
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

PJ_DECL(pj_status_t) csipsimple_destroy(void) {
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
	return (pj_status_t) pjsua_destroy();
}

void update_active_calls(const pj_str_t *new_ip_addr) {
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

void ringback_start() {
	if (css_var.ringback_on) {
		//Already ringing back
		return;
	}
	css_var.ringback_on = PJ_TRUE;
	if (++css_var.ringback_cnt == 1 && css_var.ringback_slot != PJSUA_INVALID_ID) {
		pjsua_conf_connect(css_var.ringback_slot, 0);
	}
}

void ring_stop(pjsua_call_id call_id) {
	if (css_var.ringback_on) {
		css_var.ringback_on = PJ_FALSE;

		pj_assert(css_var.ringback_cnt>0);
		if (--css_var.ringback_cnt == 0 &&
		css_var.ringback_slot!=PJSUA_INVALID_ID) {pjsua_conf_disconnect(css_var.ringback_slot, 0);
		pjmedia_tonegen_rewind(css_var.ringback_port);
	}
}
}

void init_ringback_tone() {
	pj_status_t status;
	pj_str_t name;
	pjmedia_tone_desc tone[RINGBACK_CNT];
	unsigned i;

	css_var.ringback_slot = PJSUA_INVALID_ID;
	css_var.ringback_on = PJ_FALSE;
	css_var.ringback_cnt = 0;

	//Ringback
	name = pj_str((char *) "ringback");
	status = pjmedia_tonegen_create2(css_var.pool, &name, 16000, 1, 320, 16,
			PJMEDIA_TONEGEN_LOOP, &css_var.ringback_port);
	if (status != PJ_SUCCESS) {
		goto on_error;
	}

	pj_bzero(&tone, sizeof(tone));
	for (i = 0; i < RINGBACK_CNT; ++i) {
		tone[i].freq1 = RINGBACK_FREQ1;
		tone[i].freq2 = RINGBACK_FREQ2;
		tone[i].on_msec = RINGBACK_ON;
		tone[i].off_msec = RINGBACK_OFF;
	}
	tone[RINGBACK_CNT - 1].off_msec = RINGBACK_INTERVAL;
	pjmedia_tonegen_play(css_var.ringback_port, RINGBACK_CNT, tone,
			PJMEDIA_TONEGEN_LOOP);
	status = pjsua_conf_add_port(css_var.pool, css_var.ringback_port,
			&css_var.ringback_slot);
	if (status != PJ_SUCCESS) {
		goto on_error;
	}
	return;

	on_error: return;
}

void destroy_ringback_tone() {
	/* Close ringback port */
	if (css_var.ringback_port && css_var.ringback_slot != PJSUA_INVALID_ID) {
		pjsua_conf_remove_port(css_var.ringback_slot);
		css_var.ringback_slot = PJSUA_INVALID_ID;
		pjmedia_port_destroy(css_var.ringback_port);
		css_var.ringback_port = NULL;
	}

}

/**
 * On call state used to automatically ringback.
 */
void app_on_call_state(pjsua_call_id call_id, pjsip_event *e) {
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

// Codec loader override

#include <pjmedia-codec.h>
#include <pjmedia/g711.h>

#if PJMEDIA_HAS_WEBRTC_CODEC
#include <webrtc_codec.h>
#endif

#if PJMEDIA_HAS_AMR_STAGEFRIGHT_CODEC
#include <amr_stagefright_dyn_codec.h>
#endif

#if PJMEDIA_HAS_G729_CODEC
#include <pj_g729.h>
#endif

PJ_DEF(void) pjmedia_audio_codec_config_default(pjmedia_audio_codec_config*cfg) {
	pj_bzero(cfg, sizeof(*cfg));
	cfg->speex.option = 0;
	cfg->speex.quality = PJMEDIA_CODEC_SPEEX_DEFAULT_QUALITY;
	cfg->speex.complexity = PJMEDIA_CODEC_SPEEX_DEFAULT_COMPLEXITY;
	cfg->ilbc.mode = 30;
	cfg->passthrough.setting.ilbc_mode = cfg->ilbc.mode;
}

PJ_DEF(pj_status_t) pjmedia_codec_register_audio_codecs(pjmedia_endpt *endpt,
		const pjmedia_audio_codec_config *c) {
	pjmedia_audio_codec_config default_cfg;
	pj_status_t status;

	PJ_ASSERT_RETURN(endpt, PJ_EINVAL);
	if (!c) {
		pjmedia_audio_codec_config_default(&default_cfg);
		c = &default_cfg;
	}

	PJ_ASSERT_RETURN(c->ilbc.mode==20 || c->ilbc.mode==30, PJ_EINVAL);

#if PJMEDIA_HAS_PASSTHROUGH_CODECS
	status = pjmedia_codec_passthrough_init2(endpt, &c->passthough.ilbc);
	if (status != PJ_SUCCESS)
	return status;
#endif

#if PJMEDIA_HAS_SPEEX_CODEC
	/* Register speex. */
	status = pjmedia_codec_speex_init(endpt, c->speex.option, c->speex.quality,
			c->speex.complexity);
	if (status != PJ_SUCCESS)
		return status;
#endif

#if PJMEDIA_HAS_ILBC_CODEC
	/* Register iLBC. */
	status = pjmedia_codec_ilbc_init(endpt, c->ilbc.mode);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_ILBC_CODEC */

#if PJMEDIA_HAS_GSM_CODEC
	/* Register GSM */
	status = pjmedia_codec_gsm_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_GSM_CODEC */

#if PJMEDIA_HAS_G711_CODEC
	/* Register PCMA and PCMU */
	status = pjmedia_codec_g711_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif	/* PJMEDIA_HAS_G711_CODEC */

#if PJMEDIA_HAS_G722_CODEC
	status = pjmedia_codec_g722_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif  /* PJMEDIA_HAS_G722_CODEC */

#if PJMEDIA_HAS_L16_CODEC
	/* Register L16 family codecs */
	status = pjmedia_codec_l16_init(endpt, 0);
	if (status != PJ_SUCCESS)
	return status;
#endif	/* PJMEDIA_HAS_L16_CODEC */

#if PJMEDIA_HAS_OPENCORE_AMRNB_CODEC || PJMEDIA_HAS_AMR_STAGEFRIGHT_CODEC
	/* Register OpenCORE AMR-NB */
	status = pjmedia_codec_opencore_amrnb_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif

#if PJMEDIA_HAS_WEBRTC_CODEC
	/* Register WEBRTC */
	status = pjmedia_codec_webrtc_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_WEBRTC_CODEC */

#if PJMEDIA_HAS_G729_CODEC
	/* Register WEBRTC */
	status = pjmedia_codec_g729_init(endpt);
	if (status != PJ_SUCCESS)
		return status;
#endif /* PJMEDIA_HAS_G729_CODEC */

	// Dynamic loading of plugins codecs
	unsigned i;

	for (i = 0; i < css_var.extra_aud_codecs_cnt; i++) {
		dynamic_factory *codec = &css_var.extra_aud_codecs[i];
		pj_status_t (*init_factory)(
							pjmedia_endpt *endpt) = get_library_factory(codec);
		if(init_factory != NULL){
			status = init_factory(endpt);
			if(status != PJ_SUCCESS) {
				PJ_LOG(2, (THIS_FILE,"Error loading dynamic codec plugin"));
			}
    	}
	}

	return PJ_SUCCESS;
}

