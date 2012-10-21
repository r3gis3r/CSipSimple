#ifndef __ZRTP_ANDROID_H__
#define __ZRTP_ANDROID_H__


#include <pjsua-lib/pjsua.h>
/**
 * ZRTP stuff
 */

pjmedia_transport* on_zrtp_transport_created(pjsua_call_id call_id,
	unsigned media_idx,
	pjmedia_transport *base_tp,
	unsigned flags);

PJ_BEGIN_DECL

typedef struct zrtp_state_info {
	pjsua_call_id call_id;
	pj_bool_t secure;
	pj_str_t sas;
	pj_str_t cipher;
	pj_bool_t sas_verified;
} zrtp_state_info;

PJ_DECL(void) jzrtp_SASVerified(pjsua_call_id call_id);
PJ_DECL(void) jzrtp_SASRevoked(pjsua_call_id call_id);
PJ_DECL(zrtp_state_info) jzrtp_getInfoFromCall(pjsua_call_id call_id);
PJ_END_DECL



#ifdef __cplusplus
extern "C" {
#endif

zrtp_state_info jzrtp_getInfoFromTransport(pjmedia_transport* tp);

#ifdef __cplusplus
}
#endif

#endif
