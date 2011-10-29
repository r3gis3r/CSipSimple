
#ifndef __PJMEDIA_CODEC_SILK_CODEC_H__
#define __PJMEDIA_CODEC_SILK_CODEC_H__

/**
 * @file silk.h
 * @brief SILK codec.
 */

#include <pjmedia-codec/types.h>


PJ_BEGIN_DECL

PJ_DECL(pj_status_t) pjmedia_codec_silk_init( pjmedia_endpt *endpt);
PJ_DECL(pj_status_t) pjmedia_codec_silk_deinit();

PJ_END_DECL


/**
 * @}
 */

#endif	/* __PJMEDIA_CODEC_SILK_CODEC_H__ */

