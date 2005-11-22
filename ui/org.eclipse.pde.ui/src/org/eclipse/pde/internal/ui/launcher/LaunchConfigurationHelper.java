/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;

public class LaunchConfigurationHelper {
	
	public static void synchronizeManifests(ILaunchConfiguration config, File configDir) {
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime())
			return;
		try {
			String programArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, 
														""); //$NON-NLS-1$
			if (programArgs.indexOf("-clean") != -1) //$NON-NLS-1$
				return;
		} catch (CoreException e) {
		}
		File dir = new File(configDir, "org.eclipse.osgi/manifests"); //$NON-NLS-1$
		if (dir.exists() && dir.isDirectory()) {
			PDECore.getDefault().getJavaElementChangeListener().synchronizeManifests(dir);	
		}
	}

	public static File getConfigurationArea(ILaunchConfiguration config) {
		File dir = getConfigurationLocation(config);
		if (!dir.exists()) 
			dir.mkdirs();		
		return dir;		
	}
	
	public static File getConfigurationLocation(ILaunchConfiguration config) {
		File dir = new File(PDECore.getDefault().getStateLocation().toOSString(), config.getName());
		try {
			if (!config.getAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, true)) {
				String userPath = config.getAttribute(IPDELauncherConstants.CONFIG_LOCATION, (String)null);
				if (userPath != null) {
					userPath = getSubstitutedString(userPath);
					dir = new File(userPath);
				}
			}
		} catch (CoreException e) {
		}		
		return dir;		
	}

	private static String getSubstitutedString(String text) throws CoreException {
		if (text == null)
			return ""; //$NON-NLS-1$
		IStringVariableManager mgr = VariablesPlugin.getDefault().getStringVariableManager();
		return mgr.performStringSubstitution(text);
	}
	
	public static Properties createConfigIniFile(ILaunchConfiguration configuration, String productID, Map map, File directory) throws CoreException {
		Properties properties = null;
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true)) {
			properties = createNewPropertiesFile(productID, map);
		} else {
			String templateLoc = configuration.getAttribute(IPDELauncherConstants.CONFIG_TEMPLATE_LOCATION, (String)null);
			if (templateLoc != null) {
				properties = loadFromTemplate(getSubstitutedString(templateLoc));
			}
		}
		if (properties == null)
			properties = new Properties();
		setBundleLocations(map, properties);
		if (!directory.exists())
			directory.mkdirs();
		save(new File(directory, "config.ini"), properties); //$NON-NLS-1$
		return properties;
	}
	
	private static Properties createNewPropertiesFile(String productID, Map map) {
		Properties properties = new Properties();
		properties.setProperty("osgi.install.area", "file:" + ExternalModelManager.getEclipseHome().toOSString()); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("osgi.configuration.cascaded", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		properties.setProperty("osgi.framework", "org.eclipse.osgi"); //$NON-NLS-1$ //$NON-NLS-2$
		if (productID != null)
			addSplashLocation(properties, productID, map);
		boolean refactoredRuntime = PDECore.getDefault().getModelManager().findEntry("org.eclipse.equinox.common") != null; //$NON-NLS-1$
		if (map.containsKey("org.eclipse.update.configurator")) { //$NON-NLS-1$
			StringBuffer buffer = new StringBuffer();
			if (refactoredRuntime) {
				buffer.append("org.eclipse.equinox.common@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.jobs@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.equinox.registry@2:start,"); //$NON-NLS-1$
				buffer.append("org.eclipse.equinox.preferences,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.contenttype,"); //$NON-NLS-1$
				buffer.append("org.eclipse.core.runtime@3:start,org.eclipse.update.configurator@4:start"); //$NON-NLS-1$
			} else {
				buffer.append("org.eclipse.core.runtime@2:start,org.eclipse.update.configurator@3:start"); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles", buffer.toString());  //$NON-NLS-1$
		} else {
			StringBuffer buffer = new StringBuffer();
			Iterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				String id = iter.next().toString();
				if ("org.eclipse.osgi".equals(id)) //$NON-NLS-1$
					continue;
				buffer.append(id);
				if ("org.eclipse.equinox.common".equals(id) //$NON-NLS-1$
						|| "org.eclipse.core.jobs".equals(id) //$NON-NLS-1$
						|| "org.eclipse.equinox.registry".equals(id)) { //$NON-NLS-1$
					buffer.append("@2:start"); //$NON-NLS-1$
				} else if ("org.eclipse.core.runtime".equals(id)) { //$NON-NLS-1$
					if (refactoredRuntime) {
						buffer.append("@3:start"); //$NON-NLS-1$
					} else {
						buffer.append("@2:start"); //$NON-NLS-1$
					}
				}
				if (iter.hasNext())
					buffer.append(","); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles", buffer.toString()); //$NON-NLS-1$
		}
		if (refactoredRuntime)
			properties.setProperty("osgi.bundles.defaultStartLevel", "5"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			properties.setProperty("osgi.bundles.defaultStartLevel", "4"); //$NON-NLS-1$ //$NON-NLS-2$
		return properties;
	}
	
	private static Properties loadFromTemplate(String templateLoc) throws CoreException {
		Properties properties = new Properties();
		File templateFile = new File(templateLoc);
		if (templateFile.exists() && templateFile.isFile()) {
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(templateFile);
				properties.load(stream);
			} catch (Exception e) {
				String message = e.getMessage();
				if (message != null)
					throw new CoreException(
						new Status(
							IStatus.ERROR,
							PDEPlugin.getPluginId(),
							IStatus.ERROR,
							message,
							e));
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return properties;
	}

	private static void addSplashLocation(Properties properties, String productID, Map map)  {
		Properties targetConfig = TargetPlatform.getConfigIniProperties("configuration/config.ini"); //$NON-NLS-1$
		String targetProduct = targetConfig == null ? null : targetConfig.getProperty("eclipse.product"); //$NON-NLS-1$
		String targetSplash = targetConfig == null ? null : targetConfig.getProperty("osgi.splashPath"); //$NON-NLS-1$
		ArrayList locations = new ArrayList();
		if (!productID.equals(targetProduct) || targetSplash == null) {
			String plugin = getContributingPlugin(productID);
			locations.add(plugin);
			IPluginModelBase model = (IPluginModelBase)map.get(plugin);
			if (model != null) {
				BundleDescription desc = model.getBundleDescription();
				if (desc != null) {
					BundleDescription[] fragments = desc.getFragments();
					for (int i = 0; i < fragments.length; i++)
						locations.add(fragments[i].getSymbolicName());
				}
			}
		} else {
			StringTokenizer tok = new StringTokenizer(targetSplash, ","); //$NON-NLS-1$
			while (tok.hasMoreTokens())
				locations.add(tok.nextToken());			
		}
		
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < locations.size(); i++) {
			String location = (String)locations.get(i);
			if (location.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				location = location.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = TargetPlatform.getBundleURL(location, map);
			if (url == null)
				continue;
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(url);
		}
		if (buffer.length() > 0)
			properties.setProperty("osgi.splashPath", buffer.toString()); //$NON-NLS-1$
	}
	
	
	private static void setBundleLocations(Map map, Properties properties) {
		String framework = properties.getProperty("osgi.framework"); //$NON-NLS-1$
		if (framework != null) {
			if (framework.startsWith("platform:/base/plugins/")) { //$NON-NLS-1$
				framework.replaceFirst("platform:/base/plugins/", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String url = TargetPlatform.getBundleURL(framework, map);
			if (url != null)
				properties.setProperty("osgi.framework", url); //$NON-NLS-1$
		}
		
		String bundles = properties.getProperty("osgi.bundles"); //$NON-NLS-1$
		if (bundles != null) {
			StringBuffer buffer = new StringBuffer();
			StringTokenizer tokenizer = new StringTokenizer(bundles, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				String url = TargetPlatform.getBundleURL(token, map);
				int index = -1;
				if (url == null) {
					index = token.indexOf('@');
					if (index != -1) {
						url = TargetPlatform.getBundleURL(token.substring(0,index), map);
					}
					if (url == null) {
						index = token.indexOf(':');
						if (index != -1) {
							url = TargetPlatform.getBundleURL(token.substring(0,index), map);
						}
					}
				}
				if (url == null) {
					buffer.append(token);
				} else {
					buffer.append("reference:" + url); //$NON-NLS-1$
					if (index != -1)
						buffer.append(token.substring(index));
				}
				if (tokenizer.hasMoreTokens())
					buffer.append(","); //$NON-NLS-1$
			}
			properties.setProperty("osgi.bundles", buffer.toString()); //$NON-NLS-1$
		}
	}
	
	public static void save(File file, Properties properties) {
		try {
			FileOutputStream stream = new FileOutputStream(file);
			properties.store(stream, "Configuration File"); //$NON-NLS-1$
			stream.flush();
			stream.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public static String getContributingPlugin(String productID) {
		if (productID == null)
			return null;
		int index = productID.lastIndexOf('.');
		return index == -1 ? productID : productID.substring(0, index);
	}
	
	public static String getBootPath(IPluginModelBase bootModel) {
		try {
			IResource resource = bootModel.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					resource = project.findMember("boot.jar"); //$NON-NLS-1$
					if (resource != null)
						return "file:" + resource.getLocation().toOSString(); //$NON-NLS-1$
					IPath path = JavaCore.create(project).getOutputLocation();
					if (path != null) {
						IPath sourceBootPath =
							project.getParent().getLocation().append(path);
						return sourceBootPath.addTrailingSeparator().toOSString();
					}
				}
			} else {
				File bootJar = new File(bootModel.getInstallLocation(), "boot.jar"); //$NON-NLS-1$
				if (bootJar.exists())
					return "file:" + bootJar.getAbsolutePath(); //$NON-NLS-1$
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	public static String getPrimaryPlugin() {
		Properties properties = TargetPlatform.getConfigIniProperties("install.ini");		 //$NON-NLS-1$
		return properties == null ? null : properties.getProperty("feature.default.id");		 //$NON-NLS-1$
	}
	
	public static String getProductID(ILaunchConfiguration configuration) throws CoreException {
		String result = null;
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			result = configuration.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
		} else {
			// find the product associated with the application, and return its contributing plug-in
			String appID = configuration.getAttribute(IPDELauncherConstants.APPLICATION, getDefaultApplicationName());
			IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getPluginBase().getId();
				IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
				for (int j = 0; j < extensions.length; j++) {
					String point = extensions[j].getPoint();
					if (point != null && point.equals("org.eclipse.core.runtime.products")) {//$NON-NLS-1$
						IPluginObject[] children = extensions[j].getChildren();
						if (children.length != 1)
							continue;
						if (!"product".equals(children[0].getName())) //$NON-NLS-1$
							continue;
						if (appID.equals(((IPluginElement)children[0]).getAttribute("application").getValue())) { //$NON-NLS-1$
							result = id;
							break;
						}
					}
				}
			}
		}
		if (result != null)
			return result;
		
		Properties properties = TargetPlatform.getConfigIniProperties("configuration/config.ini");		 //$NON-NLS-1$
		return properties == null ? null : properties.getProperty("eclipse.product"); //$NON-NLS-1$
	}

	public static String getDefaultApplicationName() {
		if (!PDECore.getDefault().getModelManager().isOSGiRuntime())
			return "org.eclipse.ui.workbench"; //$NON-NLS-1$
		
		Properties properties = TargetPlatform.getConfigIniProperties("configuration/config.ini"); //$NON-NLS-1$
		String appName = (properties != null) ? properties.getProperty("eclipse.application") : null; //$NON-NLS-1$
		return (appName != null) ? appName : "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$
	}

}
