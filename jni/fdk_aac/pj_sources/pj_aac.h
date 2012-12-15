
#ifndef __PJMEDIA_CODEC_AAC_CODEC_H__
#define __PJMEDIA_CODEC_AAC_CODEC_H__

/**
 * @file pj_aac.h
 * @brief AAC codec.
 */

#include <pjmedia-codec/types.h>


PJ_BEGIN_DECL

PJ_DECL(pj_status_t) pjmedia_codec_aac_init( pjmedia_endpt *endpt);
PJ_DECL(pj_status_t) pjmedia_codec_aac_deinit();

PJ_END_DECL


/**
 * @}
 */

#endif	/* __PJMEDIA_CODEC_AAC_CODEC_H__ */
