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

public class pjsua_media_transport {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_media_transport(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_media_transport obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_media_transport(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setSkinfo(SWIGTYPE_p_pjmedia_sock_info value) {
    pjsuaJNI.pjsua_media_transport_skinfo_set(swigCPtr, this, SWIGTYPE_p_pjmedia_sock_info.getCPtr(value));
  }

  public SWIGTYPE_p_pjmedia_sock_info getSkinfo() {
    return new SWIGTYPE_p_pjmedia_sock_info(pjsuaJNI.pjsua_media_transport_skinfo_get(swigCPtr, this), true);
  }

  public void setTransport(SWIGTYPE_p_pjmedia_transport value) {
    pjsuaJNI.pjsua_media_transport_transport_set(swigCPtr, this, SWIGTYPE_p_pjmedia_transport.getCPtr(value));
  }

  public SWIGTYPE_p_pjmedia_transport getTransport() {
    long cPtr = pjsuaJNI.pjsua_media_transport_transport_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pjmedia_transport(cPtr, false);
  }

  public pjsua_media_transport() {
    this(pjsuaJNI.new_pjsua_media_transport(), true);
  }

}
