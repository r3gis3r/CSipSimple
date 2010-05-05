/**
 * Copyright (C) 2010 Regis Montoya (aka r3gis - www.r3gis.fr)
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
package com.csipsimple.models;

/**
 * Utility class to be passed as message for example
 * @author r3gis3r
 *
 */
public class DownloadProgress {
	private final long downloaded;
	private final long total;

	/**
	 * Create a new download progress info object
	 * @param downloaded amount of downloaded bytes
	 * @param total total of bytes to be downloaded
	 */
	public DownloadProgress(long aDownloaded, long aTotal) {
		downloaded = aDownloaded;
		total = aTotal;
	}
	
	/**
	 * Get the amount of downloaded bytes
	 * @return amount of downloaded bytes
	 */
	public long getDownloaded() {
		return downloaded;
	}
	
	/**
	 * Get the amount of downloaded bytes
	 * @return amount of downloaded bytes
	 */
	public long getTotal() {
		return total;
	}
}
