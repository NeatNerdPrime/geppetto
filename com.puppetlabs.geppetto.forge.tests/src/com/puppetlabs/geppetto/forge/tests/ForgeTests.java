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

import junit.framework.TestSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({
	// @fmtOff
	ModuleNameTest.class,
	ForgeUtilTest.class,
	ForgeTest.class,
	ForgeServiceTest.class,
	MetadataTest.class,
	ModuleUtilsTest.class,
	TypeTest.class,
	// @fmtOn
})
@RunWith(Suite.class)
public class ForgeTests extends TestSuite {
}
