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
package com.puppetlabs.geppetto.pp.dsl.ui.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.ui.XtextProjectHelper;

import com.puppetlabs.geppetto.pp.dsl.ui.PPUiConstants;

public class ToggleNatureAction extends AbstractHandler {
	private static class ToggleJob extends Job {
		final private List<IProject> projectsToToggle;

		public ToggleJob(List<IProject> projectsToToggle) {
			super("Building Puppet Projects");
			this.projectsToToggle = projectsToToggle;
			setPriority(Job.BUILD);
		}

		/**
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				final IWorkspace workspace = ResourcesPlugin.getWorkspace();

				// Run toggle followed by a full build as an atomic workspace operation so that things
				// are done in the right order
				workspace.run(new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						for(IProject projectToToggle : projectsToToggle)
							toggleNature(projectToToggle);
						workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					}
				}, monitor);
				return Status.OK_STATUS;
			}
			catch(CoreException e) {
				return e.getStatus();
			}
		}
	}

	private static void addNature(IProject project) {
		// Also add XtextNature if not already set
		boolean addXtextNature = !XtextProjectHelper.hasNature(project);

		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			final int newCount = 1 + (addXtextNature
					? 1
					: 0);
			// Add the nature
			String[] newNatures = new String[natures.length + newCount];
			System.arraycopy(natures, 0, newNatures, newCount, natures.length);
			newNatures[0] = PPUiConstants.PUPPET_NATURE_ID;
			if(addXtextNature)
				newNatures[1] = XtextProjectHelper.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
		catch(CoreException e) {
			log.error("Failed adding Puppet Project Nature", e);
		}
	}

	protected static boolean hasNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();

			String[] natures = description.getNatureIds();

			for(int i = 0; i < natures.length; ++i)
				if(PPUiConstants.PUPPET_NATURE_ID.equals(natures[i]))
					return true;
		}
		catch(CoreException e) {
			log.error("Error while trying to obtain nature of project", e);
		}
		return false;
	}

	private static void removeNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			for(int i = 0; i < natures.length; ++i) {
				if(PPUiConstants.PUPPET_NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					return;
				}
			}
		}
		catch(CoreException e) {
			log.error("Failed removing Puppet Project Nature", e);
		}
	}

	protected static void toggleNature(IProject project) {
		if(hasNature(project))
			removeNature(project);
		else
			addNature(project);
	}

	private final static Logger log = Logger.getLogger(ToggleNatureAction.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if(selection instanceof IStructuredSelection) {
			List<IProject> projectsToToggle = new ArrayList<IProject>();
			for(Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if(element instanceof IProject) {
					project = (IProject) element;
				}
				else if(element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}
				if(project != null) {
					projectsToToggle.add(project);
				}
			}
			if(!projectsToToggle.isEmpty())
				new ToggleJob(projectsToToggle).schedule();
		}
		return null;
	}
}
