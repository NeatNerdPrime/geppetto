/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs - initial API and implementation
 */
package com.puppetlabs.geppetto.injectable.eclipse;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author thhal
 */
public class Activator implements BundleActivator {

	public static Activator getInstance() {
		Activator a = instance;
		if(a == null)
			throw new IllegalStateException("Bundle is not active");
		return a;
	}

	private static boolean renameCloudsmithContent(File oldFile, File newFile) throws IOException {
		StringBuilder bld = new StringBuilder();
		Reader reader = new FileReader(oldFile);
		try {
			char[] buf = new char[1024];
			int count;
			while((count = reader.read(buf)) > 0)
				bld.append(buf, 0, count);
		}
		finally {
			reader.close();
		}

		boolean changed = false;
		int nxt = bld.indexOf(ORG_CLOUDSMITH);
		while(nxt >= 0) {
			bld.replace(nxt, nxt + ORG_CLOUDSMITH.length(), COM_PUPPETLABS);
			nxt = bld.indexOf(ORG_CLOUDSMITH, nxt + ORG_CLOUDSMITH.length());
			changed = true;
		}
		nxt = bld.indexOf(WWW_CLOUDSMITH_ORG);
		while(nxt >= 0) {
			bld.replace(nxt, nxt + WWW_CLOUDSMITH_ORG.length(), WWW_PUPPETLABS_COM);
			nxt = bld.indexOf(WWW_CLOUDSMITH_ORG, nxt + WWW_CLOUDSMITH_ORG.length());
			changed = true;
		}
		Writer writer = new FileWriter(newFile);
		try {
			writer.write(bld.toString());
		}
		finally {
			writer.close();
		}
		return changed;
	}

	// This is a hack to preserve workspaces created with a Geppetto that used the 'org.cloudsmith.'
	// package and bundle naming. It ensures that the bundle states and workspace preferences are
	// changed accordingly.
	//
	private static void renameCloudsmithPrefs(Bundle bundle) {
		try {
			IPath wsRoot = Platform.getLocation();
			IPath bundleStateRoot = wsRoot.append(".metadata").append(".plugins");
			File[] bundleDirs = bundleStateRoot.toFile().listFiles();
			if(bundleDirs == null) {
				System.out.format("%s is not a directory\n", bundleStateRoot.toOSString());
				return;
			}

			for(File bundleDir : bundleDirs) {
				String name = bundleDir.getName();
				if(!name.startsWith(ORG_CLOUDSMITH))
					continue;

				String newName = COM_PUPPETLABS + name.substring(ORG_CLOUDSMITH.length());
				File newDir = new File(bundleDir.getParentFile(), newName);
				if(!newDir.exists()) {
					bundleDir.renameTo(newDir);
					System.out.format("Renamed %s to %s\n", bundleDir.getAbsolutePath(), newName);
				}
			}

			IPath settingsRoot = bundleStateRoot.append("org.eclipse.core.runtime").append(".settings");
			renameCloudsmithSettings(settingsRoot);

			IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
			boolean changed = false;
			for(IProject project : workspace.getProjects()) {
				IPath location = project.getLocation();
				if(".org_cloudsmith_geppetto_pptp_target".equals(project.getName())) {
					IPath destination = location.removeLastSegments(1).append(".com_puppetlabs_geppetto_pptp_target");
					File destDir = destination.toFile();
					if(!destDir.exists()) {
						project.move(Path.fromPortableString(destination.lastSegment()), true, new NullProgressMonitor());
						System.out.format("Renamed %s to %s\n", location.toOSString(), destination.lastSegment());
						for(File f : destDir.listFiles())
							if(f.getName().endsWith(".pptp"))
								renameCloudsmithContent(f, f);
						location = destination;
						changed = true;
					}
				}

				File projectFile = location.append(".project").toFile();
				if(projectFile.exists())
					if(renameCloudsmithContent(projectFile, projectFile))
						changed = true;

				settingsRoot = location.append(".settings");
				if(settingsRoot.toFile().isDirectory())
					if(renameCloudsmithSettings(settingsRoot))
						changed = true;
			}
			if(changed)
				workspace.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean renameCloudsmithSettings(IPath settingsRoot) throws IOException {
		File[] prefsFiles = settingsRoot.toFile().listFiles();
		if(prefsFiles == null) {
			System.out.format("%s is not a directory\n", settingsRoot.toOSString());
			return false;
		}

		boolean changed = false;
		for(File prefsFile : prefsFiles) {
			String name = prefsFile.getName();
			if(!name.startsWith(ORG_CLOUDSMITH))
				continue;

			String newName = COM_PUPPETLABS + name.substring(ORG_CLOUDSMITH.length());
			File newFile = new File(prefsFile.getParentFile(), newName);
			if(renameCloudsmithContent(prefsFile, newFile))
				System.out.format("Renamed %s to %s and altered its content%n", prefsFile.getAbsolutePath(), newName);
			else
				System.out.format("Renamed %s to %s%n", prefsFile.getAbsolutePath(), newName);
			prefsFile.delete();
			changed = true;
		}
		return changed;
	}

	private static Activator instance;

	private BundleContext context;

	private ServiceReference<IProxyService> proxyServiceReference;

	private IProxyService proxyService;

	private static final String ORG_CLOUDSMITH = "org.cloudsmith.";

	private static final String WWW_CLOUDSMITH_ORG = "www.cloudsmith.org";

	private static final String COM_PUPPETLABS = "com.puppetlabs.";

	private static final String WWW_PUPPETLABS_COM = "www.puppetlabs.com";

	public synchronized IProxyService getProxyService() {
		if(proxyServiceReference == null) {
			proxyServiceReference = context.getServiceReference(IProxyService.class);
			proxyService = context.getService(proxyServiceReference);
		}
		return proxyService;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		renameCloudsmithPrefs(context.getBundle());

		this.context = context;
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		instance = null;
		if(proxyServiceReference != null) {
			context.ungetService(proxyServiceReference);
			proxyServiceReference = null;
			proxyService = null;
		}
	}
}
