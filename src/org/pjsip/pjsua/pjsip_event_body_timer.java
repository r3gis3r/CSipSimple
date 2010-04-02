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

public class pjsip_event_body_timer {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsip_event_body_timer(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsip_event_body_timer obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsip_event_body_timer(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setEntry(SWIGTYPE_p_pj_timer_entry value) {
    pjsuaJNI.pjsip_event_body_timer_entry_set(swigCPtr, this, SWIGTYPE_p_pj_timer_entry.getCPtr(value));
  }

  public SWIGTYPE_p_pj_timer_entry getEntry() {
    long cPtr = pjsuaJNI.pjsip_event_body_timer_entry_get(swigCPtr, this);
    return (cPtr == 0) ? null : new SWIGTYPE_p_pj_timer_entry(cPtr, false);
  }

  public pjsip_event_body_timer() {
    this(pjsuaJNI.new_pjsip_event_body_timer(), true);
  }

}
