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

import static com.google.inject.name.Names.named;
import static com.puppetlabs.geppetto.injectable.CommonModuleProvider.getCommonModule;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.puppetlabs.geppetto.common.util.BundleAccess;
import com.puppetlabs.geppetto.forge.Cache;
import com.puppetlabs.geppetto.forge.Forge;
import com.puppetlabs.geppetto.forge.ForgeService;
import com.puppetlabs.geppetto.forge.client.ForgeHttpModule;
import com.puppetlabs.geppetto.forge.client.GsonModule;
import com.puppetlabs.geppetto.forge.impl.ForgeModule;
import com.puppetlabs.geppetto.forge.impl.ForgeServiceModule;

public class AbstractForgeTest {
	public static void delete(File fileOrDir) throws IOException {
		File[] children = fileOrDir.listFiles();
		if(children != null)
			for(File child : children)
				delete(child);
		if(!fileOrDir.delete() && fileOrDir.exists())
			throw new IOException("Unable to delete " + fileOrDir);
	}

	// private static String TEST_FORGE_URI = "http://localhost:4567/";

	private static File getBasedir() {
		if(basedir == null) {
			String basedirProp = System.getProperty("basedir");
			if(basedirProp == null) {
				try {
					File testData = getBundleAccess().getFileFromClassBundle(AbstractForgeTest.class, "testData");
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

	public static BundleAccess getBundleAccess() {
		return commonInjector.getInstance(BundleAccess.class);
	}

	public static Cache getCache() {
		return getInjector().getInstance(Cache.class);
	}

	public static ForgeService getForge() {
		return getInjector().getInstance(ForgeService.class);
	}

	public static Forge getForgeUtil() {
		return getInjector().getInstance(Forge.class);
	}

	public static Gson getGson() {
		return getInjector().getInstance(Gson.class);
	}

	private synchronized static Injector getInjector() {
		if(injector == null) {
			Module testBindings = new AbstractModule() {
				@Override
				protected void configure() {
					try {
						bind(File.class).annotatedWith(named(Forge.CACHE_LOCATION)).toInstance(
							getTestOutputFolder("cachefolder", true));
					}
					catch(IOException e) {
						fail(e.getMessage());
					}
				}
			};
			try {
				injector = commonInjector.createChildInjector(GsonModule.INSTANCE, new ForgeHttpModule() {
					@Override
					protected String getBaseURL() {
						return System.getProperty("testForgeServiceURL", TEST_FORGE_URI);
					}
				}, new ForgeServiceModule(), new ForgeModule(), testBindings);
			}
			catch(Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
		return injector;
	}

	public static File getTestData(String path) throws IOException {
		return new File(new File(getBasedir(), "testData"), path);
	}

	public static File getTestOutputFolder(String name, boolean purge) throws IOException {
		File testFolder = new File(new File(new File(getBasedir(), "target"), "testOutput"), name);
		testFolder.mkdirs();
		if(purge) {
			// Ensure that the folder is empty
			for(File file : testFolder.listFiles())
				delete(file);
		}
		return testFolder;
	}

	private static String TEST_FORGE_URI = "https://forgestagingapi.puppetlabs.com/";

	private static Injector injector;

	private static Injector commonInjector = Guice.createInjector(getCommonModule());

	private static File basedir;
}
