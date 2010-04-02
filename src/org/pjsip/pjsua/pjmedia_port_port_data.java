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

public class pjmedia_port_port_data {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_port_port_data(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_port_port_data obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_port_port_data(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setPdata(byte[] value) {
    pjsuaJNI.pjmedia_port_port_data_pdata_set(swigCPtr, this, value);
  }

  public byte[] getPdata() {
	return pjsuaJNI.pjmedia_port_port_data_pdata_get(swigCPtr, this);
}

  public void setLdata(int value) {
    pjsuaJNI.pjmedia_port_port_data_ldata_set(swigCPtr, this, value);
  }

  public int getLdata() {
    return pjsuaJNI.pjmedia_port_port_data_ldata_get(swigCPtr, this);
  }

  public pjmedia_port_port_data() {
    this(pjsuaJNI.new_pjmedia_port_port_data(), true);
  }

}
