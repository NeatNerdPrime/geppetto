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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import com.google.inject.Inject;
import com.puppetlabs.geppetto.forge.model.VersionedName;
import com.puppetlabs.geppetto.forge.v2.service.ReleaseService;
import com.puppetlabs.geppetto.forge.v3.ForgeService;
import com.puppetlabs.geppetto.forge.v3.Releases;
import com.puppetlabs.geppetto.forge.v3.api.it.EndpointTests;
import com.puppetlabs.geppetto.forge.v3.model.Release;

/**
 * @author thhal
 *
 */
public class ReleaseTestDelete extends EndpointTests<Release, VersionedName> {
	@Inject
	private Releases service;

	@Override
	protected ForgeService<Release, VersionedName> getService() {
		return service;
	}

	@Test
	public void testDeleteRelease() throws IOException {
		ReleaseService v2Service = ForgeIT.getTestUserForge().createReleaseService();
		v2Service.delete(TEST_USER, TEST_MODULE, TEST_RELEASE_VERSION);
		try {
			getService().get(new VersionedName(TEST_USER, TEST_MODULE, TEST_RELEASE_VERSION));
			fail("Expected 404");
		}
		catch(HttpResponseException e) {
			assertEquals("Wrong response code", 404, e.getStatusCode());
		}
	}
}
