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

PJ_DECL(void) jzrtp_SASVerified();

PJ_END_DECL


#endif
