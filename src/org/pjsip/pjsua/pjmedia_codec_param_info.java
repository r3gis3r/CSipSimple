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

public class pjmedia_codec_param_info {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_codec_param_info(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_codec_param_info obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_codec_param_info(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setClock_rate(long value) {
    pjsuaJNI.pjmedia_codec_param_info_clock_rate_set(swigCPtr, this, value);
  }

  public long getClock_rate() {
    return pjsuaJNI.pjmedia_codec_param_info_clock_rate_get(swigCPtr, this);
  }

  public void setChannel_cnt(long value) {
    pjsuaJNI.pjmedia_codec_param_info_channel_cnt_set(swigCPtr, this, value);
  }

  public long getChannel_cnt() {
    return pjsuaJNI.pjmedia_codec_param_info_channel_cnt_get(swigCPtr, this);
  }

  public void setAvg_bps(long value) {
    pjsuaJNI.pjmedia_codec_param_info_avg_bps_set(swigCPtr, this, value);
  }

  public long getAvg_bps() {
    return pjsuaJNI.pjmedia_codec_param_info_avg_bps_get(swigCPtr, this);
  }

  public void setMax_bps(long value) {
    pjsuaJNI.pjmedia_codec_param_info_max_bps_set(swigCPtr, this, value);
  }

  public long getMax_bps() {
    return pjsuaJNI.pjmedia_codec_param_info_max_bps_get(swigCPtr, this);
  }

  public void setFrm_ptime(int value) {
    pjsuaJNI.pjmedia_codec_param_info_frm_ptime_set(swigCPtr, this, value);
  }

  public int getFrm_ptime() {
    return pjsuaJNI.pjmedia_codec_param_info_frm_ptime_get(swigCPtr, this);
  }

  public void setEnc_ptime(int value) {
    pjsuaJNI.pjmedia_codec_param_info_enc_ptime_set(swigCPtr, this, value);
  }

  public int getEnc_ptime() {
    return pjsuaJNI.pjmedia_codec_param_info_enc_ptime_get(swigCPtr, this);
  }

  public void setPcm_bits_per_sample(short value) {
    pjsuaJNI.pjmedia_codec_param_info_pcm_bits_per_sample_set(swigCPtr, this, value);
  }

  public short getPcm_bits_per_sample() {
    return pjsuaJNI.pjmedia_codec_param_info_pcm_bits_per_sample_get(swigCPtr, this);
  }

  public void setPt(short value) {
    pjsuaJNI.pjmedia_codec_param_info_pt_set(swigCPtr, this, value);
  }

  public short getPt() {
    return pjsuaJNI.pjmedia_codec_param_info_pt_get(swigCPtr, this);
  }

  public pjmedia_codec_param_info() {
    this(pjsuaJNI.new_pjmedia_codec_param_info(), true);
  }

}
