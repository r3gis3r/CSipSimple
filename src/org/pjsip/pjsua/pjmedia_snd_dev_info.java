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

public class pjmedia_snd_dev_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_snd_dev_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_snd_dev_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_snd_dev_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setName(String value) {
    pjsuaJNI.pjmedia_snd_dev_info_name_set(swigCPtr, this, value);
  }

  public String getName() {
    return pjsuaJNI.pjmedia_snd_dev_info_name_get(swigCPtr, this);
  }

  public void setInput_count(long value) {
    pjsuaJNI.pjmedia_snd_dev_info_input_count_set(swigCPtr, this, value);
  }

  public long getInput_count() {
    return pjsuaJNI.pjmedia_snd_dev_info_input_count_get(swigCPtr, this);
  }

  public void setOutput_count(long value) {
    pjsuaJNI.pjmedia_snd_dev_info_output_count_set(swigCPtr, this, value);
  }

  public long getOutput_count() {
    return pjsuaJNI.pjmedia_snd_dev_info_output_count_get(swigCPtr, this);
  }

  public void setDefault_samples_per_sec(long value) {
    pjsuaJNI.pjmedia_snd_dev_info_default_samples_per_sec_set(swigCPtr, this, value);
  }

  public long getDefault_samples_per_sec() {
    return pjsuaJNI.pjmedia_snd_dev_info_default_samples_per_sec_get(swigCPtr, this);
  }

  public pjmedia_snd_dev_info() {
    this(pjsuaJNI.new_pjmedia_snd_dev_info(), true);
  }

}
