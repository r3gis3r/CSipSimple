/*
 * ringback_tone.h
 *
 *  Created on: 29 mai 2012
 *      Author: r3gis3r
 */

#ifndef RINGBACK_TONE_H_
#define RINGBACK_TONE_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

PJ_BEGIN_DECL

PJ_DECL(void) ringback_start();
PJ_DECL(void) ring_stop(pjsua_call_id call_id);
PJ_DECL(void) init_ringback_tone();
PJ_DECL(void) destroy_ringback_tone();

PJ_END_DECL

#endif /* RINGBACK_TONE_H_ */
