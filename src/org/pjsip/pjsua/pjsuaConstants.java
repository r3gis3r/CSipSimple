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

public interface pjsuaConstants {
  public final static pjsua_callback WRAPPER_CALLBACK_STRUCT = new pjsua_callback(pjsuaJNI.WRAPPER_CALLBACK_STRUCT_get(), false);
  public final static int PJ_SUCCESS = pjsuaJNI.PJ_SUCCESS_get();
  public final static int PJ_TRUE = pjsuaJNI.PJ_TRUE_get();
  public final static int PJ_FALSE = pjsuaJNI.PJ_FALSE_get();
  public final static int PJMEDIA_TONEGEN_LOOP = pjsuaJNI.PJMEDIA_TONEGEN_LOOP_get();
  public final static int PJMEDIA_TONEGEN_NO_LOCK = pjsuaJNI.PJMEDIA_TONEGEN_NO_LOCK_get();

  public final static int PJSUA_INVALID_ID = pjsuaJNI.PJSUA_INVALID_ID_get();
  public final static int PJSUA_ACC_MAX_PROXIES = pjsuaJNI.PJSUA_ACC_MAX_PROXIES_get();
  public final static int PJSUA_MAX_ACC = pjsuaJNI.PJSUA_MAX_ACC_get();
  public final static int PJSUA_REG_INTERVAL = pjsuaJNI.PJSUA_REG_INTERVAL_get();
  public final static int PJSUA_PUBLISH_EXPIRATION = pjsuaJNI.PJSUA_PUBLISH_EXPIRATION_get();
  public final static int PJSUA_DEFAULT_ACC_PRIORITY = pjsuaJNI.PJSUA_DEFAULT_ACC_PRIORITY_get();
  public final static String PJSUA_SECURE_SCHEME = pjsuaJNI.PJSUA_SECURE_SCHEME_get();
  public final static int PJSUA_MAX_CALLS = pjsuaJNI.PJSUA_MAX_CALLS_get();
  public final static int PJSUA_XFER_NO_REQUIRE_REPLACES = pjsuaJNI.PJSUA_XFER_NO_REQUIRE_REPLACES_get();
  public final static int PJSUA_MAX_BUDDIES = pjsuaJNI.PJSUA_MAX_BUDDIES_get();
  public final static int PJSUA_PRES_TIMER = pjsuaJNI.PJSUA_PRES_TIMER_get();
  public final static int PJSUA_MAX_CONF_PORTS = pjsuaJNI.PJSUA_MAX_CONF_PORTS_get();
  public final static int PJSUA_DEFAULT_CLOCK_RATE = pjsuaJNI.PJSUA_DEFAULT_CLOCK_RATE_get();
  public final static int PJSUA_DEFAULT_AUDIO_FRAME_PTIME = pjsuaJNI.PJSUA_DEFAULT_AUDIO_FRAME_PTIME_get();
  public final static int PJSUA_DEFAULT_CODEC_QUALITY = pjsuaJNI.PJSUA_DEFAULT_CODEC_QUALITY_get();
  public final static int PJSUA_DEFAULT_ILBC_MODE = pjsuaJNI.PJSUA_DEFAULT_ILBC_MODE_get();
  public final static int PJSUA_DEFAULT_EC_TAIL_LEN = pjsuaJNI.PJSUA_DEFAULT_EC_TAIL_LEN_get();
  public final static int PJSUA_MAX_PLAYERS = pjsuaJNI.PJSUA_MAX_PLAYERS_get();
  public final static int PJSUA_MAX_RECORDERS = pjsuaJNI.PJSUA_MAX_RECORDERS_get();
}
