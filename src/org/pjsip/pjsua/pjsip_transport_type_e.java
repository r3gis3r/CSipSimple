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

public enum pjsip_transport_type_e {
  PJSIP_TRANSPORT_UNSPECIFIED,
  PJSIP_TRANSPORT_UDP,
  PJSIP_TRANSPORT_TCP,
  PJSIP_TRANSPORT_TLS,
  PJSIP_TRANSPORT_SCTP,
  PJSIP_TRANSPORT_LOOP,
  PJSIP_TRANSPORT_LOOP_DGRAM,
  PJSIP_TRANSPORT_START_OTHER,
  PJSIP_TRANSPORT_IPV6(pjsuaJNI.PJSIP_TRANSPORT_IPV6_get()),
  PJSIP_TRANSPORT_UDP6(pjsuaJNI.PJSIP_TRANSPORT_UDP6_get()),
  PJSIP_TRANSPORT_TCP6(pjsuaJNI.PJSIP_TRANSPORT_TCP6_get());

  public final int swigValue() {
    return swigValue;
  }

  public static pjsip_transport_type_e swigToEnum(int swigValue) {
    pjsip_transport_type_e[] swigValues = pjsip_transport_type_e.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjsip_transport_type_e swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjsip_transport_type_e.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private pjsip_transport_type_e() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private pjsip_transport_type_e(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private pjsip_transport_type_e(pjsip_transport_type_e swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

