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

public enum pjsip_inv_state {
  PJSIP_INV_STATE_NULL,
  PJSIP_INV_STATE_CALLING,
  PJSIP_INV_STATE_INCOMING,
  PJSIP_INV_STATE_EARLY,
  PJSIP_INV_STATE_CONNECTING,
  PJSIP_INV_STATE_CONFIRMED,
  PJSIP_INV_STATE_DISCONNECTED;

  public final int swigValue() {
    return swigValue;
  }

  public static pjsip_inv_state swigToEnum(int swigValue) {
    pjsip_inv_state[] swigValues = pjsip_inv_state.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjsip_inv_state swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjsip_inv_state.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private pjsip_inv_state() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private pjsip_inv_state(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private pjsip_inv_state(pjsip_inv_state swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

