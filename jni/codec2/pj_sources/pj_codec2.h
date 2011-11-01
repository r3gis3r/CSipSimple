
#ifndef __PJMEDIA_CODEC2_H__
#define __PJMEDIA_CODEC2_H__

/**
 * @file codec2.h
 * @brief Codec 2
+ */

#include <pjmedia-codec/types.h>


PJ_BEGIN_DECL


/**
 * Initialize and register codec 2 factory to pjmedia endpoint.
 *
 * @param endpt		The pjmedia endpoint.
 *
 * @return		PJ_SUCCESS on success.
 */
PJ_DECL(pj_status_t) pjmedia_codec_codec2_init(pjmedia_endpt *endpt);



/**
 * Unregister codec2 factory from pjmedia endpoint.
 *
 * @return	    PJ_SUCCESS on success.
 */
PJ_DECL(pj_status_t) pjmedia_codec_codec2_deinit(void);


PJ_END_DECL

/**
 * @}
 */

#endif	/* __PJMEDIA_CODEC2_H__ */

