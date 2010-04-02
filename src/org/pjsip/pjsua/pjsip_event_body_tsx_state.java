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

public class pjsip_event_body_tsx_state {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsip_event_body_tsx_state(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsip_event_body_tsx_state obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsip_event_body_tsx_state(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setTsx(SWIGTYPE_p_pjsip_transaction value) {
    pjsuaJNI.pjsip_event_body_tsx_state_tsx_set(swigCPtr, this, SWIGTYPE_p_pjsip_transaction.getCPtr(value));
  }

  public SWIGTYPE_p_pjsip_transaction getTsx() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_tsx_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pjsip_transaction(cPtr, false);
  }

  public void setPrev_state(int value) {
    pjsuaJNI.pjsip_event_body_tsx_state_prev_state_set(swigCPtr, this, value);
  }

  public int getPrev_state() {
    return pjsuaJNI.pjsip_event_body_tsx_state_prev_state_get(swigCPtr, this);
  }

  public void setType(pjsip_event_id_e value) {
    pjsuaJNI.pjsip_event_body_tsx_state_type_set(swigCPtr, this, value.swigValue());
  }

  public pjsip_event_id_e getType() {
    return pjsip_event_id_e.swigToEnum(pjsuaJNI.pjsip_event_body_tsx_state_type_get(swigCPtr, this));
  }

  public pjsip_event_body_tsx_state_src getSrc() {
    long cPtr = pjsuaJNI.pjsip_event_body_tsx_state_src_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjsip_event_body_tsx_state_src(cPtr, false);
  }

  public pjsip_event_body_tsx_state() {
    this(pjsuaJNI.new_pjsip_event_body_tsx_state(), true);
  }

}
