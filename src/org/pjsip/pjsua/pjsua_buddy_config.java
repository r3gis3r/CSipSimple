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

public class pjsua_buddy_config {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjsua_buddy_config(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjsua_buddy_config obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjsua_buddy_config(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setUri(pj_str_t value) {
    pjsuaJNI.pjsua_buddy_config_uri_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getUri() {
    long cPtr = pjsuaJNI.pjsua_buddy_config_uri_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setSubscribe(int value) {
    pjsuaJNI.pjsua_buddy_config_subscribe_set(swigCPtr, this, value);
  }

  public int getSubscribe() {
    return pjsuaJNI.pjsua_buddy_config_subscribe_get(swigCPtr, this);
  }

  public pjsua_buddy_config() {
    this(pjsuaJNI.new_pjsua_buddy_config(), true);
  }

}
