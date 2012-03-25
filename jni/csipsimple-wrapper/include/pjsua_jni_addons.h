#ifndef __PJSUA_JNI_ADDONS_H__
#define __PJSUA_JNI_ADDONS_H__

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>
#include <pjsua-lib/pjsua_internal.h>
#include <pjmedia_audiodev.h>
#include <android/log.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
void ringback_start();
void ring_stop(pjsua_call_id call_id);
void init_ringback_tone();
void destroy_ringback_tone();
void app_on_call_state(pjsua_call_id call_id, pjsip_event *e);
static void pj_android_log_msg(int level, const char *data, int len);
static pj_bool_t on_rx_request_tcp_hack(pjsip_rx_data *rdata);

#ifdef __cplusplus
}
#endif

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
	 * For to send no update and use re-invite instead
	 */
	pj_bool_t use_no_update;

	/**
	 * Turn username
	 */
	pj_str_t turn_username;

	/**
	 * Turn password
	 */
	pj_str_t turn_password;

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

} csipsimple_config;

typedef struct csipsimple_acc_config {

	/**
	 * Use ZRTP
	 */
	int use_zrtp;

} csipsimple_acc_config;


// methods
PJ_DECL(int) codecs_get_nbr();
PJ_DECL(pj_str_t) codecs_get_id(int codec_id) ;
PJ_DECL(int) codecs_vid_get_nbr();
PJ_DECL(pj_str_t) codecs_vid_get_id(int codec_id) ;
PJ_DECL(pj_status_t) send_dtmf_info(int current_call, pj_str_t digits);
PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media, const char *indent);
PJ_DECL(pj_str_t) call_secure_info(pjsua_call_id call_id);
PJ_DECL(pj_str_t) get_error_message(int status);

PJ_DECL(void) csipsimple_config_default(csipsimple_config *css_cfg);
PJ_DECL(void) csipsimple_acc_config_default(csipsimple_acc_config* css_acc_cfg);

PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
				pjsua_logging_config *log_cfg,
				pjsua_media_config *media_cfg,
				csipsimple_config *css_cfg,
				jobject context);
PJ_DECL(pj_status_t) csipsimple_destroy(void);
PJ_DECL(pj_status_t) csipsimple_set_acc_user_data(pjsua_acc_config* acc_cfg, csipsimple_acc_config* css_acc_cfg);
PJ_DECL(pj_status_t) pj_timer_fire(long cpj_entry);
PJ_DECL(pj_status_t) pjsua_acc_clean_all_registrations( pjsua_acc_id acc_id);
PJ_DECL(pj_status_t) update_transport(const pj_str_t *new_ip_addr);
PJ_DECL(pj_status_t) vid_set_android_window(pjsua_call_id call_id, jobject window);
PJ_END_DECL

#ifdef __cplusplus
extern "C" {
#endif
struct css_data {
    pj_pool_t	    *pool;	    /**< Pool for the css app. */

    // Audio codecs
	unsigned 		extra_aud_codecs_cnt;
	dynamic_factory 	extra_aud_codecs[64];

	// Video codecs
	unsigned 		extra_vid_codecs_cnt;
	dynamic_factory 	extra_vid_codecs[64];
	dynamic_factory 	extra_vid_codecs_destroy[64];

	// About ringback
    int			    ringback_slot;
    int			    ringback_cnt;
    pjmedia_port	   *ringback_port;
    pj_bool_t ringback_on;

    // About zrtp cfg
    pj_bool_t default_use_zrtp;
    char zid_file[512];

    jobject context;
};

extern struct css_data css_var;

#ifdef __cplusplus
}
#endif
#endif
