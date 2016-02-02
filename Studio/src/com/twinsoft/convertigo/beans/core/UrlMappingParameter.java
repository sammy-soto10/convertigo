/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

public abstract class UrlMappingParameter extends DatabaseObject {

	private static final long serialVersionUID = -2280875929012349646L;

	public enum Type {
		Path,
		Query,
		Body,
		Form,
		Header;
	}
	
	public UrlMappingParameter() {
		super();
	}
	
	@Override
	public UrlMappingParameter clone() throws CloneNotSupportedException {
		UrlMappingParameter clonedObject = (UrlMappingParameter)super.clone();
		return clonedObject;
	}

	abstract public Type getType();
	
	private Boolean required = Boolean.TRUE;
	
	public Boolean isRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}
	
	private Boolean multiValued = Boolean.FALSE;
	
	public Boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(Boolean multiValued) {
		this.multiValued = multiValued;
	}
	
	private String mappedVariableName = "";

	public String getMappedVariableName() {
		return mappedVariableName;
	}

	public void setMappedVariableName(String mappedVariableName) {
		this.mappedVariableName = mappedVariableName;
	}
}