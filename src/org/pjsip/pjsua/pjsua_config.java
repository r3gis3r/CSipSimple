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

public class pjsua_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_config(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setMax_calls(long value) {
    pjsuaJNI.pjsua_config_max_calls_set(swigCPtr, this, value);
  }

  public long getMax_calls() {
    return pjsuaJNI.pjsua_config_max_calls_get(swigCPtr, this);
  }

  public void setThread_cnt(long value) {
    pjsuaJNI.pjsua_config_thread_cnt_set(swigCPtr, this, value);
  }

  public long getThread_cnt() {
    return pjsuaJNI.pjsua_config_thread_cnt_get(swigCPtr, this);
  }

  public void setNameserver_count(long value) {
    pjsuaJNI.pjsua_config_nameserver_count_set(swigCPtr, this, value);
  }

  public long getNameserver_count() {
    return pjsuaJNI.pjsua_config_nameserver_count_get(swigCPtr, this);
  }

  public void setNameserver(pj_str_t value) {
    pjsuaJNI.pjsua_config_nameserver_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getNameserver() {
    long cPtr = pjsuaJNI.pjsua_config_nameserver_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setOutbound_proxy_cnt(long value) {
    pjsuaJNI.pjsua_config_outbound_proxy_cnt_set(swigCPtr, this, value);
  }

  public long getOutbound_proxy_cnt() {
    return pjsuaJNI.pjsua_config_outbound_proxy_cnt_get(swigCPtr, this);
  }

  public void setOutbound_proxy(pj_str_t value) {
    pjsuaJNI.pjsua_config_outbound_proxy_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getOutbound_proxy() {
    long cPtr = pjsuaJNI.pjsua_config_outbound_proxy_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setStun_domain(pj_str_t value) {
    pjsuaJNI.pjsua_config_stun_domain_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getStun_domain() {
    long cPtr = pjsuaJNI.pjsua_config_stun_domain_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setStun_host(pj_str_t value) {
    pjsuaJNI.pjsua_config_stun_host_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getStun_host() {
    long cPtr = pjsuaJNI.pjsua_config_stun_host_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setNat_type_in_sdp(int value) {
    pjsuaJNI.pjsua_config_nat_type_in_sdp_set(swigCPtr, this, value);
  }

  public int getNat_type_in_sdp() {
    return pjsuaJNI.pjsua_config_nat_type_in_sdp_get(swigCPtr, this);
  }

  public void setRequire_100rel(int value) {
    pjsuaJNI.pjsua_config_require_100rel_set(swigCPtr, this, value);
  }

  public int getRequire_100rel() {
    return pjsuaJNI.pjsua_config_require_100rel_get(swigCPtr, this);
  }

  public void setCred_count(long value) {
    pjsuaJNI.pjsua_config_cred_count_set(swigCPtr, this, value);
  }

  public long getCred_count() {
    return pjsuaJNI.pjsua_config_cred_count_get(swigCPtr, this);
  }

  public void setCred_info(pjsip_cred_info value) {
    pjsuaJNI.pjsua_config_cred_info_set(swigCPtr, this, pjsip_cred_info.getCPtr(value), value);
  }

  public pjsip_cred_info getCred_info() {
    long cPtr = pjsuaJNI.pjsua_config_cred_info_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_cred_info(cPtr, false);
  }

  public void setCb(pjsua_callback value) {
    pjsuaJNI.pjsua_config_cb_set(swigCPtr, this, pjsua_callback.getCPtr(value), value);
  }

  public pjsua_callback getCb() {
    long cPtr = pjsuaJNI.pjsua_config_cb_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsua_callback(cPtr, false);
  }

  public void setUser_agent(pj_str_t value) {
    pjsuaJNI.pjsua_config_user_agent_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getUser_agent() {
    long cPtr = pjsuaJNI.pjsua_config_user_agent_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public pjsua_config() {
    this(pjsuaJNI.new_pjsua_config(), true);
  }

}
