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

public class pjsip_event_body {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsip_event_body(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsip_event_body obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsip_event_body(swigCPtr);
    }
    swigCPtr = 0;
  }

  public pjsip_event_body_user getUser() {
    long cPtr = pjsuaJNI.pjsip_event_body_user_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_user(cPtr, false);
  }

  public pjsip_event_body_rx_msg getRx_msg() {
    long cPtr = pjsuaJNI.pjsip_event_body_rx_msg_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_rx_msg(cPtr, false);
  }

  public pjsip_event_body_tx_error getTx_error() {
    long cPtr = pjsuaJNI.pjsip_event_body_tx_error_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_tx_error(cPtr, false);
  }

  public pjsip_event_body_tx_msg getTx_msg() {
    long cPtr = pjsuaJNI.pjsip_event_body_tx_msg_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_tx_msg(cPtr, false);
  }

  public pjsip_event_body_tsx_state getTsx_state() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_tsx_state(cPtr, false);
  }

  public pjsip_event_body_timer getTimer() {
    long cPtr = pjsuaJNI.pjsip_event_body_timer_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_timer(cPtr, false);
  }

  public pjsip_event_body() {
    this(pjsuaJNI.new_pjsip_event_body(), true);
  }

}
