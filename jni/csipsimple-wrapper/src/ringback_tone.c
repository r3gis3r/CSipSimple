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

#include "ringback_tone.h"
#include "csipsimple_internal.h"

//---------------------
// RINGBACK MANAGEMENT-
// --------------------
/* Ringtones		    US	       UK  */
#define RINGBACK_FREQ1	    440	    /* 400 */
#define RINGBACK_FREQ2	    480	    /* 450 */
#define RINGBACK_ON	    2000    /* 400 */
#define RINGBACK_OFF	    4000    /* 200 */
#define RINGBACK_CNT	    1	    /* 2   */
#define RINGBACK_INTERVAL   4000    /* 2000 */

void ringback_start() {
	if (css_var.ringback_on) {
		//Already ringing back
		return;
	}
	css_var.ringback_on = PJ_TRUE;
	if (++css_var.ringback_cnt == 1 && css_var.ringback_slot != PJSUA_INVALID_ID) {
		pjsua_conf_connect(css_var.ringback_slot, 0);
	}
}

void ring_stop(pjsua_call_id call_id) {
	if (css_var.ringback_on) {
		css_var.ringback_on = PJ_FALSE;

		pj_assert(css_var.ringback_cnt>0);
		if (--css_var.ringback_cnt == 0 &&
		css_var.ringback_slot!=PJSUA_INVALID_ID) {pjsua_conf_disconnect(css_var.ringback_slot, 0);
		pjmedia_tonegen_rewind(css_var.ringback_port);
	}
}
}

void init_ringback_tone() {
	pj_status_t status;
	pj_str_t name;
	pjmedia_tone_desc tone[RINGBACK_CNT];
	unsigned i;

	css_var.ringback_slot = PJSUA_INVALID_ID;
	css_var.ringback_on = PJ_FALSE;
	css_var.ringback_cnt = 0;

	//Ringback
	name = pj_str((char *) "ringback");
	status = pjmedia_tonegen_create2(css_var.pool, &name, 16000, 1, 320, 16,
			PJMEDIA_TONEGEN_LOOP, &css_var.ringback_port);
	if (status != PJ_SUCCESS) {
		goto on_error;
	}

	pj_bzero(&tone, sizeof(tone));
	for (i = 0; i < RINGBACK_CNT; ++i) {
		tone[i].freq1 = RINGBACK_FREQ1;
		tone[i].freq2 = RINGBACK_FREQ2;
		tone[i].on_msec = RINGBACK_ON;
		tone[i].off_msec = RINGBACK_OFF;
	}
	tone[RINGBACK_CNT - 1].off_msec = RINGBACK_INTERVAL;
	pjmedia_tonegen_play(css_var.ringback_port, RINGBACK_CNT, tone,
			PJMEDIA_TONEGEN_LOOP);
	status = pjsua_conf_add_port(css_var.pool, css_var.ringback_port,
			&css_var.ringback_slot);
	if (status != PJ_SUCCESS) {
		goto on_error;
	}
	return;

	on_error: return;
}

void destroy_ringback_tone() {
	/* Close ringback port */
	if (css_var.ringback_port && css_var.ringback_slot != PJSUA_INVALID_ID) {
		pjsua_conf_remove_port(css_var.ringback_slot);
		css_var.ringback_slot = PJSUA_INVALID_ID;
		pjmedia_port_destroy(css_var.ringback_port);
		css_var.ringback_port = NULL;
	}

}
