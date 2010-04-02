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

public class pjmedia_port {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_port(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_port obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      throw new UnsupportedOperationException("C++ destructor does not have public access");
    }
    swigCPtr = 0;
  }

  /** This constructor creates the proxy which initially does not create nor own any C memory */
  public pjmedia_port() {
    this(0, false);
  }

  public void setInfo(pjmedia_port_info value) {
    pjsuaJNI.pjmedia_port_info_set(swigCPtr, this, pjmedia_port_info.getCPtr(value), value);
  }

  public pjmedia_port_info getInfo() {
    long cPtr = pjsuaJNI.pjmedia_port_info_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjmedia_port_info(cPtr, false);
  }

  public void setPut_frame(SWIGTYPE_p_f_p_pjmedia_port_p_q_const__pjmedia_frame__int value) {
    pjsuaJNI.pjmedia_port_put_frame_set(swigCPtr, this, SWIGTYPE_p_f_p_pjmedia_port_p_q_const__pjmedia_frame__int.getCPtr(value));
  }

  public SWIGTYPE_p_f_p_pjmedia_port_p_q_const__pjmedia_frame__int getPut_frame() {
    long cPtr = pjsuaJNI.pjmedia_port_put_frame_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_p_pjmedia_port_p_q_const__pjmedia_frame__int(cPtr, false);
  }

  public void setGet_frame(SWIGTYPE_p_f_p_pjmedia_port_p_pjmedia_frame__int value) {
    pjsuaJNI.pjmedia_port_get_frame_set(swigCPtr, this, SWIGTYPE_p_f_p_pjmedia_port_p_pjmedia_frame__int.getCPtr(value));
  }

  public SWIGTYPE_p_f_p_pjmedia_port_p_pjmedia_frame__int getGet_frame() {
    long cPtr = pjsuaJNI.pjmedia_port_get_frame_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_p_pjmedia_port_p_pjmedia_frame__int(cPtr, false);
  }

  public void setOn_destroy(SWIGTYPE_p_f_p_pjmedia_port__int value) {
    pjsuaJNI.pjmedia_port_on_destroy_set(swigCPtr, this, SWIGTYPE_p_f_p_pjmedia_port__int.getCPtr(value));
  }

  public SWIGTYPE_p_f_p_pjmedia_port__int getOn_destroy() {
    long cPtr = pjsuaJNI.pjmedia_port_on_destroy_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_f_p_pjmedia_port__int(cPtr, false);
  }

  public pjmedia_port_port_data getPort_data() {
    long cPtr = pjsuaJNI.pjmedia_port_port_data_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjmedia_port_port_data(cPtr, false);
  }

}
