/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IRepositoryInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class RepositoryInfo extends ProductObject implements IRepositoryInfo {

	private static final long serialVersionUID = 1L;

	public static final String P_LOCATION = "location"; //$NON-NLS-1$
	public static final String P_ENABLED = "enabled"; //$NON-NLS-1$

	private String fURL;
	private boolean fEnabled = true; // enabled unless specified otherwise

	public RepositoryInfo(IProductModel model) {
		super(model);
	}

	public void setURL(String url) {
		String old = fURL;
		fURL = url;
		if (isEditable())
			firePropertyChanged(P_LOCATION, old, fURL);
	}

	public String getURL() {
		return fURL;
	}

	public boolean getEnabled() {
		return fEnabled;
	}

	public void setEnabled(boolean enabled) {
		boolean old = fEnabled;
		fEnabled = enabled;
		if (isEditable())
			firePropertyChanged(P_ENABLED, old, fEnabled);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute("location"); //$NON-NLS-1$
			fEnabled = Boolean.valueOf(element.getAttribute(P_ENABLED)).booleanValue();
		}
	}


	public void write(String indent, PrintWriter writer) {
		if (isURLDefined()) {
			writer.print(indent + "<repository location=\"" + fURL + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.print(" enabled=\"" + fEnabled + "\""); //$NON-NLS-1$//$NON-NLS-2$
			writer.println(" />"); //$NON-NLS-1$
		}
	}

	private boolean isURLDefined() {
		return fURL != null && fURL.length() > 0;
	}

}
