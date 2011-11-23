/* $Id$ */
/* 
 * Copyright (C) 2011 Keystream AB
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA 
 */
#ifndef __PJMEDIA_CODECS_G726_H__
#define __PJMEDIA_CODECS_G726_H__

/**
 * @file pjmedia-codec/g726.h
 * @brief G726 codec.
 */

#include <pjmedia-codec/types.h>

/**
 * @defgroup PJMED_G726_CODEC G.726 Codec
 * @ingroup PJMEDIA_CODEC_CODECS
 * @brief Implementation of G.726 codec
 * @{
 *
 * This section describes functions to initialize and register G.726 codec
 * factory to the codec manager. After the codec factory has been registered,
 * application can use @ref PJMEDIA_CODEC API to manipulate the codec.
 *
 * G.726 is an ITU-T ADPCM speech codec standard covering the transmission
 * of voice at rates of 16, 24, 32, and 40 kbit/s using a sampling frequency
 * of 8 KHz.
 *
 * \section codec_setting Codec Settings
 *
 * \subsection general_setting General Settings
 *
 * General codec settings for this codec such as VAD and PLC can be 
 * manipulated through the <tt>setting</tt> field in #pjmedia_codec_param. 
 * Please see the documentation of #pjmedia_codec_param for more info.
 *
 */

PJ_BEGIN_DECL

/**
 * Initialize and register G.726 codec factory to pjmedia endpoint.
 *
 * @param endpt	    The pjmedia endpoint.
 *
 * @return	    PJ_SUCCESS on success.
 */
PJ_DECL(pj_status_t) pjmedia_codec_g726_init( pjmedia_endpt *endpt );


/**
 * Unregister G.726 codecs factory from pjmedia endpoint.
 *
 * @return	    PJ_SUCCESS on success.
 */
PJ_DECL(pj_status_t) pjmedia_codec_g726_deinit(void);


PJ_END_DECL


/**
 * @}
 */

#endif	/* __PJMEDIA_CODECS_G726_H__ */

