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
package com.puppetlabs.geppetto.m2e;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;

public class ForgeBuildParticipant extends MojoExecutionBuildParticipant {

	public ForgeBuildParticipant(MojoExecution execution, boolean runOnIncremental) {
		super(execution, runOnIncremental);
	}

	public ForgeBuildParticipant(MojoExecution execution, boolean runOnIncremental, boolean runOnConfiguration) {
		super(execution, runOnIncremental, runOnConfiguration);
	}
}
