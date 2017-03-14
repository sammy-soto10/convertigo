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

package com.twinsoft.convertigo.beans.mobile.components;

public class RouteExceptionActionComponent extends RouteActionComponent {

	private static final long serialVersionUID = -8882933502852801495L;

	public RouteExceptionActionComponent() {
		super();
		this.action = Action.toast.name();
	}
	
	@Override
	public RouteExceptionActionComponent clone() throws CloneNotSupportedException {
		RouteExceptionActionComponent cloned = (RouteExceptionActionComponent)super.clone();
		return cloned;
	}

	@Override
	public String toString() {
		return "toast Exception";
	}
	
	@Override
	public String computeRoute() {
		StringBuilder sb = new StringBuilder();
		sb.append("new C8oRoute((exception:any)=>{return true})")
			.append(".setTarget(\""+Action.toast.name()+"\")")
			.append(".setToastMesage(\""+ "Exception" +"\")");
		
		String position = getToastPosition();
		sb.append(".setToastPosition(\""+ (position.isEmpty() ? "bottom":position) +"\")");
		
		String duration = ""+ getToastDuration();
		sb.append(".setToastDuration("+ (duration.isEmpty() ? "5000":duration) +")");
		
		return sb.toString();
	}

}
