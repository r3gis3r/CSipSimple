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

public class pjsua_call_info_buf_ {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_call_info_buf_(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_call_info_buf_ obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_call_info_buf_(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setLocal_info(String value) {
    pjsuaJNI.pjsua_call_info_buf__local_info_set(swigCPtr, this, value);
  }

  public String getLocal_info() {
    return pjsuaJNI.pjsua_call_info_buf__local_info_get(swigCPtr, this);
  }

  public void setLocal_contact(String value) {
    pjsuaJNI.pjsua_call_info_buf__local_contact_set(swigCPtr, this, value);
  }

  public String getLocal_contact() {
    return pjsuaJNI.pjsua_call_info_buf__local_contact_get(swigCPtr, this);
  }

  public void setRemote_info(String value) {
    pjsuaJNI.pjsua_call_info_buf__remote_info_set(swigCPtr, this, value);
  }

  public String getRemote_info() {
    return pjsuaJNI.pjsua_call_info_buf__remote_info_get(swigCPtr, this);
  }

  public void setRemote_contact(String value) {
    pjsuaJNI.pjsua_call_info_buf__remote_contact_set(swigCPtr, this, value);
  }

  public String getRemote_contact() {
    return pjsuaJNI.pjsua_call_info_buf__remote_contact_get(swigCPtr, this);
  }

  public void setCall_id(String value) {
    pjsuaJNI.pjsua_call_info_buf__call_id_set(swigCPtr, this, value);
  }

  public String getCall_id() {
    return pjsuaJNI.pjsua_call_info_buf__call_id_get(swigCPtr, this);
  }

  public void setLast_status_text(String value) {
    pjsuaJNI.pjsua_call_info_buf__last_status_text_set(swigCPtr, this, value);
  }

  public String getLast_status_text() {
    return pjsuaJNI.pjsua_call_info_buf__last_status_text_get(swigCPtr, this);
  }

  public pjsua_call_info_buf_() {
    this(pjsuaJNI.new_pjsua_call_info_buf_(), true);
  }

}
