/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of pjsip_android.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "csipsimple_codecs_utils.h"

/**
 * Get nbr of codecs
 */

PJ_DECL(int) codecs_get_nbr() {
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);
	pj_status_t status = pjsua_enum_codecs(c, &count);
	if (status == PJ_SUCCESS) {
		return count;
	}
	return 0;
}

/**
 * Get codec id
 */

PJ_DECL(pj_str_t) codecs_get_id(int codec_id) {
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);

	pjsua_enum_codecs(c, &count);
	if (codec_id < count) {
		return c[codec_id].codec_id;
	}
	return pj_str((char *) "INVALID/8000/1");
}

/**
 * Get nbr of codecs
 */

PJ_DECL(int) codecs_vid_get_nbr() {
#if PJMEDIA_HAS_VIDEO
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);
	pj_status_t status = pjsua_vid_enum_codecs(c, &count);
	if (status == PJ_SUCCESS) {
		return count;
	}
#endif
	return 0;
}

/**
 * Get codec id
 */

PJ_DECL(pj_str_t) codecs_vid_get_id(int codec_id) {
#if PJMEDIA_HAS_VIDEO
	pjsua_codec_info c[32];
	unsigned i, count = PJ_ARRAY_SIZE(c);

	pjsua_vid_enum_codecs(c, &count);
	if (codec_id < count) {
		return c[codec_id].codec_id;
	}
#endif
	return pj_str((char *) "INVALID/8000/1");
}

PJ_DECL(pj_status_t) codec_set_frames_per_packet(pj_str_t codec_id,
		int frames_per_packet) {
	pjmedia_codec_param param;
	pj_status_t status;

	if(frames_per_packet <= 0){
		return PJ_EINVAL;
	}

	status = pjsua_codec_get_param(&codec_id, &param);
	if (status == PJ_SUCCESS) {
		param.setting.frm_per_pkt = frames_per_packet;
		return pjsua_codec_set_param(&codec_id, &param);
	}
	return status;
}

