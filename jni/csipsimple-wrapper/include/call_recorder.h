/*
 * call_recorder.h
 *
 *  Created on: 11 juin 2012
 *      Author: r3gis3r
 */

#ifndef CALL_RECORDER_H_
#define CALL_RECORDER_H_

#include <pj/config_site.h>
#include <pjsua-lib/pjsua.h>

PJ_BEGIN_DECL

/**
 * Start call recording.
 * @param call_id The identifier of the call to record
 * @param file The file path to save recording to
 * @param stereo Record each channel in stereo mode
 * @return Status of record start.
 */
PJ_DECL(pj_status_t) call_recording_start(pjsua_call_id call_id, const pj_str_t *file, pj_bool_t stereo);
/**
 * Stop an ongoing call recording.
 * @param call_id The identifier of the call to stop record of
 * @return Status of record stop.
 */
PJ_DECL(pj_status_t) call_recording_stop(pjsua_call_id call_id);
/**
 * Get recording status for a call.
 * @param call_id The identifier of the call to get call status
 * @return PJ_TRUE if call recording is ongoing
 */
PJ_DECL(pj_bool_t) call_recording_status(pjsua_call_id call_id);

PJ_END_DECL

#endif /* CALL_RECORDER_H_ */
