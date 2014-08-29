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

import static com.google.inject.Guice.createInjector;
import static com.puppetlabs.geppetto.injectable.CommonModuleProvider.getCommonModule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;

import com.puppetlabs.geppetto.common.util.BundleAccess;

public class TestDataProvider {

	/**
	 * Return the project root so that we can get testData in a way that works for both
	 * PDE and Maven test launchers
	 *
	 * @return absolute path of the project.
	 */
	private static File getBasedir() {
		if(basedir == null) {
			String basedirProp = System.getProperty("basedir");
			if(basedirProp == null) {
				try {
					File testData = createInjector(getCommonModule()).getInstance(BundleAccess.class).getFileFromClassBundle(
						TestDataProvider.class, "testData");
					if(testData == null || !testData.isDirectory())
						fail("Unable to determine basedir");
					basedir = testData.getParentFile();
				}
				catch(IOException e) {
					fail(e.getMessage());
				}
			}
			else
				basedir = new File(basedirProp);
		}
		return basedir;
	}

	public static byte[] getTestData(IPath testBundleRelativePath) {
		File f = getTestFile(testBundleRelativePath);
		byte[] buf = new byte[(int) f.length()];
		try {
			FileInputStream fi = new FileInputStream(f);
			int bytesRead = fi.read(buf);
			fi.close();
			assertEquals("Unable to read full content of file" + f.getAbsolutePath(), buf.length, bytesRead);
		}
		catch(IOException e) {
			fail(e.getMessage());
		}
		return buf;
	}

	public static File getTestFile(IPath testBundleRelativePath) {
		return new File(getBasedir(), testBundleRelativePath.toOSString());
	}

	public static File getTestOutputDir() {
		File testOutputDir = new File(getBasedir(), "target/testOutput");
		testOutputDir.mkdirs();
		return testOutputDir;
	}

	private static File basedir;
}
