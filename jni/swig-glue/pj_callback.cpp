#include "pj_callback.h"

static Callback* registeredCallbackObject = NULL;

#define THIS_FILE "pj_callback.cpp"

extern "C" {

#include <pj/config_site.h>
#include <pjsua-lib/pjsua_internal.h>

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


#define MAX_COMPARE_LEN 64

static unsigned max_common_substr_len(const pj_str_t* str1, const pj_str_t* str2)
{
    unsigned max_len = 0;
    /* We compare only on first MAX_COMPARE_LEN char */
    unsigned tree[MAX_COMPARE_LEN][MAX_COMPARE_LEN];
    unsigned m1=0, m2=0;
    int i=0, j=0;

    if(str1->slen == 0 || str2->slen == 0)
    {
        return 0;
    }

    /* Init tree */
    for(i=0;i < MAX_COMPARE_LEN;i++) {
        pj_bzero(tree[i], PJ_ARRAY_SIZE( tree[i] ));
    }

    m1 = PJ_MIN(str1->slen, MAX_COMPARE_LEN);
    m2 = PJ_MIN(str2->slen, MAX_COMPARE_LEN);

    for (i = 0; i < m1; i++) {
        for (j = 0; j < m2; j++) {
            if (str1->ptr[i] != str2->ptr[j])
            {
                tree[i][j] = 0;
            }
            else
            {
                if ((i == 0) || (j == 0))
                {
                    tree[i][j] = 1;
                }
                else
                {
                    tree[i][j] = 1 + tree[i - 1][j - 1];
                }

                if (tree[i][j] > max_len)
                {
                    max_len = tree[i][j];
                }
            }
        }
    }
    return max_len;
}

/*
 * This is an internal function to find the most appropriate account to be
 * used to handle incoming calls.
 */
void on_acc_find_for_incoming_wrapper(const pjsip_rx_data *rdata, pjsua_acc_id* out_acc_id)
{
    pjsip_uri *uri;
    pjsip_sip_uri *sip_uri;
    unsigned i;
    int current_matching_score = 0;
    int matching_scores[PJSUA_MAX_ACC];
    pjsua_acc_id best_matching = pjsua_var.default_acc;

    /* Check that there's at least one account configured */
    PJ_ASSERT_RETURN(pjsua_var.acc_cnt!=0, pjsua_var.default_acc);

    uri = rdata->msg_info.to->uri;

    /* Just return if To URI is not SIP: */
    if (!PJSIP_URI_SCHEME_IS_SIP(uri) &&
    !PJSIP_URI_SCHEME_IS_SIPS(uri))
    {
    return;
    }


    PJSUA_LOCK();

    sip_uri = (pjsip_sip_uri*)pjsip_uri_get_uri(uri);

    /* Find account which has matching username and domain. */
    for (i=0; i < pjsua_var.acc_cnt; ++i) {
    unsigned acc_id = pjsua_var.acc_ids[i];
    pjsua_acc *acc = &pjsua_var.acc[acc_id];

    if (acc->valid && pj_stricmp(&acc->user_part, &sip_uri->user)==0 &&
        pj_stricmp(&acc->srv_domain, &sip_uri->host)==0)
    {
        /* Match ! */
        PJSUA_UNLOCK();
        *out_acc_id = acc_id;
        return;
    }
    }

    /* No exact matching, try fuzzy matching */
    pj_bzero(matching_scores, sizeof(matching_scores));

    /* No matching account, try match domain part only. */
    for (i=0; i < pjsua_var.acc_cnt; ++i) {
    unsigned acc_id = pjsua_var.acc_ids[i];
    pjsua_acc *acc = &pjsua_var.acc[acc_id];

    if (acc->valid && pj_stricmp(&acc->srv_domain, &sip_uri->host)==0) {
        /* Match ! */
        /* We apply 100 weight if account has reg uri
         * Because in pragmatic case we are more looking
         * for these one than for the local acc
         */
        matching_scores[i] += (acc->cfg.reg_uri.slen > 0) ? (300 * sip_uri->host.slen) : 1;
    }
    }

    /* No matching account, try match user part (and transport type) only. */
    for (i=0; i < pjsua_var.acc_cnt; ++i) {
    unsigned acc_id = pjsua_var.acc_ids[i];
    pjsua_acc *acc = &pjsua_var.acc[acc_id];

    if (acc->valid) {
        /* We apply 100 weight if account has reg uri
         * Because in pragmatic case we are more looking
         * for these one than for the local acc
         */
        unsigned weight = (acc->cfg.reg_uri.slen > 0) ? 100 : 1;

        if (acc->cfg.transport_id != PJSUA_INVALID_ID) {
        pjsip_transport_type_e type;
        type = pjsip_transport_get_type_from_name(&sip_uri->transport_param);
        if (type == PJSIP_TRANSPORT_UNSPECIFIED)
            type = PJSIP_TRANSPORT_UDP;

        if (pjsua_var.tpdata[acc->cfg.transport_id].type != type)
            continue;
        }
        /* Match ! */
        matching_scores[i] += (max_common_substr_len(&acc->user_part, &sip_uri->user) * weight);
    }
    }

    /* Still no match, use default account */
    PJSUA_UNLOCK();
    for(i=0; i<pjsua_var.acc_cnt; i++) {
        if(current_matching_score < matching_scores[i])
        {
            best_matching = pjsua_var.acc_ids[i];
            current_matching_score = matching_scores[i];
        }
    }
    *out_acc_id = best_matching;
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
	NULL, //on_create_media_transport
	&on_acc_find_for_incoming_wrapper
};

void setCallbackObject(Callback* callback) {
	registeredCallbackObject = callback;
}

}
