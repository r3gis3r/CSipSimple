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

public enum pjsip_event_id_e {
  PJSIP_EVENT_UNKNOWN,
  PJSIP_EVENT_TIMER,
  PJSIP_EVENT_TX_MSG,
  PJSIP_EVENT_RX_MSG,
  PJSIP_EVENT_TRANSPORT_ERROR,
  PJSIP_EVENT_TSX_STATE,
  PJSIP_EVENT_USER;

  public final int swigValue() {
    return swigValue;
  }

  public static pjsip_event_id_e swigToEnum(int swigValue) {
    pjsip_event_id_e[] swigValues = pjsip_event_id_e.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjsip_event_id_e swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjsip_event_id_e.class + " with value " + swigValue);
  }

  private pjsip_event_id_e() {
    this.swigValue = SwigNext.next++;
  }

  private pjsip_event_id_e(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  private pjsip_event_id_e(pjsip_event_id_e swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

