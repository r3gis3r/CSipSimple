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

#define THIS_FILE "css_codecs_utils.c"

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



// --- H264 -- get from vid_codec_util of pjsip

/* Declaration of H.264 level info */
typedef struct h264_level_info_t
{
    unsigned id;	    /* Level id.			*/
    unsigned max_mbps;	    /* Max macroblocks per second.	*/
    unsigned max_mb;	    /* Max macroblocks.			*/
    unsigned bitrate;	    /* Max bitrate (kbps).		*/
    unsigned def_w;	    /* Default width.			*/
    unsigned def_h;	    /* Default height.			*/
    unsigned def_fps;	    /* Default fps.			*/
    unsigned def_bitrate;  /* Default bitrate (kbps). */
} h264_level_info_t;

/* Get H.264 level info from specified level ID */
static pj_status_t get_h264_level_info(unsigned id, h264_level_info_t *level)
{
    unsigned i;
    const h264_level_info_t level_info[] =
    {
	{ 10,   1485,    99,     64,  176,  144, 15, 2 },
	{ 9,    1485,    99,    128,  176,  144, 15, 4 }, /*< level 1b */
	{ 11,   3000,   396,    192,  320,  240, 10, 5 },
	{ 12,   6000,   396,    384,  352,  288, 15, 10 },
	{ 13,  11880,   396,    768,  352,  288, 15, 20 },
	{ 20,  11880,   396,   2000,  352,  288, 30, 50 },
	{ 21,  19800,   792,   4000,  352,  288, 30, 100 },
	{ 22,  20250,  1620,   4000,  352,  288, 30, 128 },
	{ 30,  40500,  1620,  10000,  720,  480, 30, 256 },
	{ 31, 108000,  3600,  14000, 1280,  720, 30, 360 },
	{ 32, 216000,  5120,  20000, 1280,  720, 30, 512 },
	{ 40, 245760,  8192,  20000, 1920, 1080, 30, 512 },
	{ 41, 245760,  8192,  50000, 1920, 1080, 30, 1024 },
	{ 42, 522240,  8704,  50000, 1920, 1080, 30, 1024 },
	{ 50, 589824, 22080, 135000, 1920, 1080, 30, 3000 },
	{ 51, 983040, 36864, 240000, 1920, 1080, 30, 6000 },
    };

    for (i = 0; i < PJ_ARRAY_SIZE(level_info); ++i) {
	if (level_info[i].id == id) {
	    *level = level_info[i];
	    return PJ_SUCCESS;
	}
    }
    return PJ_ENOTFOUND;
}


PJ_DECL(pj_status_t) codec_h264_set_profile(unsigned id,
		unsigned width, unsigned height,
		unsigned fps,
		unsigned avg_kbps, unsigned max_kbps) {
	pj_status_t status = PJ_ENOTSUP;
#if PJMEDIA_HAS_VIDEO
	PJ_LOG(4, (THIS_FILE, "Set H264 profile %d %dx%d@%d %dkbps", id, width, height, fps, avg_kbps));
	pjmedia_vid_codec_param param;
	unsigned i;
	const pj_str_t codec_id = { "H264", 4 };
    const pj_str_t PROFILE_LEVEL_ID	= {"profile-level-id", 16};
	h264_level_info_t level_info;

	status = PJ_EINVAL;

	status = pjsua_vid_codec_get_param(&codec_id, &param);
	if(status != PJ_SUCCESS) {
		return status;
	}

	status = get_h264_level_info(id, &level_info);
	if(status != PJ_SUCCESS) {
		return status;
	}
	PJ_LOG(4, (THIS_FILE, "Found default infos for this level %d %dx%d@%d %d", level_info.id, level_info.def_w, level_info.def_h, level_info.def_fps, level_info.def_bitrate));

	param.enc_fmt.det.vid.size.w = (width > 0) ? width : level_info.def_w;
	param.enc_fmt.det.vid.size.h = (height > 0) ? height : level_info.def_h;
	param.enc_fmt.det.vid.fps.num = (fps > 0 ) ? fps : level_info.def_fps;
	param.enc_fmt.det.vid.fps.denum = 1;

	param.enc_fmt.det.vid.avg_bps = (avg_kbps > 0 && avg_kbps <= level_info.bitrate ) ? avg_kbps * 1000 : level_info.def_bitrate * 1000;
	param.enc_fmt.det.vid.max_bps = (max_kbps > 0 && max_kbps <= level_info.bitrate) ? max_kbps * 1000 : level_info.def_bitrate * 1000;

	// We expect here to already have fmtp_level_profile_id
	for (i = 0; i < param.dec_fmtp.cnt; ++i) {
	    if (pj_stricmp(&param.dec_fmtp.param[i].name, &PROFILE_LEVEL_ID) == 0) {
	    	if(param.dec_fmtp.param[i].val.slen >= 6) {
	    		// Unchanged profile_idc and profile_iop so +4
	    		/*
				char *p = param.dec_fmtp.param[i].val.ptr + 4;
				pj_val_to_hex_digit(id, p);
				*/
	    	}else{
		    	PJ_LOG(2, (THIS_FILE, "Impossible to set dec_fmtp"));
	    	}
	    }
	}

	status = pjsua_vid_codec_set_param(&codec_id, &param);
#endif
	return status;
}
