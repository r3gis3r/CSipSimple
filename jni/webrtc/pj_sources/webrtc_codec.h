
#ifndef __PJMEDIA_CODEC_WEBRTC_CODEC_H__
#define __PJMEDIA_CODEC_WEBRTC_CODEC_H__

/**
 * @file webrtc_codec.h
 * @brief webRTC codec.
 */

#include <pjmedia-codec/types.h>


PJ_BEGIN_DECL

PJ_DECL(pj_status_t) pjmedia_codec_webrtc_init( pjmedia_endpt *endpt);
PJ_DECL(pj_status_t) pjmedia_codec_webrtc_deinit(void);

PJ_END_DECL


/**
 * @}
 */

#endif	/* __PJMEDIA_CODEC_WEBRTC_CODEC_H__ */

