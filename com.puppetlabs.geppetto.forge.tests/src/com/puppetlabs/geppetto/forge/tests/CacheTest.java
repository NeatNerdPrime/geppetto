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
package com.puppetlabs.geppetto.forge.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.puppetlabs.geppetto.forge.Cache;
import com.puppetlabs.geppetto.forge.model.ModuleName;
import com.puppetlabs.geppetto.semver.Version;

public class CacheTest extends AbstractForgeTest {

	static final String FILE_TO_TEST = "/system/releases/p/puppetlabs/puppetlabs-stdlib-2.3.1.tar.gz";

	private Cache fixture = null;

	@Before
	public void setUp() throws Exception {
		fixture = getCache();
	}

	@After
	public void tearDown() throws Exception {
		fixture = null;
	}

	@Test
	public void testClean() {
		try {
			File file = fixture.retrieve(ModuleName.fromString("puppetlabs-stdlib"), Version.fromString("2.3.1"));
			assertTrue("Retrieved file is not a file", file.isFile());
			fixture.clean();
			assertFalse("Clean did not remove the cached files", file.isFile());
		}
		catch(IOException e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testRetrieve__String() {
		try {
			File file = fixture.retrieve(ModuleName.fromString("puppetlabs-stdlib"), Version.fromString("2.3.1"));
			assertTrue("Retrieved file is not a file", file.isFile());
		}
		catch(IOException e) {
			fail(e.getMessage());
		}
	}
}
