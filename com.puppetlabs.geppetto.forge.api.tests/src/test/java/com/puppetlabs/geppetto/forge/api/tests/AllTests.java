package com.puppetlabs.geppetto.forge.api.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * All Puppet Tests.
 */
@SuiteClasses({
// @fmtOff
	DependencyTest.class
	// @fmtOn
})
@RunWith(Suite.class)
public class AllTests {
}
