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

public class pjmedia_codec_param_setting {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected pjmedia_codec_param_setting(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(pjmedia_codec_param_setting obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if(swigCPtr != 0 && swigCMemOwn) {
      swigCMemOwn = false;
      pjsuaJNI.delete_pjmedia_codec_param_setting(swigCPtr);
    }
    swigCPtr = 0;
  }

  public void setFrm_per_pkt(short value) {
    pjsuaJNI.pjmedia_codec_param_setting_frm_per_pkt_set(swigCPtr, this, value);
  }

  public short getFrm_per_pkt() {
    return pjsuaJNI.pjmedia_codec_param_setting_frm_per_pkt_get(swigCPtr, this);
  }

  public void setVad(long value) {
    pjsuaJNI.pjmedia_codec_param_setting_vad_set(swigCPtr, this, value);
  }

  public long getVad() {
    return pjsuaJNI.pjmedia_codec_param_setting_vad_get(swigCPtr, this);
  }

  public void setCng(long value) {
    pjsuaJNI.pjmedia_codec_param_setting_cng_set(swigCPtr, this, value);
  }

  public long getCng() {
    return pjsuaJNI.pjmedia_codec_param_setting_cng_get(swigCPtr, this);
  }

  public void setPenh(long value) {
    pjsuaJNI.pjmedia_codec_param_setting_penh_set(swigCPtr, this, value);
  }

  public long getPenh() {
    return pjsuaJNI.pjmedia_codec_param_setting_penh_get(swigCPtr, this);
  }

  public void setPlc(long value) {
    pjsuaJNI.pjmedia_codec_param_setting_plc_set(swigCPtr, this, value);
  }

  public long getPlc() {
    return pjsuaJNI.pjmedia_codec_param_setting_plc_get(swigCPtr, this);
  }

  public void setReserved(long value) {
    pjsuaJNI.pjmedia_codec_param_setting_reserved_set(swigCPtr, this, value);
  }

  public long getReserved() {
    return pjsuaJNI.pjmedia_codec_param_setting_reserved_get(swigCPtr, this);
  }

  public void setEnc_fmtp_mode(short value) {
    pjsuaJNI.pjmedia_codec_param_setting_enc_fmtp_mode_set(swigCPtr, this, value);
  }

  public short getEnc_fmtp_mode() {
    return pjsuaJNI.pjmedia_codec_param_setting_enc_fmtp_mode_get(swigCPtr, this);
  }

  public void setDec_fmtp_mode(short value) {
    pjsuaJNI.pjmedia_codec_param_setting_dec_fmtp_mode_set(swigCPtr, this, value);
  }

  public short getDec_fmtp_mode() {
    return pjsuaJNI.pjmedia_codec_param_setting_dec_fmtp_mode_get(swigCPtr, this);
  }

  public pjmedia_codec_param_setting() {
    this(pjsuaJNI.new_pjmedia_codec_param_setting(), true);
  }

}
