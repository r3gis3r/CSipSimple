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

public class pjsua_buddy_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_buddy_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_buddy_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_buddy_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setId(int value) {
    pjsuaJNI.pjsua_buddy_info_id_set(swigCPtr, this, value);
  }

  public int getId() {
    return pjsuaJNI.pjsua_buddy_info_id_get(swigCPtr, this);
  }

  public void setUri(pj_str_t value) {
    pjsuaJNI.pjsua_buddy_info_uri_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getUri() {
    long cPtr = pjsuaJNI.pjsua_buddy_info_uri_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setContact(pj_str_t value) {
    pjsuaJNI.pjsua_buddy_info_contact_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getContact() {
    long cPtr = pjsuaJNI.pjsua_buddy_info_contact_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setStatus(pjsua_buddy_status value) {
    pjsuaJNI.pjsua_buddy_info_status_set(swigCPtr, this, value.swigValue());
  }

  public pjsua_buddy_status getStatus() {
    return pjsua_buddy_status.swigToEnum(pjsuaJNI.pjsua_buddy_info_status_get(swigCPtr, this));
  }

  public void setStatus_text(pj_str_t value) {
    pjsuaJNI.pjsua_buddy_info_status_text_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getStatus_text() {
    long cPtr = pjsuaJNI.pjsua_buddy_info_status_text_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setMonitor_pres(int value) {
    pjsuaJNI.pjsua_buddy_info_monitor_pres_set(swigCPtr, this, value);
  }

  public int getMonitor_pres() {
    return pjsuaJNI.pjsua_buddy_info_monitor_pres_get(swigCPtr, this);
  }

  public void setRpid(SWIGTYPE_p_pjrpid_element value) {
    pjsuaJNI.pjsua_buddy_info_rpid_set(swigCPtr, this, SWIGTYPE_p_pjrpid_element.getCPtr(value));
  }

  public SWIGTYPE_p_pjrpid_element getRpid() {
    return new SWIGTYPE_p_pjrpid_element(pjsuaJNI.pjsua_buddy_info_rpid_get(swigCPtr, this), true);
  }

  public void setBuf_(String value) {
    pjsuaJNI.pjsua_buddy_info_buf__set(swigCPtr, this, value);
  }

  public String getBuf_() {
    return pjsuaJNI.pjsua_buddy_info_buf__get(swigCPtr, this);
  }

  public pjsua_buddy_info() {
    this(pjsuaJNI.new_pjsua_buddy_info(), true);
  }

}
