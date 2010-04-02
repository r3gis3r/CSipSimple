/**
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pjsip.pjsua;

public class pjsua_callback {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_callback(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_callback obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_callback(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setOn_call_state(SWIGTYPE_p_f_int_p_pjsip_event__void value) {
    pjsuaJNI.pjsua_callback_on_call_state_set(swigCPtr, this, SWIGTYPE_p_f_int_p_pjsip_event__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_pjsip_event__void getOn_call_state() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_pjsip_event__void(cPtr, false);
  }

  public void setOn_incoming_call(SWIGTYPE_p_f_int_int_p_pjsip_rx_data__void value) {
    pjsuaJNI.pjsua_callback_on_incoming_call_set(swigCPtr, this, SWIGTYPE_p_f_int_int_p_pjsip_rx_data__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_int_p_pjsip_rx_data__void getOn_incoming_call() {
    long cPtr = pjsuaJNI.pjsua_callback_on_incoming_call_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_int_p_pjsip_rx_data__void(cPtr, false);
  }

  public void setOn_call_tsx_state(SWIGTYPE_p_f_int_p_pjsip_transaction_p_pjsip_event__void value) {
    pjsuaJNI.pjsua_callback_on_call_tsx_state_set(swigCPtr, this, SWIGTYPE_p_f_int_p_pjsip_transaction_p_pjsip_event__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_pjsip_transaction_p_pjsip_event__void getOn_call_tsx_state() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_tsx_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_pjsip_transaction_p_pjsip_event__void(cPtr, false);
  }

  public void setOn_call_media_state(SWIGTYPE_p_f_int__void value) {
    pjsuaJNI.pjsua_callback_on_call_media_state_set(swigCPtr, this, SWIGTYPE_p_f_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int__void getOn_call_media_state() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_media_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int__void(cPtr, false);
  }

  public void setOn_stream_created(SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int_p_p_pjmedia_port__void value) {
    pjsuaJNI.pjsua_callback_on_stream_created_set(swigCPtr, this, SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int_p_p_pjmedia_port__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int_p_p_pjmedia_port__void getOn_stream_created() {
    long cPtr = pjsuaJNI.pjsua_callback_on_stream_created_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int_p_p_pjmedia_port__void(cPtr, false);
  }

  public void setOn_stream_destroyed(SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int__void value) {
    pjsuaJNI.pjsua_callback_on_stream_destroyed_set(swigCPtr, this, SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int__void getOn_stream_destroyed() {
    long cPtr = pjsuaJNI.pjsua_callback_on_stream_destroyed_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_pjmedia_session_unsigned_int__void(cPtr, false);
  }

  public void setOn_dtmf_digit(SWIGTYPE_p_f_int_int__void value) {
    pjsuaJNI.pjsua_callback_on_dtmf_digit_set(swigCPtr, this, SWIGTYPE_p_f_int_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_int__void getOn_dtmf_digit() {
    long cPtr = pjsuaJNI.pjsua_callback_on_dtmf_digit_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_int__void(cPtr, false);
  }

  public void setOn_call_transfer_request(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_enum_pjsip_status_code__void value) {
    pjsuaJNI.pjsua_callback_on_call_transfer_request_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_enum_pjsip_status_code__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_enum_pjsip_status_code__void getOn_call_transfer_request() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_transfer_request_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_enum_pjsip_status_code__void(cPtr, false);
  }

  public void setOn_call_transfer_status(SWIGTYPE_p_f_int_int_p_q_const__pj_str_t_int_p_int__void value) {
    pjsuaJNI.pjsua_callback_on_call_transfer_status_set(swigCPtr, this, SWIGTYPE_p_f_int_int_p_q_const__pj_str_t_int_p_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_int_p_q_const__pj_str_t_int_p_int__void getOn_call_transfer_status() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_transfer_status_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_int_p_q_const__pj_str_t_int_p_int__void(cPtr, false);
  }

  public void setOn_call_replace_request(SWIGTYPE_p_f_int_p_pjsip_rx_data_p_int_p_pj_str_t__void value) {
    pjsuaJNI.pjsua_callback_on_call_replace_request_set(swigCPtr, this, SWIGTYPE_p_f_int_p_pjsip_rx_data_p_int_p_pj_str_t__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_pjsip_rx_data_p_int_p_pj_str_t__void getOn_call_replace_request() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_replace_request_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_pjsip_rx_data_p_int_p_pj_str_t__void(cPtr, false);
  }

  public void setOn_call_replaced(SWIGTYPE_p_f_int_int__void value) {
    pjsuaJNI.pjsua_callback_on_call_replaced_set(swigCPtr, this, SWIGTYPE_p_f_int_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_int__void getOn_call_replaced() {
    long cPtr = pjsuaJNI.pjsua_callback_on_call_replaced_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_int__void(cPtr, false);
  }

  public void setOn_reg_state(SWIGTYPE_p_f_int__void value) {
    pjsuaJNI.pjsua_callback_on_reg_state_set(swigCPtr, this, SWIGTYPE_p_f_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int__void getOn_reg_state() {
    long cPtr = pjsuaJNI.pjsua_callback_on_reg_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int__void(cPtr, false);
  }

  public void setOn_buddy_state(SWIGTYPE_p_f_int__void value) {
    pjsuaJNI.pjsua_callback_on_buddy_state_set(swigCPtr, this, SWIGTYPE_p_f_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int__void getOn_buddy_state() {
    long cPtr = pjsuaJNI.pjsua_callback_on_buddy_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int__void(cPtr, false);
  }

  public void setOn_pager(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t__void value) {
    pjsuaJNI.pjsua_callback_on_pager_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t__void getOn_pager() {
    long cPtr = pjsuaJNI.pjsua_callback_on_pager_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t__void(cPtr, false);
  }

  public void setOn_pager2(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_pjsip_rx_data__void value) {
    pjsuaJNI.pjsua_callback_on_pager2_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_pjsip_rx_data__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_pjsip_rx_data__void getOn_pager2() {
    long cPtr = pjsuaJNI.pjsua_callback_on_pager2_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_p_pjsip_rx_data__void(cPtr, false);
  }

  public void setOn_pager_status(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t__void value) {
    pjsuaJNI.pjsua_callback_on_pager_status_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t__void getOn_pager_status() {
    long cPtr = pjsuaJNI.pjsua_callback_on_pager_status_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t__void(cPtr, false);
  }

  public void setOn_pager_status2(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t_p_pjsip_tx_data_p_pjsip_rx_data__void value) {
    pjsuaJNI.pjsua_callback_on_pager_status2_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t_p_pjsip_tx_data_p_pjsip_rx_data__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t_p_pjsip_tx_data_p_pjsip_rx_data__void getOn_pager_status2() {
    long cPtr = pjsuaJNI.pjsua_callback_on_pager_status2_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_void_enum_pjsip_status_code_p_q_const__pj_str_t_p_pjsip_tx_data_p_pjsip_rx_data__void(cPtr, false);
  }

  public void setOn_typing(SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_int__void value) {
    pjsuaJNI.pjsua_callback_on_typing_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_int__void getOn_typing() {
    long cPtr = pjsuaJNI.pjsua_callback_on_typing_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__pj_str_t_p_q_const__pj_str_t_p_q_const__pj_str_t_int__void(cPtr, false);
  }

  public void setOn_nat_detect(SWIGTYPE_p_f_p_q_const__pj_stun_nat_detect_result__void value) {
    pjsuaJNI.pjsua_callback_on_nat_detect_set(swigCPtr, this, SWIGTYPE_p_f_p_q_const__pj_stun_nat_detect_result__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_p_q_const__pj_stun_nat_detect_result__void getOn_nat_detect() {
    long cPtr = pjsuaJNI.pjsua_callback_on_nat_detect_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_p_q_const__pj_stun_nat_detect_result__void(cPtr, false);
  }

  public pjsua_callback() {
    this(pjsuaJNI.new_pjsua_callback(), true);
  }

}
