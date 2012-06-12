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

#include "call_recorder.h"
#include "csipsimple_internal.h"

#define THIS_FILE "call_recorder.c"

#define STEREO_RECORDER_ID -2

PJ_DECL(pj_status_t) call_recording_start(pjsua_call_id call_id, const pj_str_t *file, pj_bool_t stereo){
	pj_status_t status = PJ_EINVAL;
	pjsua_conf_port_id rec_port;
	pjsua_call_info call_info;
    char path[PJ_MAXPATH];

	// If nothing is recording currently, create recorder
	if (file != NULL && file->slen > 0) {
		if (css_var.call_recorder_ids[call_id] == PJSUA_INVALID_ID) {
			if (stereo) {
				// TODO --- very first implementation -- no error check -- should be done !!!!!
				// TODO --- allocation totally unclean !!!!
				// Device port => Chan 0 of Split/comb => Wav file writer opened in stereo
				// Call port   => Chan 1 of Split/comb __/

				struct css_stereo_recorder_data *rec_datas = &css_var.call_stereo_recoders[call_id];

				pj_memcpy(path, file->ptr, file->slen);
				path[file->slen] = '\0';

				rec_datas->pool = pjsua_pool_create("stereo_recorder", 1000, 1000);

				/* Create stereo wave file writer */
				status = pjmedia_wav_writer_port_create(rec_datas->pool, path,
						pjsua_var.media_cfg.clock_rate, 2,
						2 * pjsua_var.mconf_cfg.samples_per_frame,
						pjsua_var.mconf_cfg.bits_per_sample, 0, 0, &rec_datas->file_port);
				PJ_LOG(4, (THIS_FILE, "Wav writter created, %d", status));

				/* Create stereo-mono splitter/combiner */
				status = pjmedia_splitcomb_create(rec_datas->pool,
						pjsua_var.media_cfg.clock_rate /* clock rate */,
						2 /* stereo */, 2 * pjsua_var.mconf_cfg.samples_per_frame,
						pjsua_var.mconf_cfg.bits_per_sample, 0 /* options */,
						&rec_datas->splitcomb_port);
				PJ_LOG(4, (THIS_FILE, "SC created, %d", status));

				/* Create the master sound for connecting splitcomb and wav writter*/
				status = pjmedia_master_port_create(rec_datas->pool, rec_datas->splitcomb_port, rec_datas->file_port, 0,
						&rec_datas->master_stereo_port);
				PJ_LOG(4, (THIS_FILE, "Master port created, %d", status));

				/* Get each channel for right and left splitcomb port */
				// Channel 0
				status = pjmedia_splitcomb_create_rev_channel(rec_datas->pool, rec_datas->splitcomb_port,
						0 /* ch0 */, 0 /* options */, &rec_datas->splitcomb_chan0_port);
				PJ_LOG(4, (THIS_FILE, "SC port created [0], %d", status));
				status = pjsua_conf_add_port(rec_datas->pool, rec_datas->splitcomb_chan0_port, &rec_datas->splitcomb_chan0_slot);
				PJ_LOG(4, (THIS_FILE, "Conf port added [0], %d", rec_datas->splitcomb_chan0_slot));

				// Channel 1
				status = pjmedia_splitcomb_create_rev_channel(rec_datas->pool, rec_datas->splitcomb_port,
						1 /* ch1 */, 0 /* options */, &rec_datas->splitcomb_chan1_port);
				PJ_LOG(4, (THIS_FILE, "SC port created [1], %d", status));
				status = pjsua_conf_add_port(rec_datas->pool, rec_datas->splitcomb_chan1_port, &rec_datas->splitcomb_chan1_slot);
				PJ_LOG(4, (THIS_FILE, "Conf port added [1], %d", rec_datas->splitcomb_chan1_slot));

				// Fake for now
				css_var.call_recorder_ids[call_id] = STEREO_RECORDER_ID;
			} else {
				status = pjsua_recorder_create(file, 0, NULL, 0, 0,
						&css_var.call_recorder_ids[call_id]);
				PJ_LOG(4, (THIS_FILE, "File creation status is %d", status));
			}
		}
	}

    // If a recorded has been created, connect call port to recorder and sound port (0) to recorder.
	status = pjsua_call_get_info(call_id, &call_info);

	if (status == PJ_SUCCESS) {
		if (css_var.call_recorder_ids[call_id] != PJSUA_INVALID_ID) {
			if (stereo) {
				struct css_stereo_recorder_data *rec_datas = &css_var.call_stereo_recoders[call_id];

				pjsua_conf_connect(call_info.conf_slot, rec_datas->splitcomb_chan0_slot);
				pjsua_conf_connect(0,                   rec_datas->splitcomb_chan1_slot);

				pjmedia_master_port_start(rec_datas->master_stereo_port);
				return PJ_SUCCESS;
			} else {
				PJ_LOG(4, (THIS_FILE, "Start recording call %d", call_id));
				rec_port = pjsua_recorder_get_conf_port(
						css_var.call_recorder_ids[call_id]);
				pjsua_conf_connect(call_info.conf_slot, rec_port);
				pjsua_conf_connect(0, rec_port);
				return PJ_SUCCESS;
			}
		}
	}

    return status;
}

PJ_DECL(pj_status_t) call_recording_stop(pjsua_call_id call_id){
	pj_status_t status = PJ_EIGNORED;
    if( css_var.call_recorder_ids[call_id] != PJSUA_INVALID_ID) {
    	PJ_LOG(4, (THIS_FILE, "Stop recording call %d", call_id));
    	if(css_var.call_recorder_ids[call_id] == STEREO_RECORDER_ID){
			struct css_stereo_recorder_data *rec_datas = &css_var.call_stereo_recoders[call_id];
			// Stop master port that fill from sc to file writer
			pjmedia_master_port_stop(rec_datas->master_stereo_port);

			// Destroy split comb chanel ports
    		if (rec_datas->splitcomb_chan0_port) {
    			pjsua_conf_remove_port(rec_datas->splitcomb_chan0_slot);
    			rec_datas->splitcomb_chan0_slot = PJSUA_INVALID_ID;
    			pjmedia_port_destroy(rec_datas->splitcomb_chan0_port);
    			rec_datas->splitcomb_chan0_port = NULL;
    		}
    		if (rec_datas->splitcomb_chan1_port) {
    			pjsua_conf_remove_port(rec_datas->splitcomb_chan1_slot);
    			rec_datas->splitcomb_chan1_slot = PJSUA_INVALID_ID;
    			pjmedia_port_destroy(rec_datas->splitcomb_chan1_port);
    			rec_datas->splitcomb_chan1_port = NULL;
    		}

    		// This will also destroy splitcomb_port, file_port
    		if(rec_datas->master_stereo_port){
				pjmedia_master_port_destroy(rec_datas->master_stereo_port, PJ_TRUE);
				rec_datas->splitcomb_port = NULL;
				// TODO : should we hide these ports since managed by master port?
				rec_datas->file_port = NULL;
				rec_datas->master_stereo_port = NULL;
    		}

    		if(rec_datas->pool != NULL){
				pj_pool_release(rec_datas->pool);
				rec_datas->pool = NULL;
    		}
    	}else{
			status = pjsua_recorder_destroy(css_var.call_recorder_ids[call_id]);
    	}
    	css_var.call_recorder_ids[call_id] = PJSUA_INVALID_ID;
    }
    return status;
}


PJ_DECL(pj_bool_t) call_recording_status(pjsua_call_id call_id) {
	return ( css_var.call_recorder_ids[call_id] != PJSUA_INVALID_ID );
}

