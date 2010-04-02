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

public class pjmedia_codec_param {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_codec_param(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_codec_param obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_codec_param(swigCPtr);
    }
    swigCPtr = 0;
  }

  public pjmedia_codec_param_setting getSetting() {
    long cPtr = pjsuaJNI.pjmedia_codec_param_setting_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjmedia_codec_param_setting(cPtr, false);
  }

  public pjmedia_codec_param_info getInfo() {
    long cPtr = pjsuaJNI.pjmedia_codec_param_info_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pjmedia_codec_param_info(cPtr, false);
  }

  public pjmedia_codec_param() {
    this(pjsuaJNI.new_pjmedia_codec_param(), true);
  }

}
