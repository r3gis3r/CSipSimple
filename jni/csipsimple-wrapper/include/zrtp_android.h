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

PJ_DECL(void) jzrtp_SASVerified(long zrtp_data_p);
PJ_DECL(int) jzrtp_getCallId(long zrtp_data_p);
PJ_END_DECL



#ifdef __cplusplus
extern "C" {
#endif

pj_str_t jzrtp_getInfo(pjmedia_transport* tp);

#ifdef __cplusplus
}
#endif

#endif
