/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */
package com.twinsoft.convertigo.beans.core;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;

public abstract class MobileComponent extends MobileObject {

	private static final long serialVersionUID = 5069650793367687807L;
	
	public MobileComponent() {
		super();
		databaseType = "MobileComponent";
	}
	
	@Override
	public MobileComponent clone() throws CloneNotSupportedException {
		MobileComponent cloned = (MobileComponent) super.clone();
		return cloned;
	}
	
	public ApplicationComponent getApplication() {
		DatabaseObject databaseObject = this;
		while (!(databaseObject instanceof ApplicationComponent) && databaseObject != null) { 
			databaseObject = databaseObject.getParent();
		}
		
		if (databaseObject == null)
			return null;
		else
			return (ApplicationComponent) databaseObject;
	}
	
	private String getTplCafVersion() {
		return getProject().getMobileBuilder().getTplCafVersion();
	}
	
	protected int compareToTplCafVersion(String version) {
		int result = -1;
		if (version != null) {
			String tplCafVersion = getTplCafVersion();
			if (tplCafVersion != null) {
				tplCafVersion = tplCafVersion.trim().toLowerCase();
				if (tplCafVersion.equals("latest")) {
					result = 1;
				} else {
					result = tplCafVersion.compareTo(version.trim().toLowerCase());
				}
			}
		}
		return result;
	}
}