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
package com.puppetlabs.geppetto.validation.runner;

import com.puppetlabs.geppetto.forge.model.ModuleName;
import com.google.common.collect.Multimap;

/**
 * Provides access to any build result produced by "validation" (which should really be renamed to "build").
 * 
 */
public class BuildResult {

	private AllModulesState allModuleReferences;

	private RakefileInfo rakefileInfo;

	private boolean rubyServicesAvailable;

	private Multimap<ModuleName, MetadataInfo> moduleData;

	public BuildResult(boolean rubyAvailable) {
		this.rubyServicesAvailable = rubyAvailable;
	}

	public AllModulesState getAllModuleReferences() {
		return allModuleReferences;
	}

	public Multimap<ModuleName, MetadataInfo> getModuleData() {
		return moduleData;
	}

	/**
	 * Get information about all rakefiles and their tasks discovered in the result, or null if rakefile information was
	 * not requested or possible to compute for a particular request.
	 * 
	 * @return
	 */
	public RakefileInfo getRakefileInfo() {
		return rakefileInfo;
	}

	public boolean isRubyServicesAvailable() {
		return rubyServicesAvailable;
	}

	public void setAllModuleReferences(AllModulesState allReferences) {
		this.allModuleReferences = allReferences;
	}

	public void setModuleData(Multimap<ModuleName, MetadataInfo> moduleData) {
		this.moduleData = moduleData;
	}

	public void setRakefileInfo(RakefileInfo rakefileInfo) {
		this.rakefileInfo = rakefileInfo;
	}

	public void setRubyServicesAvailable(boolean flag) {
		rubyServicesAvailable = flag;
	}
}
