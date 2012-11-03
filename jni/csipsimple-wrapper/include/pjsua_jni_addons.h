#ifndef __PJSUA_JNI_ADDONS_H__
#define __PJSUA_JNI_ADDONS_H__

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>
#include <pjsua-lib/pjsua_internal.h>
#include <jni.h>


PJ_BEGIN_DECL

// css config

typedef struct dynamic_factory {
	/**
	 * Path to the shared library
	 */
	pj_str_t shared_lib_path;

	/**
	 * Name of the factory function to launch to init the codec
	 */
	pj_str_t init_factory_name;
} dynamic_factory;

typedef struct csipsimple_config {
    /**
     * Use compact form for sdp
     */
	pj_bool_t use_compact_form_sdp;

	/**
	* Use compact form for header
	*/
	pj_bool_t use_compact_form_headers;

	/**
	 * Disable SDP bandwidth modifier "TIAS"
     * (RFC3890)
	 */
	pj_bool_t add_bandwidth_tias_in_sdp;

	/**
	 * For to send no update and use re-invite instead
	 */
	pj_bool_t use_no_update;

	/**
	 * Use ZRTP
	 */
	pj_bool_t use_zrtp;

	/**
	 * Number of dynamically loaded codecs
	 */
	unsigned extra_aud_codecs_cnt;

	/**
	 * Codecs to be dynamically loaded
	 */
	dynamic_factory extra_aud_codecs[64];

	/**
	 * Number of dynamically loaded codecs
	 */
	unsigned extra_vid_codecs_cnt;

	/**
	 * Codecs to be dynamically loaded
	 */
	dynamic_factory extra_vid_codecs[64];

	dynamic_factory extra_vid_codecs_destroy[64];

	dynamic_factory vid_converter;

	/**
	 * Target folder for content storage
	 */
	pj_str_t storage_folder;

	/**
	 * Audio dev implementation if empty string fallback to default
	 */
	dynamic_factory audio_implementation;

	/**
	 * Video renderer dev implementation if empty no video feature
	 */
	dynamic_factory video_render_implementation;
	/**
	 * Video capture dev implementation if empty no video feature
	 */
	dynamic_factory video_capture_implementation;

	/**
	 * Interval for tcp keep alive
	 */
	int tcp_keep_alive_interval;

	/**
	 * Interval for tls keep alive
	 */
	int tls_keep_alive_interval;

	/**
	 * Transaction T1 Timeout
	 */
	int tsx_t1_timeout;

	/**
	 * Transaction T2 Timeout
	 */
	int tsx_t2_timeout;

	/**
	 * Transaction T4 Timeout
	 */
	int tsx_t4_timeout;
	/**
	 * Transaction TD Timeout
	 */
	int tsx_td_timeout;

	/**
	 * Disable automatic switching from UDP to TCP if outgoing request
	 * is greater than 1300 bytes. See PJSIP_DONT_SWITCH_TO_TCP.
	 */
	pj_bool_t disable_tcp_switch;

	/**
	 * Enable or not noise suppressor.
	 * Only has impact if using webRTC echo canceller as backend.
	 * Disabled by default
	 */
	pj_bool_t use_noise_suppressor;

} csipsimple_config;

typedef struct csipsimple_acc_config {

	/**
	 * Use ZRTP
	 */
	int use_zrtp;

	/**
	 * P-Preferred-Identity
	 */
	pj_str_t p_preferred_identity;

} csipsimple_acc_config;


// methods
PJ_DECL(pj_status_t) send_dtmf_info(int current_call, pj_str_t digits);
PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media, const char *indent);
PJ_DECL(pj_str_t) call_secure_info(pjsua_call_id call_id);
PJ_DECL(pj_str_t) get_error_message(int status);
PJ_DECL(int) get_event_status_code(pjsip_event *e);

PJ_DECL(void) csipsimple_config_default(csipsimple_config *css_cfg);
PJ_DECL(void) csipsimple_acc_config_default(csipsimple_acc_config* css_acc_cfg);

PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
				pjsua_logging_config *log_cfg,
				pjsua_media_config *media_cfg,
				csipsimple_config *css_cfg,
				jobject context);
PJ_DECL(pj_status_t) csipsimple_destroy(unsigned flags);
PJ_DECL(pj_status_t) csipsimple_set_acc_user_data(pjsua_acc_config* acc_cfg, csipsimple_acc_config* css_acc_cfg);
PJ_DECL(pj_status_t) csipsimple_init_acc_msg_data(pjsua_acc_id acc_id, pjsua_msg_data* msg_data);
PJ_DECL(pj_status_t) pj_timer_fire(int entry_id);
PJ_DECL(pj_status_t) pjsua_acc_clean_all_registrations( pjsua_acc_id acc_id);
PJ_DECL(pj_status_t) update_transport(const pj_str_t *new_ip_addr);
PJ_DECL(pj_status_t) vid_set_android_window(pjsua_call_id call_id, jobject window);
PJ_DECL(pj_status_t) set_turn_credentials(const pj_str_t username, const pj_str_t password, const pj_str_t realm, pj_stun_auth_cred *turn_auth_cred);
PJ_DECL(pj_str_t)    get_rx_data_header(const pj_str_t name, pjsip_rx_data* data);

// App callback
PJ_DECL(void) css_on_call_state(pjsua_call_id call_id, pjsip_event *e);
PJ_DECL(void) css_on_call_media_state(pjsua_call_id call_id);

PJ_END_DECL

#endif
