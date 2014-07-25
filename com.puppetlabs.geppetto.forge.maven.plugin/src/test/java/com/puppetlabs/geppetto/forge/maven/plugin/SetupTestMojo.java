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
package com.puppetlabs.geppetto.forge.maven.plugin;

import static com.puppetlabs.geppetto.injectable.CommonModuleProvider.getCommonModule;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Module;
import com.puppetlabs.geppetto.forge.client.ForgeHttpModule;
import com.puppetlabs.geppetto.forge.client.GsonModule;
import com.puppetlabs.geppetto.forge.client.OAuthModule;
import com.puppetlabs.geppetto.forge.v2.ForgeAPI;
import com.puppetlabs.geppetto.forge.v2.service.ReleaseService;
import com.puppetlabs.geppetto.semver.Version;

public class SetupTestMojo extends AbstractForgeTestMojo {
	private static Module createCredentialsModule(Properties props, String login, String password) throws IOException {
		return new OAuthModule(
			props.getProperty("forge.oauth.clientID"), props.getProperty("forge.oauth.clientSecret"), login, password);
	}

	@Test
	public void dropTestModules() throws Exception {
		Properties props = AbstractForgeMojo.readForgeProperties();

		// Login using the primary login (bob)
		Module forgeModule = new ForgeHttpModule() {
			@Override
			protected String getBaseURL() {
				return System.getProperty("forge.base.url");
			}
		};

		String owner = System.getProperty("forge.login");
		ForgeAPI forge = new ForgeAPI(getCommonModule(), GsonModule.INSTANCE, createCredentialsModule(
			props, owner, System.getProperty("forge.password")), forgeModule);

		// Create the modules used in publishing tests
		ReleaseService releaseService = forge.createReleaseService();
		try {
			releaseService.delete(owner, "test_module_a", Version.create(1, 0, 0));
		}
		catch(Exception e) {
		}
		try {
			releaseService.delete(owner, "test_module_b", Version.create(1, 0, 0));
		}
		catch(Exception e) {
		}
		try {
			releaseService.delete(owner, "test_module_c", Version.create(1, 0, 0));
		}
		catch(Exception e) {
		}
		try {
			releaseService.delete(owner, "test_module_d", Version.create(1, 0, 0));
		}
		catch(Exception e) {
		}
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}
}
