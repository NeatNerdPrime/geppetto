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
package com.puppetlabs.geppetto.forge.v2.api.it;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.puppetlabs.geppetto.forge.api.it.ForgeAPITestBase;
import com.puppetlabs.geppetto.forge.v2.model.AnnotatedLink;
import com.puppetlabs.geppetto.forge.v2.model.Module;
import com.puppetlabs.geppetto.forge.v2.service.ListPreferences;
import com.puppetlabs.geppetto.forge.v2.service.ModuleService;

/**
 * @author thhal
 *
 */
public class ModuleTests extends ForgeAPITestBase {
	@Test
	public void testListModules() throws IOException {
		ModuleService service = getTestUserForge().createModuleService();
		List<Module> modules = service.search("puppetlabs", null);
		assertNotNull("Null module list", modules);
		assertFalse("Empty module list", modules.isEmpty());

		boolean someReleaseLinkTested = false;
		int moduleCount = modules.size();
		int max = 3;
		if(max > moduleCount)
			max = moduleCount;

		for(int idx = 0; idx < max; ++idx) {
			Module module = modules.get(idx);
			List<AnnotatedLink> releases = module.getReleases();
			assertNotNull("Null module releases list", releases);
			for(AnnotatedLink release : releases) {
				assertNotNull("Null module release slug", release.getSlug());
				someReleaseLinkTested = true;
			}
		}
		assertTrue("No release links found", someReleaseLinkTested);
	}

	@Test
	public void testListModulesSorted() throws IOException {
		ModuleService service = getTestUserForge().createModuleService();
		ListPreferences listPrefs = new ListPreferences();
		listPrefs.setLimit(4);
		listPrefs.setOffset(2);
		listPrefs.setSortBy("name");
		listPrefs.setSortOrder("descending");
		List<Module> modules = service.search(null, listPrefs);
		assertNotNull("Null module list", modules);
		assertFalse("Empty module list", modules.isEmpty());
	}
}
