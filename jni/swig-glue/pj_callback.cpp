#include "pj_callback.h"

static Callback* registeredCallbackObject = NULL;

#define THIS_FILE "pj_callback.cpp"

extern "C" {


void on_call_state_wrapper(pjsua_call_id call_id, pjsip_event *e) {
	registeredCallbackObject->on_call_state(call_id, e);
}

void on_incoming_call_wrapper (pjsua_acc_id acc_id, pjsua_call_id call_id,
	pjsip_rx_data *rdata) {
	registeredCallbackObject->on_incoming_call(acc_id, call_id, rdata);
}

void on_call_tsx_state_wrapper (pjsua_call_id call_id,
		pjsip_transaction *tsx,
		pjsip_event *e) {
	registeredCallbackObject->on_call_tsx_state(call_id, tsx, e);
}

void on_call_media_state_wrapper (pjsua_call_id call_id) {
	registeredCallbackObject->on_call_media_state(call_id);
}

void on_call_sdp_created_wrapper (pjsua_call_id call_id,
	                                pjmedia_sdp_session *sdp,
	                                pj_pool_t *pool,
	                                const pjmedia_sdp_session *rem_sdp) {
	registeredCallbackObject->on_call_sdp_created(call_id, sdp, pool, rem_sdp);
}

void on_stream_created_wrapper (pjsua_call_id call_id,
		pjmedia_stream *strm,
		unsigned stream_idx,
		pjmedia_port **p_port) {
	registeredCallbackObject->on_stream_created(call_id, strm, stream_idx, p_port);
}

void on_stream_destroyed_wrapper (pjsua_call_id call_id,
	pjmedia_stream *strm,
	unsigned stream_idx) {
	registeredCallbackObject->on_stream_destroyed(call_id, strm, stream_idx);
}

void on_dtmf_digit_wrapper (pjsua_call_id call_id, int digit) {
	registeredCallbackObject->on_dtmf_digit(call_id, digit);
}

void on_call_transfer_request_wrapper (pjsua_call_id call_id,
	const pj_str_t *dst,
	pjsip_status_code *code) {
	registeredCallbackObject->on_call_transfer_request(call_id, dst, code);
}

void on_call_transfer_status_wrapper (pjsua_call_id call_id,
	int st_code,
	const pj_str_t *st_text,
	pj_bool_t final_,
	pj_bool_t *p_cont) {
	registeredCallbackObject->on_call_transfer_status(call_id, st_code, st_text, final_, p_cont);
}

void on_call_replace_request_wrapper (pjsua_call_id call_id,
	pjsip_rx_data *rdata,
	int *st_code,
	pj_str_t *st_text) {
	registeredCallbackObject->on_call_replace_request(call_id, rdata, st_code, st_text);
}

void on_call_replaced_wrapper (pjsua_call_id old_call_id,
	pjsua_call_id new_call_id) {
	registeredCallbackObject->on_call_replaced(old_call_id, new_call_id);
}

void on_reg_state_wrapper (pjsua_acc_id acc_id) {
	registeredCallbackObject->on_reg_state(acc_id);
}

void on_buddy_state_wrapper (pjsua_buddy_id buddy_id) {
	registeredCallbackObject->on_buddy_state(buddy_id);
}

void on_pager_wrapper (pjsua_call_id call_id, const pj_str_t *from,
	const pj_str_t *to, const pj_str_t *contact,
	const pj_str_t *mime_type, const pj_str_t *body) {
	registeredCallbackObject->on_pager(call_id, from, to, contact, mime_type, body);
}

void on_pager2_wrapper (pjsua_call_id call_id, const pj_str_t *from,
	const pj_str_t *to, const pj_str_t *contact,
	const pj_str_t *mime_type, const pj_str_t *body,
	pjsip_rx_data *rdata, pjsua_acc_id acc_id) {
	registeredCallbackObject->on_pager2(call_id, from, to, contact, mime_type, body, rdata);
}

void on_pager_status_wrapper (pjsua_call_id call_id,
	const pj_str_t *to,
	const pj_str_t *body,
	void *user_data,
	pjsip_status_code status,
	const pj_str_t *reason) {
	registeredCallbackObject->on_pager_status(call_id, to, body, /*XXX user_data,*/ status, reason);
}

void on_pager_status2_wrapper (pjsua_call_id call_id,
	const pj_str_t *to,
	const pj_str_t *body,
	void *user_data,
	pjsip_status_code status,
	const pj_str_t *reason,
	pjsip_tx_data *tdata,
	pjsip_rx_data *rdata, pjsua_acc_id acc_id) {
	registeredCallbackObject->on_pager_status2(call_id, to, body, /*XXX user_data,*/ status, reason, tdata, rdata);
}

void on_typing_wrapper (pjsua_call_id call_id, const pj_str_t *from,
	const pj_str_t *to, const pj_str_t *contact,
	pj_bool_t is_typing) {
	registeredCallbackObject->on_typing(call_id, from, to, contact, is_typing);
}

void on_nat_detect_wrapper (const pj_stun_nat_detect_result *res) {
	registeredCallbackObject->on_nat_detect(res);
}


pjsip_redirect_op on_call_redirected_wrapper (pjsua_call_id call_id, const pjsip_uri *target, const pjsip_event *e) {
	char uristr[PJSIP_MAX_URL_SIZE];
	int len;
	pj_str_t uri_pstr;

	len = pjsip_uri_print(PJSIP_URI_IN_FROMTO_HDR, target, uristr,
			      sizeof(uristr));
	if (len < 1) {
	    pj_ansi_strcpy(uristr, "--URI too long--");
	}

	uri_pstr = pj_str(uristr);

	return registeredCallbackObject->on_call_redirected(call_id, &uri_pstr);
}

void on_mwi_info_wrapper (pjsua_acc_id acc_id, pjsua_mwi_info *mwi_info) {
	pj_str_t body;
	pj_str_t mime_type;
	char mime_type_c[80];

	// Ignore empty messages
	if (!mwi_info->rdata->msg_info.msg->body) {
		PJ_LOG(4, (THIS_FILE, "MWI info has no body"));
		return;
	}

	// Get the mime type
	if (mwi_info->rdata->msg_info.ctype) {
    	const pjsip_ctype_hdr *ctype = mwi_info->rdata->msg_info.ctype;
    	pj_ansi_snprintf(mime_type_c, sizeof(mime_type_c),
    		  "%.*s/%.*s",
              (int)ctype->media.type.slen,
              ctype->media.type.ptr,
              (int)ctype->media.subtype.slen,
              ctype->media.subtype.ptr);
    }


	body.ptr = (char *) mwi_info->rdata->msg_info.msg->body->data;
	body.slen = mwi_info->rdata->msg_info.msg->body->len;

	// Ignore empty messages
	if (body.slen == 0){
		return;
	}

	mime_type = pj_str(mime_type_c);

	registeredCallbackObject->on_mwi_info(acc_id, &mime_type, &body);
}

pj_status_t on_validate_audio_clock_rate_wrapper (int clock_rate) {
	return registeredCallbackObject->on_validate_audio_clock_rate(clock_rate);
}

void on_setup_audio_wrapper (pj_bool_t before_init) {
	registeredCallbackObject->on_setup_audio(before_init);
}

void on_teardown_audio_wrapper () {
	registeredCallbackObject->on_teardown_audio();
}

int on_set_micro_source_wrapper () {
	return registeredCallbackObject->on_set_micro_source();
}

int timer_schedule_wrapper(int entry, int entryId, int time) {
	return registeredCallbackObject->timer_schedule(entry, entryId, time);
}

int timer_cancel_wrapper(int entry, int entryId) {
	return registeredCallbackObject->timer_cancel(entry, entryId);
}

struct pjsua_callback wrapper_callback_struct = {
	&on_call_state_wrapper,
	&on_incoming_call_wrapper,
	&on_call_tsx_state_wrapper,
	&on_call_media_state_wrapper,
	&on_call_sdp_created_wrapper,
	&on_stream_created_wrapper,
	&on_stream_destroyed_wrapper,
	&on_dtmf_digit_wrapper,
	&on_call_transfer_request_wrapper,
	NULL, //on_call_transfer_request2
	&on_call_transfer_status_wrapper,
	&on_call_replace_request_wrapper,
	NULL, //on_call_replace_request2
	&on_call_replaced_wrapper,
	NULL, // on_call_rx_offer
	NULL, // on_reg_started
	&on_reg_state_wrapper,
	NULL, //on_reg2_state
	NULL, // incoming subscribe &on_incoming_subscribe_wrapper,
	NULL, // srv_subscribe state &on_srv_subscribe_state_wrapper,
	&on_buddy_state_wrapper,
	NULL, // on_buddy_evsub_state
	&on_pager_wrapper,
	&on_pager2_wrapper,
	&on_pager_status_wrapper,
	&on_pager_status2_wrapper,
	&on_typing_wrapper,
	NULL, //Typing 2
	&on_nat_detect_wrapper,
	&on_call_redirected_wrapper,
	NULL, //on_mwi_state
	&on_mwi_info_wrapper,
	NULL, //on_call_media_transport_state
	NULL, //on_transport_state
	NULL, //on_ice_transport_error
	NULL, //on_snd_dev_operation
	NULL, //on_call_media_event
	NULL //on_create_media_transport
};

void setCallbackObject(Callback* callback) {
	registeredCallbackObject = callback;
}

}
