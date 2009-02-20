/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.ui.progress.UIJob;

/**
 * Operation for creating a new API problem filter
 * 
 * @see IApiProblem
 * @see IApiProblemFilter
 * @see IApiFilterStore
 * 
 * @since 1.0.0
 */
public class CreateApiFilterOperation extends UIJob {

	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param element the element to create the filter for (method, field, class, enum, etc)
	 * @param kind the kind of filter to create
	 * 
	 * @see IApiProblemFilter#getKinds()
	 */
	public CreateApiFilterOperation(IMarker marker) {
		super(MarkerMessages.CreateApiFilterOperation_0);
		fBackingMarker = marker;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			IResource resource = fBackingMarker.getResource();
			IProject project = resource.getProject();
			if(project == null) {
				return Status.CANCEL_STATUS;
			}
			IApiComponent component = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline().getApiComponent(project.getName());
			if(component == null) {
				return Status.CANCEL_STATUS;
			}
			IApiFilterStore store = component.getFilterStore();
			String typeNameFromMarker = Util.getTypeNameFromMarker(fBackingMarker);
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
					typeNameFromMarker,
					getMessageArgumentsFromMarker(), 
					null,
					null,
					fBackingMarker.getAttribute(IMarker.LINE_NUMBER, -1), 
					fBackingMarker.getAttribute(IMarker.CHAR_START, -1),
					fBackingMarker.getAttribute(IMarker.CHAR_END, -1), 
					fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0));
			store.addFilters(new IApiProblem[] {problem});
			Util.touchCorrespondingResource(project, resource, typeNameFromMarker);
			if(!ResourcesPlugin.getWorkspace().isAutoBuilding()) {
				Util.getBuildJob(new IProject[] {resource.getProject()}, IncrementalProjectBuilder.INCREMENTAL_BUILD).schedule();
			}
			return Status.OK_STATUS;
		}
		catch(CoreException ce) {
			ApiUIPlugin.log(ce);
		}
		return Status.CANCEL_STATUS;
	}
	
	/**
	 * @return the listing of message arguments from the marker.
	 */
	private String[] getMessageArgumentsFromMarker() {
		ArrayList args = new ArrayList();
		String arguments = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, null);
		if(arguments != null) {
			return arguments.split("#"); //$NON-NLS-1$
		}
		return (String[]) args.toArray(new String[args.size()]);
	}
}