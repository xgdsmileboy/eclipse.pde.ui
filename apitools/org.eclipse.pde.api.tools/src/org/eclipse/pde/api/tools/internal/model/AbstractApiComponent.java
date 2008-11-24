/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

/**
 * Common implementation of an API component as a composite class file container.
 * 
 * @since 1.0.0
 */
public abstract class AbstractApiComponent extends AbstractApiTypeContainer implements IApiComponent {
	/**
	 * API description
	 */
	private IApiDescription fApiDescription = null;
		
	/**
	 * Api Filter store
	 */
	private IApiFilterStore fFilterStore = null;
	
	/**
	 * Constructs an API component in the given {@link IApiBaseline}.
	 * 
	 * @param baseline the parent {@link IApiBaseline}
	 */
	public AbstractApiComponent(IApiBaseline baseline) {
		super(baseline, IApiElement.COMPONENT, null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		if (visitor.visit(this)) {
			super.accept(visitor);
		}
		visitor.end(this);
	}	
		
	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#getBaseline()
	 */
	public IApiBaseline getBaseline() {
		return (IApiBaseline) getAncestor(IApiElement.BASELINE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#dispose()
	 */
	public void dispose() {
		try {
			close();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		fApiDescription = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getApiDescription()
	 */
	public synchronized IApiDescription getApiDescription() throws CoreException {
		if (fApiDescription == null) {
			fApiDescription = createApiDescription();
		}
		return fApiDescription;
	}
	
	/**
	 * Returns whether this component has created an API description.
	 * 
	 * @return whether this component has created an API description
	 */
	protected synchronized boolean isApiDescriptionInitialized() {
		return fApiDescription != null;
	}

	/**
	 * Returns if this component has created an API filter store
	 * 
	 * @return true if a store has been created, false other wise
	 */
	protected synchronized boolean hasApiFilterStore() {
		return fFilterStore != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public synchronized IApiTypeContainer[] getApiTypeContainers() {
		return super.getApiTypeContainers();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractClassFileContainer#getClassFileContainers()
	 */
	public synchronized IApiTypeContainer[] getApiTypeContainers(String id) {
		if (this.hasFragments()) {
			return super.getApiTypeContainers(id);
		} else {
			return super.getApiTypeContainers();
		}
	}
	
	/**
	 * Creates and returns the API description for this component.
	 * 
	 * @return newly created API description for this component
	 */
	protected abstract IApiDescription createApiDescription() throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#getFilterStore()
	 */
	public IApiFilterStore getFilterStore() throws CoreException {
		if(fFilterStore == null) {
			fFilterStore = createApiFilterStore();
		}
		return fFilterStore;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#newProblemFilter(org.eclipse.pde.api.tools.internal.provisional.IApiProblem)
	 */
	public IApiProblemFilter newProblemFilter(IApiProblem problem) {
		//TODO either expose a way to make problems or change the method to accept the parts of a problem
		return new ApiProblemFilter(getId(), problem);
	}
	
	/**
	 * Lazily creates a new {@link IApiFilterStore} when it is requested
	 * 
	 * @return the current {@link IApiFilterStore} for this component
	 * @throws CoreException
	 */
	protected abstract IApiFilterStore createApiFilterStore() throws CoreException;	
}