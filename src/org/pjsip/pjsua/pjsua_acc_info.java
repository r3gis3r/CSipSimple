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

public class pjsua_acc_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_acc_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_acc_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_acc_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setId(int value) {
    pjsuaJNI.pjsua_acc_info_id_set(swigCPtr, this, value);
  }

  public int getId() {
    return pjsuaJNI.pjsua_acc_info_id_get(swigCPtr, this);
  }

  public void setIs_default(int value) {
    pjsuaJNI.pjsua_acc_info_is_default_set(swigCPtr, this, value);
  }

  public int getIs_default() {
    return pjsuaJNI.pjsua_acc_info_is_default_get(swigCPtr, this);
  }

  public void setAcc_uri(pj_str_t value) {
    pjsuaJNI.pjsua_acc_info_acc_uri_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getAcc_uri() {
    long cPtr = pjsuaJNI.pjsua_acc_info_acc_uri_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setHas_registration(int value) {
    pjsuaJNI.pjsua_acc_info_has_registration_set(swigCPtr, this, value);
  }

  public int getHas_registration() {
    return pjsuaJNI.pjsua_acc_info_has_registration_get(swigCPtr, this);
  }

  public void setExpires(int value) {
    pjsuaJNI.pjsua_acc_info_expires_set(swigCPtr, this, value);
  }

  public int getExpires() {
    return pjsuaJNI.pjsua_acc_info_expires_get(swigCPtr, this);
  }

  public void setStatus(pjsip_status_code value) {
    pjsuaJNI.pjsua_acc_info_status_set(swigCPtr, this, value.swigValue());
  }

  public pjsip_status_code getStatus() {
    return pjsip_status_code.swigToEnum(pjsuaJNI.pjsua_acc_info_status_get(swigCPtr, this));
  }

  public void setStatus_text(pj_str_t value) {
    pjsuaJNI.pjsua_acc_info_status_text_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getStatus_text() {
    long cPtr = pjsuaJNI.pjsua_acc_info_status_text_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setOnline_status(int value) {
    pjsuaJNI.pjsua_acc_info_online_status_set(swigCPtr, this, value);
  }

  public int getOnline_status() {
    return pjsuaJNI.pjsua_acc_info_online_status_get(swigCPtr, this);
  }

  public void setOnline_status_text(pj_str_t value) {
    pjsuaJNI.pjsua_acc_info_online_status_text_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getOnline_status_text() {
    long cPtr = pjsuaJNI.pjsua_acc_info_online_status_text_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setRpid(SWIGTYPE_p_pjrpid_element value) {
    pjsuaJNI.pjsua_acc_info_rpid_set(swigCPtr, this, SWIGTYPE_p_pjrpid_element.getCPtr(value));
  }

  public SWIGTYPE_p_pjrpid_element getRpid() {
    return new SWIGTYPE_p_pjrpid_element(pjsuaJNI.pjsua_acc_info_rpid_get(swigCPtr, this), true);
  }

  public void setBuf_(String value) {
    pjsuaJNI.pjsua_acc_info_buf__set(swigCPtr, this, value);
  }

  public String getBuf_() {
    return pjsuaJNI.pjsua_acc_info_buf__get(swigCPtr, this);
  }

  public pjsua_acc_info() {
    this(pjsuaJNI.new_pjsua_acc_info(), true);
  }

}
