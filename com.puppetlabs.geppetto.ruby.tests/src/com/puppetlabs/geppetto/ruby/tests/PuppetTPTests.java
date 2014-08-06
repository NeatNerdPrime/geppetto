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
package com.puppetlabs.geppetto.ruby.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;

import com.puppetlabs.geppetto.common.os.OsUtil;
import com.puppetlabs.geppetto.common.os.StreamUtil;
import com.puppetlabs.geppetto.forge.util.TarUtils;
import com.puppetlabs.geppetto.pp.facter.Facter.Facter1_6;
import com.puppetlabs.geppetto.pp.pptp.AbstractType;
import com.puppetlabs.geppetto.pp.pptp.Function;
import com.puppetlabs.geppetto.pp.pptp.Parameter;
import com.puppetlabs.geppetto.pp.pptp.Property;
import com.puppetlabs.geppetto.pp.pptp.TargetEntry;
import com.puppetlabs.geppetto.pp.pptp.Type;
import com.puppetlabs.geppetto.pp.pptp.TypeFragment;
import com.puppetlabs.geppetto.ruby.RubyHelper;

public class PuppetTPTests {

	private static final Logger log = Logger.getLogger(PuppetTPTests.class);

	private static final String PUPPET_DOWNLOADS = "http://downloads.puppetlabs.com/";

	private File download(String type, String tgzFileName) throws Exception {
		File downloadedFile = new File(getDownloadsDir(), tgzFileName);
		if(downloadedFile.exists())
			return downloadedFile;

		URL ascURL = new URL(PUPPET_DOWNLOADS + type + '/' + tgzFileName);
		InputStream input = ascURL.openStream();
		try {
			OutputStream output = new FileOutputStream(downloadedFile);
			StreamUtil.copy(input, output);
			output.close();
		}
		catch(Exception e) {
			downloadedFile.delete();
			throw e;
		}
		finally {
			input.close();
		}
		return downloadedFile;
	}

	/* uncomment and modify path to test load of puppet distribution and creating an xml version */

	private File getDownloadsDir() {
		File downloads = new File(TestDataProvider.getBasedir(), "downloads");
		if(!(downloads.isDirectory() || downloads.mkdirs()))
			fail("Unable to create directory: " + downloads.getAbsolutePath());
		return downloads;
	}

	private Function getFunction(String name, TargetEntry target) {
		for(Function f : target.getFunctions())
			if(name.equals(f.getName()))
				return f;
		return null;
	}

	private Parameter getParameter(String name, Type type) {
		for(Parameter p : type.getParameters())
			if(name.equals(p.getName()))
				return p;
		return null;
	}

	private Property getProperty(String name, AbstractType type) {
		for(Property p : type.getProperties())
			if(name.equals(p.getName()))
				return p;
		return null;
	}

	private File getPuppetDistribution(String version) throws Exception {
		return getUnpackedDownload(getDownloadsDir(), "puppet", version);
	}

	private File getPuppetPlugins(String version) throws Exception {
		String pluginsName = "plugins-" + version;
		File downloads = new File(getDownloadsDir(), pluginsName);
		return getUnpackedDownload(downloads, "hiera", "1.3.0");
	}

	private File getUnpackedDownload(File destDir, String type, String version) throws Exception {
		String distroName = type + '-' + version;
		File distroDir = new File(destDir, distroName);
		if(distroDir.isDirectory())
			return distroDir;

		InputStream input = new GZIPInputStream(new FileInputStream(download(type, distroName + ".tar.gz")));
		try {
			TarUtils.unpack(input, destDir, false, null);
			input.close();
			return distroDir;
		}
		catch(Exception e) {
			OsUtil.deleteRecursive(distroDir);
			throw e;
		}
		finally {
			input.close();
		}
	}

	// Puppet PE 2.0 unzipped is not a full distribution - has no source to scan
	// public void testLoad_PE_2_0() throws Exception {
	// performLoad(new File("/Users/henrik/PuppetDistributions/puppet-enterprise-2.0.0-el-4-i386/lib/puppet"), //
	// null, //
	// new File("puppet_enterprise-2.0.0.pptp"));
	// }

	private void performLoad(File distroDir, File pluginsDir, File tptpFile) throws Exception {
		RubyHelper helper = new RubyHelper();
		helper.setUp();
		try {
			TargetEntry target = helper.loadDistroTarget(distroDir);

			// Load the variables in the settings:: namespace
			helper.loadSettings(target);

			// Load the default meta variables (available as local in every scope).
			helper.loadMetaVariables(target);
			helper.loadPuppetVariables(target);

			for(Type t : target.getTypes())
				log.info("Found t: " + t.getName());
			for(Function f : target.getFunctions())
				log.info("Found f: " + f.getName());

			// Load (optional) any plugins
			List<TargetEntry> plugins = helper.loadPluginsTarget(pluginsDir);

			// Save the TargetEntry as a loadable resource
			ResourceSet resourceSet = new ResourceSetImpl();
			URI fileURI = URI.createFileURI(tptpFile.getAbsolutePath());
			Resource targetResource = resourceSet.createResource(fileURI);

			// Add all (optional) plugins
			targetResource.getContents().add(target);
			for(TargetEntry entry : plugins)
				targetResource.getContents().add(entry);
			targetResource.save(null);
			log.info("Target saved to: " + fileURI.toString());

		}
		finally {
			helper.tearDown();
		}

	}

	private void performLoad(String version) throws Exception {
		performLoad(version, version);
	}

	private void performLoad(String version, String pptpVersion) throws Exception {
		performLoad(new File(getPuppetDistribution(version), "lib/puppet"), //
			getPuppetPlugins(version), //
			new File(TestDataProvider.getTestOutputDir(), "puppet-" + pptpVersion + ".pptp"));
	}

	/**
	 * This is a really odd place to do this, but since the other generators of pptp modesl
	 * are here...
	 *
	 * @throws Exception
	 */
	@Test
	public void testLoad_Facter1_6() throws Exception {
		File pptpFile = new File(TestDataProvider.getTestOutputDir(), "facter-1.6.pptp");
		Facter1_6 facter = new Facter1_6();

		// Save the TargetEntry as a loadable resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(pptpFile.getAbsolutePath());
		Resource targetResource = resourceSet.createResource(fileURI);

		// Add all (optional) plugins
		targetResource.getContents().add(facter.asPPTP());
		targetResource.save(null);
		log.info("Target saved to: " + fileURI.toString());

	}

	@Test
	public void testLoad_Facter1_7() throws Exception {
		File pptpFile = new File(TestDataProvider.getTestOutputDir(), "facter-1.7.pptp");
		Facter1_6 facter = new Facter1_6();

		// Save the TargetEntry as a loadable resource
		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(pptpFile.getAbsolutePath());
		Resource targetResource = resourceSet.createResource(fileURI);

		// Add all (optional) plugins
		targetResource.getContents().add(facter.asPPTP());
		targetResource.save(null);
		log.info("Target saved to: " + fileURI.toString());

	}

	@Test
	public void testLoad2_6_18() throws Exception {
		performLoad("2.6.18");
	}

	@Test
	public void testLoad2_7_25() throws Exception {
		performLoad("2.7.25");
	}

	@Test
	public void testLoad3_0_2() throws Exception {
		performLoad("3.0.2");
	}

	@Test
	public void testLoad3_1_1() throws Exception {
		performLoad("3.1.1");
	}

	@Test
	public void testLoad3_2_4() throws Exception {
		performLoad("3.2.4");
	}

	@Test
	public void testLoad3_3_2() throws Exception {
		performLoad("3.3.2");
	}

	@Test
	public void testLoad3_4_2() throws Exception {
		performLoad("3.4.2");
	}

	@Test
	public void testLoad3_5_1() throws Exception {
		performLoad("3.5.1");
	}

	@Test
	public void testLoad3_6_2() throws Exception {
		performLoad("3.6.2");
	}

	@Test
	public void testLoadEMFTP() throws Exception {
		File pptpFile = TestDataProvider.getTestFile(new Path("testData/pptp/puppet-2.6.4_0.pptp"));

		ResourceSet resourceSet = new ResourceSetImpl();
		URI fileURI = URI.createFileURI(pptpFile.getAbsolutePath());
		Resource targetResource = resourceSet.getResource(fileURI, true);
		TargetEntry target = (TargetEntry) targetResource.getContents().get(0);
		assertEquals("Should have found 46 types", 46, target.getTypes().size());
		assertEquals("Should have found 29 functions", 29, target.getFunctions().size());

		pptpFile = TestDataProvider.getTestFile(new Path("testData/pptp/puppet-2.6.4_0.pptp"));

		resourceSet = new ResourceSetImpl();
		fileURI = URI.createFileURI(pptpFile.getAbsolutePath());
		targetResource = resourceSet.getResource(fileURI, true);
		target = (TargetEntry) targetResource.getContents().get(0);
		assertEquals("Should have found 46 types", 46, target.getTypes().size());
		assertEquals("Should have found 29 functions", 29, target.getFunctions().size());
	}

	@Test
	public void testLoadMockDistro() throws Exception {
		File distroDir = TestDataProvider.getTestFile(new Path("testData/mock-puppet-distro/puppet-2.6.2_0/lib/puppet"));
		RubyHelper helper = new RubyHelper();
		helper.setUp();
		try {
			TargetEntry target = helper.loadDistroTarget(distroDir);

			// check the target itself
			assertNotNull("Should have resultet in a TargetEntry", target);
			assertEquals("Should have defined description", "Puppet Distribution", target.getDescription());
			assertEquals("Should have defined name", "puppet", target.getLabel());
			assertEquals("Should have defined version", "2.6.2_0", target.getVersion());

			// should have found one type "mocktype"
			assertEquals("Should have found one type", 1, target.getTypes().size());
			Type type = target.getTypes().get(0);
			assertEquals("Should have found 'mocktype'", "mocktype", type.getName());
			assertEquals("Should have found documentation", "<p>This is a mock type</p>", type.getDocumentation());

			assertEquals("Should have one property", 1, type.getProperties().size());
			{
				Property prop = getProperty("prop1", type);
				assertNotNull("Should have a property 'prop1", prop);
				assertEquals("Should have defined documentation", "<p>This is property1</p>", prop.getDocumentation());
			}
			{
				assertEquals("Should have one parameter", 1, type.getParameters().size());
				Parameter param = getParameter("param1", type);
				assertNotNull("Should have a parameter 'param1", param);
				assertEquals("Should have defined documentation", "<p>This is parameter1</p>", param.getDocumentation());
			}

			// There should be two type fragments, with a contribution each
			List<TypeFragment> typeFragments = target.getTypeFragments();
			assertEquals("Should have found two fragments", 2, typeFragments.size());

			TypeFragment fragment1 = typeFragments.get(0);
			TypeFragment fragment2 = typeFragments.get(1);
			boolean fragment1HasExtra1 = getProperty("extra1", fragment1) != null;
			{
				Property prop = getProperty("extra1", fragment1HasExtra1
						? fragment1
						: fragment2);
				assertNotNull("Should have a property 'extra1", prop);
				assertEquals(
					"Should have defined documentation", "<p>An extra property called extra1</p>",
					prop.getDocumentation());
			}
			{
				Property prop = getProperty("extra2", fragment1HasExtra1
						? fragment2
						: fragment1);
				assertNotNull("Should have a property 'extra2", prop);
				assertEquals(
					"Should have defined documentation", "<p>An extra property called extra2</p>",
					prop.getDocumentation());
			}

			// should have found two functions "echotest" and "echotest2"
			// and the log functions (8)
			assertEquals("Should have found two functions", 10, target.getFunctions().size());
			{
				Function f = getFunction("echotest", target);
				assertNotNull("Should have found function 'echotest'", f);
				assertTrue("echotest should be an rValue", f.isRValue());
			}
			{
				Function f = getFunction("echotest2", target);
				assertNotNull("Should have found function 'echotest2'", f);
				assertFalse("echotest2 should not be an rValue", f.isRValue());
			}

		}
		finally {
			helper.tearDown();
		}
	}

	// NOTE: On mac, macports changed format to a tgz file - can not load this (test has played out its role).

	/*
	 * @Test
	 * public void testLoadRealTP() throws Exception {
	 * File distroDir = new File(
	 * "/opt/local/var/macports/software/puppet/2.6.4_0/opt/local/lib/ruby/site_ruby/1.8/puppet/");
	 * RubyHelper helper = new RubyHelper();
	 * helper.setUp();
	 * try {
	 * TargetEntry target = helper.loadDistroTarget(distroDir);
	 * for(Type t : target.getTypes())
	 * System.err.println("Found t: " + t.getName());
	 * assertEquals("Should have found 46 types", 46, target.getTypes().size());
	 * for(Function f : target.getFunctions())
	 * System.err.println("Found f: " + f.getName());
	 * assertEquals("Should have found 29 functions", 29, target.getFunctions().size());
	 *
	 * // Save the TargetEntry as a loadable resource
	 * ResourceSet resourceSet = new ResourceSetImpl();
	 * URI fileURI = URI.createFileURI(new File("puppet-2.6.4_0.pptp").getAbsolutePath());
	 * Resource targetResource = resourceSet.createResource(fileURI);
	 * targetResource.getContents().add(target);
	 * targetResource.save(null);
	 * System.err.println("Target saved to: " + fileURI.toString());
	 *
	 * }
	 * finally {
	 * helper.tearDown();
	 * }
	 *
	 * }
	 */
}
