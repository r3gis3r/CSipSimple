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

public class pjsua_media_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_media_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_media_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_media_config(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setClock_rate(long value) {
    pjsuaJNI.pjsua_media_config_clock_rate_set(swigCPtr, this, value);
  }

  public long getClock_rate() {
    return pjsuaJNI.pjsua_media_config_clock_rate_get(swigCPtr, this);
  }

  public void setSnd_clock_rate(long value) {
    pjsuaJNI.pjsua_media_config_snd_clock_rate_set(swigCPtr, this, value);
  }

  public long getSnd_clock_rate() {
    return pjsuaJNI.pjsua_media_config_snd_clock_rate_get(swigCPtr, this);
  }

  public void setChannel_count(long value) {
    pjsuaJNI.pjsua_media_config_channel_count_set(swigCPtr, this, value);
  }

  public long getChannel_count() {
    return pjsuaJNI.pjsua_media_config_channel_count_get(swigCPtr, this);
  }

  public void setAudio_frame_ptime(long value) {
    pjsuaJNI.pjsua_media_config_audio_frame_ptime_set(swigCPtr, this, value);
  }

  public long getAudio_frame_ptime() {
    return pjsuaJNI.pjsua_media_config_audio_frame_ptime_get(swigCPtr, this);
  }

  public void setMax_media_ports(long value) {
    pjsuaJNI.pjsua_media_config_max_media_ports_set(swigCPtr, this, value);
  }

  public long getMax_media_ports() {
    return pjsuaJNI.pjsua_media_config_max_media_ports_get(swigCPtr, this);
  }

  public void setHas_ioqueue(int value) {
    pjsuaJNI.pjsua_media_config_has_ioqueue_set(swigCPtr, this, value);
  }

  public int getHas_ioqueue() {
    return pjsuaJNI.pjsua_media_config_has_ioqueue_get(swigCPtr, this);
  }

  public void setThread_cnt(long value) {
    pjsuaJNI.pjsua_media_config_thread_cnt_set(swigCPtr, this, value);
  }

  public long getThread_cnt() {
    return pjsuaJNI.pjsua_media_config_thread_cnt_get(swigCPtr, this);
  }

  public void setQuality(long value) {
    pjsuaJNI.pjsua_media_config_quality_set(swigCPtr, this, value);
  }

  public long getQuality() {
    return pjsuaJNI.pjsua_media_config_quality_get(swigCPtr, this);
  }

  public void setPtime(long value) {
    pjsuaJNI.pjsua_media_config_ptime_set(swigCPtr, this, value);
  }

  public long getPtime() {
    return pjsuaJNI.pjsua_media_config_ptime_get(swigCPtr, this);
  }

  public void setNo_vad(int value) {
    pjsuaJNI.pjsua_media_config_no_vad_set(swigCPtr, this, value);
  }

  public int getNo_vad() {
    return pjsuaJNI.pjsua_media_config_no_vad_get(swigCPtr, this);
  }

  public void setIlbc_mode(long value) {
    pjsuaJNI.pjsua_media_config_ilbc_mode_set(swigCPtr, this, value);
  }

  public long getIlbc_mode() {
    return pjsuaJNI.pjsua_media_config_ilbc_mode_get(swigCPtr, this);
  }

  public void setTx_drop_pct(long value) {
    pjsuaJNI.pjsua_media_config_tx_drop_pct_set(swigCPtr, this, value);
  }

  public long getTx_drop_pct() {
    return pjsuaJNI.pjsua_media_config_tx_drop_pct_get(swigCPtr, this);
  }

  public void setRx_drop_pct(long value) {
    pjsuaJNI.pjsua_media_config_rx_drop_pct_set(swigCPtr, this, value);
  }

  public long getRx_drop_pct() {
    return pjsuaJNI.pjsua_media_config_rx_drop_pct_get(swigCPtr, this);
  }

  public void setEc_options(long value) {
    pjsuaJNI.pjsua_media_config_ec_options_set(swigCPtr, this, value);
  }

  public long getEc_options() {
    return pjsuaJNI.pjsua_media_config_ec_options_get(swigCPtr, this);
  }

  public void setEc_tail_len(long value) {
    pjsuaJNI.pjsua_media_config_ec_tail_len_set(swigCPtr, this, value);
  }

  public long getEc_tail_len() {
    return pjsuaJNI.pjsua_media_config_ec_tail_len_get(swigCPtr, this);
  }

  public void setJb_init(int value) {
    pjsuaJNI.pjsua_media_config_jb_init_set(swigCPtr, this, value);
  }

  public int getJb_init() {
    return pjsuaJNI.pjsua_media_config_jb_init_get(swigCPtr, this);
  }

  public void setJb_min_pre(int value) {
    pjsuaJNI.pjsua_media_config_jb_min_pre_set(swigCPtr, this, value);
  }

  public int getJb_min_pre() {
    return pjsuaJNI.pjsua_media_config_jb_min_pre_get(swigCPtr, this);
  }

  public void setJb_max_pre(int value) {
    pjsuaJNI.pjsua_media_config_jb_max_pre_set(swigCPtr, this, value);
  }

  public int getJb_max_pre() {
    return pjsuaJNI.pjsua_media_config_jb_max_pre_get(swigCPtr, this);
  }

  public void setJb_max(int value) {
    pjsuaJNI.pjsua_media_config_jb_max_set(swigCPtr, this, value);
  }

  public int getJb_max() {
    return pjsuaJNI.pjsua_media_config_jb_max_get(swigCPtr, this);
  }

  public void setEnable_ice(int value) {
    pjsuaJNI.pjsua_media_config_enable_ice_set(swigCPtr, this, value);
  }

  public int getEnable_ice() {
    return pjsuaJNI.pjsua_media_config_enable_ice_get(swigCPtr, this);
  }

  public void setIce_no_host_cands(int value) {
    pjsuaJNI.pjsua_media_config_ice_no_host_cands_set(swigCPtr, this, value);
  }

  public int getIce_no_host_cands() {
    return pjsuaJNI.pjsua_media_config_ice_no_host_cands_get(swigCPtr, this);
  }

  public void setEnable_turn(int value) {
    pjsuaJNI.pjsua_media_config_enable_turn_set(swigCPtr, this, value);
  }

  public int getEnable_turn() {
    return pjsuaJNI.pjsua_media_config_enable_turn_get(swigCPtr, this);
  }

  public void setTurn_server(pj_str_t value) {
    pjsuaJNI.pjsua_media_config_turn_server_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getTurn_server() {
    long cPtr = pjsuaJNI.pjsua_media_config_turn_server_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setTurn_conn_type(SWIGTYPE_p_pj_turn_tp_type value) {
    pjsuaJNI.pjsua_media_config_turn_conn_type_set(swigCPtr, this, SWIGTYPE_p_pj_turn_tp_type.getCPtr(value));
  }

  public SWIGTYPE_p_pj_turn_tp_type getTurn_conn_type() {
    return new SWIGTYPE_p_pj_turn_tp_type(pjsuaJNI.pjsua_media_config_turn_conn_type_get(swigCPtr, this), true);
  }

  public void setTurn_auth_cred(SWIGTYPE_p_pj_stun_auth_cred value) {
    pjsuaJNI.pjsua_media_config_turn_auth_cred_set(swigCPtr, this, SWIGTYPE_p_pj_stun_auth_cred.getCPtr(value));
  }

  public SWIGTYPE_p_pj_stun_auth_cred getTurn_auth_cred() {
    return new SWIGTYPE_p_pj_stun_auth_cred(pjsuaJNI.pjsua_media_config_turn_auth_cred_get(swigCPtr, this), true);
  }

  public void setSnd_auto_close_time(int value) {
    pjsuaJNI.pjsua_media_config_snd_auto_close_time_set(swigCPtr, this, value);
  }

  public int getSnd_auto_close_time() {
    return pjsuaJNI.pjsua_media_config_snd_auto_close_time_get(swigCPtr, this);
  }

  public pjsua_media_config() {
    this(pjsuaJNI.new_pjsua_media_config(), true);
  }

}
