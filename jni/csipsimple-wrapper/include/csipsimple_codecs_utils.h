/*
 * csipsimple_codecs_utils.h
 *
 *  Created on: 2 juin 2012
 *      Author: r3gis3r
 */

#ifndef CSIPSIMPLE_CODECS_UTILS_H_
#define CSIPSIMPLE_CODECS_UTILS_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

PJ_BEGIN_DECL


PJ_DECL(int) codecs_get_nbr();
PJ_DECL(pj_str_t) codecs_get_id(int codec_id);
PJ_DECL(int) codecs_vid_get_nbr();
PJ_DECL(pj_str_t) codecs_vid_get_id(int codec_id);
PJ_DECL(pj_status_t) codec_set_frames_per_packet(pj_str_t codec_id,
		int frames_per_packet);
PJ_DECL(pj_status_t) codec_h264_set_profile(unsigned profile_id, unsigned level_id,
		unsigned width, unsigned height,
		unsigned fps,
		unsigned avg_kbps, unsigned max_kbps);

PJ_END_DECL

#endif /* CSIPSIMPLE_CODECS_UTILS_H_ */
