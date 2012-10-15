
#include <pjsua-lib/pjsua.h>

class Callback {
public:
	virtual ~Callback() {}
	virtual void on_call_state (pjsua_call_id call_id, pjsip_event *e) {}
	virtual void on_incoming_call (pjsua_acc_id acc_id, pjsua_call_id call_id,
		pjsip_rx_data *rdata) {}
	virtual void on_call_tsx_state (pjsua_call_id call_id,
		pjsip_transaction *tsx,
		pjsip_event *e) {}
	virtual void on_call_media_state (pjsua_call_id call_id) {}
	virtual void on_call_sdp_created (pjsua_call_id call_id,
	                                pjmedia_sdp_session *sdp,
	                                pj_pool_t *pool,
	                                const pjmedia_sdp_session *rem_sdp) {}
	virtual void on_stream_created (pjsua_call_id call_id,
		pjmedia_stream *strm,
		unsigned stream_idx,
		pjmedia_port **p_port) {}
	virtual void on_stream_destroyed (pjsua_call_id call_id,
		pjmedia_stream *strm,
		unsigned stream_idx) {}
	virtual void on_dtmf_digit (pjsua_call_id call_id, int digit) {}
	virtual void on_call_transfer_request (pjsua_call_id call_id,
		const pj_str_t *dst,
		pjsip_status_code *code) {}
	virtual void on_call_transfer_status (pjsua_call_id call_id,
		int st_code,
		const pj_str_t *st_text,
		pj_bool_t final_,
		pj_bool_t *p_cont) {}
	virtual void on_call_replace_request (pjsua_call_id call_id,
		pjsip_rx_data *rdata,
		int *st_code,
		pj_str_t *st_text) {}
	virtual void on_call_replaced (pjsua_call_id old_call_id,
		pjsua_call_id new_call_id) {}
	virtual void on_reg_state (pjsua_acc_id acc_id) {}
	virtual void on_buddy_state (pjsua_buddy_id buddy_id) {}
	virtual void on_pager (pjsua_call_id call_id, const pj_str_t *from,
		const pj_str_t *to, const pj_str_t *contact,
		const pj_str_t *mime_type, const pj_str_t *body) {}
	virtual void on_pager2 (pjsua_call_id call_id, const pj_str_t *from,
		const pj_str_t *to, const pj_str_t *contact,
		const pj_str_t *mime_type, const pj_str_t *body,
		pjsip_rx_data *rdata) {}
	virtual void on_pager_status (pjsua_call_id call_id,
		const pj_str_t *to,
		const pj_str_t *body,
/*XXX		void *user_data,*/
		pjsip_status_code status,
		const pj_str_t *reason) {}
	virtual void on_pager_status2 (pjsua_call_id call_id,
		const pj_str_t *to,
		const pj_str_t *body,
/*XXX		void *user_data,*/
		pjsip_status_code status,
		const pj_str_t *reason,
		pjsip_tx_data *tdata,
		pjsip_rx_data *rdata) {}
	virtual void on_typing (pjsua_call_id call_id, const pj_str_t *from,
		const pj_str_t *to, const pj_str_t *contact,
		pj_bool_t is_typing) {}
	virtual void on_nat_detect (const pj_stun_nat_detect_result *res) {}
	virtual pjsip_redirect_op on_call_redirected (pjsua_call_id call_id, const pj_str_t *target) {}
	virtual void on_mwi_info (pjsua_acc_id acc_id, const pj_str_t *mime_type, const pj_str_t *body) {}

	virtual pj_status_t on_validate_audio_clock_rate (int clock_rate) {}
	virtual void on_setup_audio (pj_bool_t before_init) {}
	virtual void on_teardown_audio () {}
	virtual int on_set_micro_source () {}

//#if PJMEDIA_HAS_ZRTP
	virtual void on_zrtp_show_sas (int data, const pj_str_t *sas, int verified) {}
	virtual void on_zrtp_update_transport (int data) {}
//#endif

//#if USE_CSIPSIMPLE
	virtual int timer_schedule(int entry, int entryId, int time) {}
	virtual int timer_cancel(int entry, int entryId) {}
//#endif

};
extern "C" {
void setCallbackObject(Callback* callback);
}
