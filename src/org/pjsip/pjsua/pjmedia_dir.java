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

public enum pjmedia_dir {
  PJMEDIA_DIR_NONE(pjsuaJNI.PJMEDIA_DIR_NONE_get()),
  PJMEDIA_DIR_ENCODING(pjsuaJNI.PJMEDIA_DIR_ENCODING_get()),
  PJMEDIA_DIR_DECODING(pjsuaJNI.PJMEDIA_DIR_DECODING_get()),
  PJMEDIA_DIR_ENCODING_DECODING(pjsuaJNI.PJMEDIA_DIR_ENCODING_DECODING_get());

  public final int swigValue() {
    return swigValue;
  }

  public static pjmedia_dir swigToEnum(int swigValue) {
    pjmedia_dir[] swigValues = pjmedia_dir.class.getEnumConstants();
    if (swigValue < swigValues.length && swigValue >= 0 && swigValues[swigValue].swigValue == swigValue)
      return swigValues[swigValue];
    for (pjmedia_dir swigEnum : swigValues)
      if (swigEnum.swigValue == swigValue)
        return swigEnum;
    throw new IllegalArgumentException("No enum " + pjmedia_dir.class + " with value " + swigValue);
  }

  private pjmedia_dir() {
    this.swigValue = SwigNext.next++;
  }

  private pjmedia_dir(int swigValue) {
    this.swigValue = swigValue;
    SwigNext.next = swigValue+1;
  }
  
  private pjmedia_dir(pjmedia_dir swigEnum) {
    this.swigValue = swigEnum.swigValue;
    SwigNext.next = this.swigValue+1;
  }

  private final int swigValue;

  private static class SwigNext {
    private static int next = 0;
  }
}

