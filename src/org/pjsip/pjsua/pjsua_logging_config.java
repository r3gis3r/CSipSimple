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

public class pjsua_logging_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_logging_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_logging_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_logging_config(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setMsg_logging(int value) {
    pjsuaJNI.pjsua_logging_config_msg_logging_set(swigCPtr, this, value);
  }

  public int getMsg_logging() {
    return pjsuaJNI.pjsua_logging_config_msg_logging_get(swigCPtr, this);
  }

  public void setLevel(long value) {
    pjsuaJNI.pjsua_logging_config_level_set(swigCPtr, this, value);
  }

  public long getLevel() {
    return pjsuaJNI.pjsua_logging_config_level_get(swigCPtr, this);
  }

  public void setConsole_level(long value) {
    pjsuaJNI.pjsua_logging_config_console_level_set(swigCPtr, this, value);
  }

  public long getConsole_level() {
    return pjsuaJNI.pjsua_logging_config_console_level_get(swigCPtr, this);
  }

  public void setDecor(long value) {
    pjsuaJNI.pjsua_logging_config_decor_set(swigCPtr, this, value);
  }

  public long getDecor() {
    return pjsuaJNI.pjsua_logging_config_decor_get(swigCPtr, this);
  }

  public void setLog_filename(pj_str_t value) {
    pjsuaJNI.pjsua_logging_config_log_filename_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getLog_filename() {
    long cPtr = pjsuaJNI.pjsua_logging_config_log_filename_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setCb(SWIGTYPE_p_f_int_p_q_const__char_int__void value) {
    pjsuaJNI.pjsua_logging_config_cb_set(swigCPtr, this, SWIGTYPE_p_f_int_p_q_const__char_int__void.getCPtr(value));
  }

  public SWIGTYPE_p_f_int_p_q_const__char_int__void getCb() {
    long cPtr = pjsuaJNI.pjsua_logging_config_cb_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_int_p_q_const__char_int__void(cPtr, false);
  }

  public pjsua_logging_config() {
    this(pjsuaJNI.new_pjsua_logging_config(), true);
  }

}
