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

public class pjsua_msg_data {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_msg_data(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_msg_data obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_msg_data(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setHdr_list(SWIGTYPE_p_pjsip_hdr value) {
    pjsuaJNI.pjsua_msg_data_hdr_list_set(swigCPtr, this, SWIGTYPE_p_pjsip_hdr.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_hdr getHdr_list() {
    return new SWIGTYPE_p_pjsip_hdr(pjsuaJNI.pjsua_msg_data_hdr_list_get(swigCPtr, this), true);
  }

  public void setContent_type(pj_str_t value) {
    pjsuaJNI.pjsua_msg_data_content_type_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getContent_type() {
    long cPtr = pjsuaJNI.pjsua_msg_data_content_type_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setMsg_body(pj_str_t value) {
    pjsuaJNI.pjsua_msg_data_msg_body_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getMsg_body() {
    long cPtr = pjsuaJNI.pjsua_msg_data_msg_body_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public pjsua_msg_data() {
    this(pjsuaJNI.new_pjsua_msg_data(), true);
  }

}
