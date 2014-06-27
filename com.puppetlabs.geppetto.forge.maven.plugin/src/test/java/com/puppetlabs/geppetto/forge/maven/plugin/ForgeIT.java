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

import java.io.File;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.puppetlabs.geppetto.common.os.FileUtils;

// @fmtOff
@SuiteClasses({
	SetupTestMojo.class,
	ValidateTestMojo.class,
	PublishTestMojo.class,
	RepublishTestMojo.class,
	ValidateTest2Mojo.class,
})
// @fmtOn
@RunWith(Suite.class)
public class ForgeIT {
	@BeforeClass
	public static void init() {
		FileUtils.rmR(new File(TEST_POM_DIR, "target"));
	}

	static final File TEST_POM_DIR = new File(
		new File(System.getProperty("basedir", ".")), "target/test-projects/publisher");
}
