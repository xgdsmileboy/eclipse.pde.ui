/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;

/**
 * Ant task to compare API scopes.
 */
public class CompareTask extends CommonUtilsTask {
	
	private static final String VISIBILITY_ALL = "ALL"; //$NON-NLS-1$
	private static final String VISIBILITY_API = "API"; //$NON-NLS-1$
	private static final String REPORT_XML_FILE_NAME = "compare.xml"; //$NON-NLS-1$

	private int visibilityModifiers = VisibilityModifiers.API;
	private String componentsList;
	private String excludeListLocation;

	public void execute() throws BuildException {
		if (this.referenceBaselineLocation == null
				|| this.currentBaselineLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				Messages.bind(Messages.printArguments,
					new String[] {
						this.referenceBaselineLocation,
						this.currentBaselineLocation,
						this.reportLocation,
					})
			);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.debug) {
			System.out.println("Reference baseline : " + this.referenceBaselineLocation); //$NON-NLS-1$
			System.out.println("Baseline to compare : " + this.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("Report location : " + this.reportLocation); //$NON-NLS-1$
			System.out.println("Component's list : " + this.componentsList); //$NON-NLS-1$
			if (this.excludeListLocation != null) {
				System.out.println("exclude list location : " + this.excludeListLocation); //$NON-NLS-1$
			} else {
				System.out.println("No exclude list location"); //$NON-NLS-1$
			}
		}
		// create reference
		File referenceInstallDir = extractSDK(REFERENCE, this.referenceBaselineLocation);

		File baselineInstallDir = extractSDK(CURRENT, this.currentBaselineLocation);

		// run the comparison
		// create baseline for the reference
		IApiBaseline referenceBaseline = createBaseline(REFERENCE_PROFILE_NAME, getInstallDir(referenceInstallDir), this.eeFileLocation);
		IApiBaseline currentBaseline = createBaseline(CURRENT_BASELINE_NAME, getInstallDir(baselineInstallDir), this.eeFileLocation);
		
		IDelta delta = null;
		
		ApiScope scope = new ApiScope();
		if (this.componentsList != null) {
			// needs to set up individual components
			IApiComponent[] apiComponents = currentBaseline.getApiComponents();
			String[] componentsNames = this.componentsList.split(","); //$NON-NLS-1$
			if (componentsNames.length == 0) {
				scope.add(currentBaseline);
			} else {
				for (int i = 0, max = componentsNames.length; i < max; i++) {
					String componentName = componentsNames[i];
					componentName = componentName.trim();
					if (componentName.startsWith(REGULAR_EXPRESSION_START)) {
						// regular expression
						componentName = componentName.substring(2);
						Pattern pattern = null;
						try {
							pattern = Pattern.compile(componentName);
							for (int j = 0, max2 = apiComponents.length; j < max2; j++) {
								IApiComponent apiComponent = apiComponents[j];
								try {
									String componentId = apiComponent.getId();
									Matcher matcher = pattern.matcher(componentId);
									if (matcher.matches()) {
										scope.add(apiComponent);
									}
								} catch (CoreException e) {
									ApiPlugin.log(e);
								}
							}
						} catch (PatternSyntaxException e) {
							throw new BuildException(Messages.bind(
									Messages.comparison_invalidRegularExpression,
									componentName));
						}
					} else {
						IApiComponent apiComponent = currentBaseline.getApiComponent(componentName);
						if (apiComponent != null) {
							scope.add(apiComponent);
						}
					}
				}
			}
		} else {
			scope.add(currentBaseline);
		}
		try {
			delta = ApiComparator.compare(scope, referenceBaseline, this.visibilityModifiers);
		} finally {
			referenceBaseline.dispose();
			currentBaseline.dispose();
			StubApiComponent.disposeAllCaches();
			deleteBaseline(this.referenceBaselineLocation, referenceInstallDir);
			deleteBaseline(this.currentBaselineLocation, baselineInstallDir);
		}
		if (delta == null) {
			// an error occurred during the comparison
			throw new BuildException(Messages.errorInComparison);
		}
		// dump the report in the appropriate folder
		BufferedWriter writer = null;
		File outputDir = new File(this.reportLocation);
		if (!outputDir.exists()) {
			if (!outputDir.mkdirs()) {
				throw new BuildException(
					Messages.bind(
							Messages.errorCreatingParentReportFile,
							outputDir.getAbsolutePath()
					));
			}
		}
		File outputFile = new File(this.reportLocation, REPORT_XML_FILE_NAME);
		try {
			if (outputFile.exists()) {
				// delete the file
				// TODO we might want to customize it
				outputFile.delete();
			}
			writer = new BufferedWriter(new FileWriter(outputFile));
			ExcludeListDeltaVisitor visitor = new ExcludeListDeltaVisitor(this.excludeListLocation);
			delta.accept(visitor);
			writer.write(visitor.getXML());
			writer.flush();
			if (this.debug) {
				String potentialExcludeList = visitor.getPotentialExcludeList();
				if (potentialExcludeList.length() != 0) {
					System.out.println("Potential exclude list:"); //$NON-NLS-1$
					System.out.println(potentialExcludeList);
				}
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch(IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the location of the current product or baseline that you want to compare against
	 * the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the baseline to analyze
	 */
	public void setProfile(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}
	/**
	 * Set the location of the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.referenceBaselineLocation = baselineLocation;
	}
	/**
	 * Set the output location where the report will be generated.
	 * 
	 * <p>Once the task is completed, a report file called "compare.xml" is generated into this location.</p>
	 * 
	 * @param reportLocation the output location where the report will be generated
	 */
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
	/**
	 * Set the visibility to use for the comparison.
	 * 
	 * <p>The two expected values are: <code>"API"</code>, <code>"ALL"</code>.</p>
	 * 
	 * <p>If the given string doesn't match one of the two values, a build exception is thrown. If none is set,
	 * then the default visibility is "API".</p>
	 * 
	 * @param visibility the given visibility
	 * @throws BuildException if the given value is not "API" or "ALL".
	 */
	public void setVisibility(String value) {
		if (this.debug) {
			System.out.println("Visibility : " + value); //$NON-NLS-1$
		}
		if (VISIBILITY_ALL.equals(value)) {
			this.visibilityModifiers = VisibilityModifiers.ALL_VISIBILITIES;
		} else if (VISIBILITY_API.equals(value)) {
			this.visibilityModifiers = VisibilityModifiers.API;
		} else {
			throw new BuildException("The given value " + value + " is not equals to \"ALL \" or \"API\"."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Set the given components that needs to be compared against the baseline.
	 * 
	 * <p>This is set using a list of component names separated by commas. Each component name can be
	 * either an exact name, or a regular expression if it starts with <code>"R:"</code>.</p>
	 * 
	 * <p>Each component should be listed only once.</p>
	 * 
	 * <p>This is optional. If not set, the whole given baseline will be compared with the given reference baseline.</p>
	 *
	 * @param elements the given components
	 */
	public void setComponents(String componentsList) {
		this.componentsList = componentsList;
	}
	/**
	 * Set the exclude list location.
	 * 
	 * <p>The exclude list is used to know what bundles should excluded from the xml report
	 * generated by the task execution. Lines starting with '#' are ignored from
	 * the excluded element.</p>
	 * <p>The format of the exclude file looks like this:</p>
	 * <pre># 229688
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#getElements(Ljava/lang/Object;)[Ljava/lang/Object;
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListContentProvider#inputChanged(Lorg/eclipse/jface/viewers/Viewer;Ljava/lang/Object;Ljava/lang/Object;)V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#dispose()V
	 * org.eclipse.jface.databinding_1.2.0:org.eclipse.jface.databinding.viewers.ObservableListTreeContentProvider#getChildren(Ljava/lang/Object;)[Ljava/lang/Object;
	 * ...
	 * </pre>
	 * @param excludeListLocation the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
}