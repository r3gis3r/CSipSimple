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


#ifndef CSIPSIMPLE_INTERNAL_H_
#define CSIPSIMPLE_INTERNAL_H_

#include "pjsua_jni_addons.h"

PJ_BEGIN_DECL

PJ_DECL(void*) get_library_factory(dynamic_factory *impl);

struct css_stereo_recorder_data {
	pj_pool_t		*pool;

	// Stereo ports
    pjmedia_port *file_port;
    pjmedia_port *splitcomb_port;
    pjmedia_master_port *master_stereo_port;

    // Mono ports and slots
    pjmedia_port *splitcomb_chan0_port;
    pjsua_conf_port_id splitcomb_chan0_slot;
    pjmedia_port *splitcomb_chan1_port;
    pjsua_conf_port_id splitcomb_chan1_slot;

};

struct css_data {
    pj_pool_t	    *pool;	    /**< Pool for the css app. */

    // Audio codecs
	unsigned 		extra_aud_codecs_cnt;
	dynamic_factory 	extra_aud_codecs[64];

	// Video codecs
	unsigned 		extra_vid_codecs_cnt;
	dynamic_factory 	extra_vid_codecs[64];
	dynamic_factory 	extra_vid_codecs_destroy[64];

	// About ringback
    int			    ringback_slot;
    int			    ringback_cnt;
    pjmedia_port	   *ringback_port;
    pj_bool_t ringback_on;

    // About zrtp cfg
    pj_bool_t default_use_zrtp;
    char zid_file[512];

    jobject context;

    // About call recording
    pjsua_recorder_id	call_recorder_ids[PJSUA_MAX_CALLS];
    struct css_stereo_recorder_data call_stereo_recoders[PJSUA_MAX_CALLS];
};

extern struct css_data css_var;

PJ_END_DECL

#endif /* CSIPSIMPLE_INTERNAL_H_ */
