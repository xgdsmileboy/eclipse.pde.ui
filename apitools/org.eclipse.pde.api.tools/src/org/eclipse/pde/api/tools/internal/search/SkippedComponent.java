/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;


public class SkippedComponent implements IApiElement{
	/**
	 * the id of of the skipped component
	 */
	private String componentid;
	/**
	 * The version of the component 
	 */
	private String version;
	/**
	 * the set of resolution errors barring the component from being scanned
	 */
	private ResolverError[] errors = null;

	/**
	 * Constructor
	 * @param componentid
	 * @param version
	 * @param errors the {@link ResolverError}s, if any, that prevented this component from being scanned
	 */
	public SkippedComponent(String componentid, String version, ResolverError[] errors) {
		this.componentid = componentid;
		this.version = version;
		this.errors = errors;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof SkippedComponent) {
			return this.componentid.equals(((SkippedComponent)obj).componentid);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.componentid.hashCode();
	}
	
	/**
	 * @return the component id of the skipped component
	 */
	public String getComponentId() {
		return this.componentid;
	}
	
	/**
	 * @return true if the component was skipped because it appeared in an exclude list
	 */
	public boolean wasExcluded() {
		return this.errors == null;
	}
	
	/**
	 * @return true if the the component had resolution errors
	 */
	public boolean hasResolutionErrors() {
		return this.errors != null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getAncestor(int)
	 */
	public IApiElement getAncestor(int ancestorType) {
		return null;
	}

	/**
	 * @return the version
	 */
	public String getVersion() {
		return this.version;
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getName()
	 */
	public String getName() {
		return this.componentid;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getParent()
	 */
	public IApiElement getParent() {
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiElement#getType()
	 */
	public int getType() {
		return IApiElement.COMPONENT;
	}
	
	/**
	 * @return the errors
	 */
	public ResolverError[] getErrors() {
		return this.errors;
	}
	
	/**
	 * @return the formatted details of why the component was skipped
	 */
	public String getErrorDetails() {
		if(this.errors != null) {
			StringBuffer buffer = new StringBuffer();
			VersionConstraint constraint = null;
			VersionRange version = null;
			String min = null, max = null;
			for (int i = 0; i < this.errors.length; i++) {
				constraint = this.errors[i].getUnsatisfiedConstraint();
				if(constraint != null) {				
					buffer.append(constraint.toString()).append("\n"); //$NON-NLS-1$
				}
				else {
					buffer.append(this.errors[i].toString()).append("\n"); //$NON-NLS-1$
				}
			}
			return buffer.toString();
		}
		return SearchMessages.SkippedComponent_component_was_excluded;
	}
}