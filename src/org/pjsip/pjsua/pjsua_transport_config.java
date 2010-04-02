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

public class pjsua_transport_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_transport_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_transport_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_transport_config(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setPort(long value) {
    pjsuaJNI.pjsua_transport_config_port_set(swigCPtr, this, value);
  }

  public long getPort() {
    return pjsuaJNI.pjsua_transport_config_port_get(swigCPtr, this);
  }

  public void setPublic_addr(pj_str_t value) {
    pjsuaJNI.pjsua_transport_config_public_addr_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getPublic_addr() {
    long cPtr = pjsuaJNI.pjsua_transport_config_public_addr_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setBound_addr(pj_str_t value) {
    pjsuaJNI.pjsua_transport_config_bound_addr_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getBound_addr() {
    long cPtr = pjsuaJNI.pjsua_transport_config_bound_addr_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setTls_setting(SWIGTYPE_p_pjsip_tls_setting value) {
    pjsuaJNI.pjsua_transport_config_tls_setting_set(swigCPtr, this, SWIGTYPE_p_pjsip_tls_setting.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_tls_setting getTls_setting() {
    return new SWIGTYPE_p_pjsip_tls_setting(pjsuaJNI.pjsua_transport_config_tls_setting_get(swigCPtr, this), true);
  }

  public pjsua_transport_config() {
    this(pjsuaJNI.new_pjsua_transport_config(), true);
  }

}
