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

public class pjsip_cred_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsip_cred_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsip_cred_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsip_cred_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setRealm(pj_str_t value) {
    pjsuaJNI.pjsip_cred_info_realm_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getRealm() {
    long cPtr = pjsuaJNI.pjsip_cred_info_realm_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setScheme(pj_str_t value) {
    pjsuaJNI.pjsip_cred_info_scheme_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getScheme() {
    long cPtr = pjsuaJNI.pjsip_cred_info_scheme_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setUsername(pj_str_t value) {
    pjsuaJNI.pjsip_cred_info_username_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getUsername() {
    long cPtr = pjsuaJNI.pjsip_cred_info_username_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setData_type(int value) {
    pjsuaJNI.pjsip_cred_info_data_type_set(swigCPtr, this, value);
  }

  public int getData_type() {
    return pjsuaJNI.pjsip_cred_info_data_type_get(swigCPtr, this);
  }

  public void setData(pj_str_t value) {
    pjsuaJNI.pjsip_cred_info_data_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getData() {
    long cPtr = pjsuaJNI.pjsip_cred_info_data_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public pjsip_cred_info_ext getExt() {
    long cPtr = pjsuaJNI.pjsip_cred_info_ext_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_cred_info_ext(cPtr, false);
  }

  public pjsip_cred_info() {
    this(pjsuaJNI.new_pjsip_cred_info(), true);
  }

}
