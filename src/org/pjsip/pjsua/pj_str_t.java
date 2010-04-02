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

public class pj_str_t {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pj_str_t(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pj_str_t obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pj_str_t(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setPtr(String value) {
    pjsuaJNI.pj_str_t_ptr_set(swigCPtr, this, value);
  }

  public String getPtr() {
    return pjsuaJNI.pj_str_t_ptr_get(swigCPtr, this);
  }

  public void setSlen(SWIGTYPE_p_pj_ssize_t value) {
    pjsuaJNI.pj_str_t_slen_set(swigCPtr, this, SWIGTYPE_p_pj_ssize_t.getCPtr(value));
  }

  public SWIGTYPE_p_pj_ssize_t getSlen() {
    return new SWIGTYPE_p_pj_ssize_t(pjsuaJNI.pj_str_t_slen_get(swigCPtr, this), true);
  }

  public pj_str_t() {
    this(pjsuaJNI.new_pj_str_t(), true);
  }

}
