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
package com.puppetlabs.geppetto.pp.dsl.ui.container;

import org.eclipse.xtext.resource.containers.IAllContainersState;
import org.eclipse.xtext.ui.containers.WorkspaceProjectsState;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * A Provider of PPWorkspaceProjectsState
 *
 */
public class PPWorkspaceProjectsStateProvider implements Provider<IAllContainersState> {

	@Inject
	private Provider<WorkspaceProjectsState> stateProvider;

	@Override
	public IAllContainersState get() {
		return stateProvider.get();
	}

}
