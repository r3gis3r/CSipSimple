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

public enum pjsip_cred_data_type {
  PJSIP_CRED_DATA_PLAIN_PASSWD(pjsuaJNI.PJSIP_CRED_DATA_PLAIN_PASSWD_get()),
  PJSIP_CRED_DATA_DIGEST(pjsuaJNI.PJSIP_CRED_DATA_DIGEST_get()),
  PJSIP_CRED_DATA_EXT_AKA(pjsuaJNI.PJSIP_CRED_DATA_EXT_AKA_get());

  public final int swigValue() {
    return swigValue;
  }

  public static pjsip_cred_data_type swigToEnum(int swigValue) {
    pjsip_cred_data_type[] swigValues = pjsip_cred_data_type.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjsip_cred_data_type swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjsip_cred_data_type.class + " with value " + swigValue);
  }

  @SuppressWarnings("unused")
  private pjsip_cred_data_type() {
    this.swigValue = SwigNext.next++;
  }

  @SuppressWarnings("unused")
  private pjsip_cred_data_type(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }

  @SuppressWarnings("unused")
  private pjsip_cred_data_type(pjsip_cred_data_type swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

