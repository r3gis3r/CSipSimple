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

public class pjmedia_port_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_port_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_port_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_port_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setName(pj_str_t value) {
    pjsuaJNI.pjmedia_port_info_name_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getName() {
    long cPtr = pjsuaJNI.pjmedia_port_info_name_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setSignature(long value) {
    pjsuaJNI.pjmedia_port_info_signature_set(swigCPtr, this, value);
  }

  public long getSignature() {
    return pjsuaJNI.pjmedia_port_info_signature_get(swigCPtr, this);
  }

  public void setType(SWIGTYPE_p_pjmedia_type value) {
    pjsuaJNI.pjmedia_port_info_type_set(swigCPtr, this, SWIGTYPE_p_pjmedia_type.getCPtr(value));
  }

  public SWIGTYPE_p_pjmedia_type getType() {
    return new SWIGTYPE_p_pjmedia_type(pjsuaJNI.pjmedia_port_info_type_get(swigCPtr, this), true);
  }

  public void setHas_info(int value) {
    pjsuaJNI.pjmedia_port_info_has_info_set(swigCPtr, this, value);
  }

  public int getHas_info() {
    return pjsuaJNI.pjmedia_port_info_has_info_get(swigCPtr, this);
  }

  public void setNeed_info(int value) {
    pjsuaJNI.pjmedia_port_info_need_info_set(swigCPtr, this, value);
  }

  public int getNeed_info() {
    return pjsuaJNI.pjmedia_port_info_need_info_get(swigCPtr, this);
  }

  public void setPt(long value) {
    pjsuaJNI.pjmedia_port_info_pt_set(swigCPtr, this, value);
  }

  public long getPt() {
    return pjsuaJNI.pjmedia_port_info_pt_get(swigCPtr, this);
  }

  public void setEncoding_name(pj_str_t value) {
    pjsuaJNI.pjmedia_port_info_encoding_name_set(swigCPtr, this, pj_str_t.getCPtr(value), value);
  }

  public pj_str_t getEncoding_name() {
    long cPtr = pjsuaJNI.pjmedia_port_info_encoding_name_get(swigCPtr, this);
    return (cPtr == 0) ? null : new pj_str_t(cPtr, false);
  }

  public void setClock_rate(long value) {
    pjsuaJNI.pjmedia_port_info_clock_rate_set(swigCPtr, this, value);
  }

  public long getClock_rate() {
    return pjsuaJNI.pjmedia_port_info_clock_rate_get(swigCPtr, this);
  }

  public void setChannel_count(long value) {
    pjsuaJNI.pjmedia_port_info_channel_count_set(swigCPtr, this, value);
  }

  public long getChannel_count() {
    return pjsuaJNI.pjmedia_port_info_channel_count_get(swigCPtr, this);
  }

  public void setBits_per_sample(long value) {
    pjsuaJNI.pjmedia_port_info_bits_per_sample_set(swigCPtr, this, value);
  }

  public long getBits_per_sample() {
    return pjsuaJNI.pjmedia_port_info_bits_per_sample_get(swigCPtr, this);
  }

  public void setSamples_per_frame(long value) {
    pjsuaJNI.pjmedia_port_info_samples_per_frame_set(swigCPtr, this, value);
  }

  public long getSamples_per_frame() {
    return pjsuaJNI.pjmedia_port_info_samples_per_frame_get(swigCPtr, this);
  }

  public void setBytes_per_frame(long value) {
    pjsuaJNI.pjmedia_port_info_bytes_per_frame_set(swigCPtr, this, value);
  }

  public long getBytes_per_frame() {
    return pjsuaJNI.pjmedia_port_info_bytes_per_frame_get(swigCPtr, this);
  }

  public pjmedia_port_info() {
    this(pjsuaJNI.new_pjmedia_port_info(), true);
  }

}
