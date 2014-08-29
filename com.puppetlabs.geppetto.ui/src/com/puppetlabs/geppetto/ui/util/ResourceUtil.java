/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Puppet Labs
 */
package com.puppetlabs.geppetto.ui.util;

import static com.puppetlabs.geppetto.forge.Forge.METADATA_JSON_NAME;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.xtext.ui.XtextProjectHelper;

import com.puppetlabs.geppetto.pp.dsl.ui.PPUiConstants;
import com.puppetlabs.geppetto.ui.UIPlugin;

public class ResourceUtil {

	public static IProject createProject(IPath projectContainerPath, URI projectLocationURI, List<IProject> referencedProjects,
			IProgressMonitor progressMonitor) {
		String projectName = projectContainerPath.segment(0);
		IProject project = null;

		try {
			progressMonitor.beginTask("", 10); //$NON-NLS-1$
			progressMonitor.subTask(UIPlugin.getLocalString("_UI_CreatingPuppetProject_message", //$NON-NLS-1$
				new Object[] { projectName, projectLocationURI != null
					? projectLocationURI.toString()
					: projectName }));
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			project = workspace.getRoot().getProject(projectName);
			URI defaultLocation = URI.createFileURI(workspace.getRoot().getLocation().append(projectName).toOSString());

			if(!project.exists()) {
				URI location = projectLocationURI;

				if(location == null) {
					location = defaultLocation;
				}

				location = location.appendSegment(".project"); //$NON-NLS-1$
				File projectFile = new File(location.toFileString());

				if(projectFile.exists()) {
					projectFile.renameTo(new File(location.toString() + ".old")); //$NON-NLS-1$
				}
			}

			IProjectDescription projectDescription = null;

			if(project.exists())
				projectDescription = project.getDescription();
			else {
				projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);

				if(!(projectLocationURI == null || defaultLocation.equals(projectLocationURI))) {
					projectDescription.setLocationURI(new java.net.URI(projectLocationURI.toString()));
				}

				project.create(projectDescription, new SubProgressMonitor(progressMonitor, 1));
			}
			project.open(new SubProgressMonitor(progressMonitor, 1));

			if(!referencedProjects.isEmpty())
				projectDescription.setReferencedProjects(referencedProjects.toArray(new IProject[referencedProjects.size()]));

			String[] oldNatureIds = projectDescription.getNatureIds();
			String[] newNatureIds = oldNatureIds;

			if(oldNatureIds == null || oldNatureIds.length == 0)
				newNatureIds = new String[] { PPUiConstants.PUPPET_NATURE_ID, XtextProjectHelper.NATURE_ID };
			else {
				boolean missingXtextNature = true;
				boolean missingPuppetNature = true;
				int missingCount = 2;
				for(String natureId : oldNatureIds) {
					if(PPUiConstants.PUPPET_NATURE_ID.equals(natureId)) {
						missingPuppetNature = false;
						--missingCount;
					}
					if(XtextProjectHelper.NATURE_ID.equals(natureId)) {
						missingXtextNature = false;
						--missingCount;
					}
				}
				if(missingCount > 0) {
					newNatureIds = new String[oldNatureIds.length + missingCount];
					System.arraycopy(oldNatureIds, 0, newNatureIds, missingCount, oldNatureIds.length);
					int addAt = 0;
					if(missingPuppetNature)
						newNatureIds[addAt++] = PPUiConstants.PUPPET_NATURE_ID;
					if(missingXtextNature)
						newNatureIds[addAt++] = XtextProjectHelper.NATURE_ID;
				}
			}
			if(!(oldNatureIds == newNatureIds && referencedProjects.isEmpty())) {
				projectDescription.setNatureIds(newNatureIds);
				project.setDescription(projectDescription, new SubProgressMonitor(progressMonitor, 1));
			}
		}
		catch(Exception exception) {
			UIPlugin.logException("Unable to create project", exception);
		}
		finally {
			progressMonitor.done();
		}

		return project;
	}

	public static IFile getFile(IPath path) {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
	}

	protected static boolean isManifest(IFile file) {
		return file.getFileExtension().equals("pp"); //$NON-NLS-1$
	}

	protected static boolean isMetadata(IFile file) {
		return METADATA_JSON_NAME.equals(file.getName());
	}

	public static void openEditor(IFile file) throws PartInitException {
		openEditor(file, isMetadata(file)
			? "com.puppetlabs.geppetto.module.dsl.Module" //$NON-NLS-1$
			: "com.puppetlabs.geppetto.pp.dsl.Puppet"); //$NON-NLS-1$
	}

	public static void openEditor(IFile file, String editorId) throws PartInitException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
		IEditorDescriptor defaultEditor = workbench.getEditorRegistry().getDefaultEditor(file.getFullPath().toString());
		page.openEditor(new FileEditorInput(file), defaultEditor == null
			? editorId
			: defaultEditor.getId());
	}

	public static void selectFile(IResource file) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		final IWorkbenchPart activePart = page.getActivePart();

		if(activePart instanceof ISetSelectionTarget) {
			final ISelection targetSelection = new StructuredSelection(file);
			window.getShell().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					((ISetSelectionTarget) activePart).selectReveal(targetSelection);
				}
			});
		}
	}

}
