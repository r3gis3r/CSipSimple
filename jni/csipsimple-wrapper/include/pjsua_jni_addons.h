#ifndef __PJSUA_JNI_ADDONS_H__
#define __PJSUA_JNI_ADDONS_H__

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>
#include <pjsua-lib/pjsua_internal.h>
#include <pjmedia_audiodev.h>
#include <android/log.h>

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

typedef struct dynamic_codec {
	/**
	 * Path to the shared library
	 */
	pj_str_t shared_lib_path;

	/**
	 * Name of the factory function to launch to init the codec
	 */
	pj_str_t init_factory_name;
} dynamic_codec;

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
	unsigned extra_codecs_cnt;

	/**
	 * Codecs to be dynamically loaded
	 */
	dynamic_codec extra_codecs[64];

} csipsimple_config;

// methods
PJ_DECL(int) codecs_get_nbr();
PJ_DECL(pj_str_t) codecs_get_id(int codec_id) ;
PJ_DECL(pj_status_t) send_dtmf_info(int current_call, pj_str_t digits);
PJ_DECL(pj_str_t) call_dump(pjsua_call_id call_id, pj_bool_t with_media, const char *indent);
PJ_DECL(pj_bool_t) can_use_tls();
PJ_DECL(pj_bool_t) can_use_srtp();
PJ_DECL(pj_str_t) call_secure_info(pjsua_call_id call_id);
PJ_DECL(pj_status_t) media_transports_create_ipv6(pjsua_transport_config rtp_cfg);
PJ_DECL(pj_str_t) get_error_message(int status);

PJ_DECL(void) csipsimple_config_default(csipsimple_config *css_cfg);
PJ_DECL(pj_status_t) csipsimple_init(pjsua_config *ua_cfg,
				pjsua_logging_config *log_cfg,
				pjsua_media_config *media_cfg,
				csipsimple_config *css_cfg);
PJ_DECL(pj_status_t) csipsimple_destroy(void);
PJ_DECL(pj_status_t) pj_timer_fire(long cpj_entry);
PJ_DECL(pj_status_t) pjsua_acc_clean_all_registrations( pjsua_acc_id acc_id);
PJ_DECL(pj_status_t) update_transport(const pj_str_t *new_ip_addr);

PJ_END_DECL

#ifdef __cplusplus
extern "C" {
#endif
struct css_data {
    pj_pool_t	    *pool;	    /**< Pool for the css app. */

    // About codecs
	unsigned 		extra_codecs_cnt;
	dynamic_codec 	extra_codecs[64];

	// About ringback
    int			    ringback_slot;
    int			    ringback_cnt;
    pjmedia_port	   *ringback_port;
    pj_bool_t ringback_on;
};

extern struct css_data css_var;

#ifdef __cplusplus
}
#endif
#endif
