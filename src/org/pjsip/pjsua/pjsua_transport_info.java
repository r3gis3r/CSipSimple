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

public class pjsua_transport_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_transport_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_transport_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_transport_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setId(int value) {
    pjsuaJNI.pjsua_transport_info_id_set(swigCPtr, this, value);
  }

  public int getId() {
    return pjsuaJNI.pjsua_transport_info_id_get(swigCPtr, this);
  }

  public void setType(pjsip_transport_type_e value) {
    pjsuaJNI.pjsua_transport_info_type_set(swigCPtr, this, value.swigValue());
  }

  public pjsip_transport_type_e getType() {
    return pjsip_transport_type_e.swigToEnum(pjsuaJNI.pjsua_transport_info_type_get(swigCPtr, this));
  }

  public void setType_name(pj_str_t value) {
    pjsuaJNI.pjsua_transport_info_type_name_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getType_name() {
    long cPtr = pjsuaJNI.pjsua_transport_info_type_name_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setInfo(pj_str_t value) {
    pjsuaJNI.pjsua_transport_info_info_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getInfo() {
    long cPtr = pjsuaJNI.pjsua_transport_info_info_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setFlag(long value) {
    pjsuaJNI.pjsua_transport_info_flag_set(swigCPtr, this, value);
  }

  public long getFlag() {
    return pjsuaJNI.pjsua_transport_info_flag_get(swigCPtr, this);
  }

  public void setAddr_len(long value) {
    pjsuaJNI.pjsua_transport_info_addr_len_set(swigCPtr, this, value);
  }

  public long getAddr_len() {
    return pjsuaJNI.pjsua_transport_info_addr_len_get(swigCPtr, this);
  }

  public void setLocal_addr(SWIGTYPE_p_pj_sockaddr value) {
    pjsuaJNI.pjsua_transport_info_local_addr_set(swigCPtr, this, SWIGTYPE_p_pj_sockaddr.getCPtr(value));
  }

  public SWIGTYPE_p_pj_sockaddr getLocal_addr() {
    return new SWIGTYPE_p_pj_sockaddr(pjsuaJNI.pjsua_transport_info_local_addr_get(swigCPtr, this), true);
  }

  public void setLocal_name(SWIGTYPE_p_pjsip_host_port value) {
    pjsuaJNI.pjsua_transport_info_local_name_set(swigCPtr, this, SWIGTYPE_p_pjsip_host_port.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_host_port getLocal_name() {
    return new SWIGTYPE_p_pjsip_host_port(pjsuaJNI.pjsua_transport_info_local_name_get(swigCPtr, this), true);
  }

  public void setUsage_count(long value) {
    pjsuaJNI.pjsua_transport_info_usage_count_set(swigCPtr, this, value);
  }

  public long getUsage_count() {
    return pjsuaJNI.pjsua_transport_info_usage_count_get(swigCPtr, this);
  }

  public pjsua_transport_info() {
    this(pjsuaJNI.new_pjsua_transport_info(), true);
  }

}
