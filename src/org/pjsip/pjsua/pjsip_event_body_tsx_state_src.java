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

public class pjsip_event_body_tsx_state_src {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsip_event_body_tsx_state_src(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsip_event_body_tsx_state_src obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsip_event_body_tsx_state_src(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setRdata(SWIGTYPE_p_pjsip_rx_data value) {
    pjsuaJNI.pjsip_event_body_tsx_state_src_rdata_set(swigCPtr, this, SWIGTYPE_p_pjsip_rx_data.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_rx_data getRdata() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_src_rdata_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pjsip_rx_data(cPtr, false);
  }

  public void setTdata(SWIGTYPE_p_pjsip_tx_data value) {
    pjsuaJNI.pjsip_event_body_tsx_state_src_tdata_set(swigCPtr, this, SWIGTYPE_p_pjsip_tx_data.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_tx_data getTdata() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_src_tdata_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pjsip_tx_data(cPtr, false);
  }

  public void setTimer(SWIGTYPE_p_pj_timer_entry value) {
    pjsuaJNI.pjsip_event_body_tsx_state_src_timer_set(swigCPtr, this, SWIGTYPE_p_pj_timer_entry.getCPtr(value));
  }

  public SWIGTYPE_p_pj_timer_entry getTimer() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_src_timer_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pj_timer_entry(cPtr, false);
  }

  public void setStatus(int value) {
    pjsuaJNI.pjsip_event_body_tsx_state_src_status_set(swigCPtr, this, value);
  }

  public int getStatus() {
    return pjsuaJNI.pjsip_event_body_tsx_state_src_status_get(swigCPtr, this);
  }

  public void setData(byte[] value) {
    pjsuaJNI.pjsip_event_body_tsx_state_src_data_set(swigCPtr, this, value);
  }

  public byte[] getData() {
	return pjsuaJNI.pjsip_event_body_tsx_state_src_data_get(swigCPtr, this);
}

  public pjsip_event_body_tsx_state_src() {
    this(pjsuaJNI.new_pjsip_event_body_tsx_state_src(), true);
  }

}
