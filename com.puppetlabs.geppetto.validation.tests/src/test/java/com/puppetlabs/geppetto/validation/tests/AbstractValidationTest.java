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
package com.puppetlabs.geppetto.validation.tests;

import static org.junit.Assert.fail;

import java.io.File;

import org.eclipse.emf.common.util.URI;
import org.junit.Before;

import com.google.common.collect.Multimap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.client.GsonModule;
import com.puppetlabs.geppetto.forge.impl.ForgeModule;
import com.puppetlabs.geppetto.module.dsl.validation.DefaultModuleValidationAdvisor;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;
import com.puppetlabs.geppetto.ruby.RubyHelper;
import com.puppetlabs.geppetto.ruby.jrubyparser.JRubyServices;
import com.puppetlabs.geppetto.validation.ValidationOptions;
import com.puppetlabs.geppetto.validation.ValidationService;
import com.puppetlabs.geppetto.validation.impl.ValidationModule;
import com.puppetlabs.geppetto.validation.runner.AllModulesState;
import com.puppetlabs.geppetto.validation.runner.AllModulesState.Export;
import com.puppetlabs.geppetto.validation.runner.IEncodingProvider;
import com.puppetlabs.geppetto.validation.runner.PPDiagnosticsSetup;

public class AbstractValidationTest {
	protected static int countErrors(Diagnostic chain) {
		int count = 0;
		for(Diagnostic diag : chain)
			if(diag.getSeverity() >= Diagnostic.ERROR)
				++count;
		return count;
	}

	private Injector injector;

	protected final void assertContainsIssue(Diagnostic chain, String issue) {
		for(Diagnostic d : chain.getChildren())
			if(issue.equals(d.getIssue()))
				return;
		fail("Should contain issue: " + issue);

	}

	protected final void assertDoesNotContainIssue(Diagnostic chain, String issue) {
		for(Diagnostic d : chain.getChildren())
			if(issue.equals(d.getIssue()))
				fail("Should not contain issue: " + issue);
	}

	protected final void dumpErrors(Diagnostic chain) {
		System.err.println(errorsToString(chain));
	}

	protected final void dumpExports(AllModulesState exports) {
		Multimap<File, Export> exportmap = exports.getExportsPerModule();
		for(File f : exportmap.keySet()) {
			System.err.println("Exports from: " + f);
			for(Export e : exportmap.get(f)) {
				System.err.printf("    %s, %s inherits %s\n", e.getEClass().getName(), e.getName(), e.getParentName());
			}
		}
	}

	protected final void dumpExports(Iterable<Export> exports) {
		System.err.println("START DUMP");
		System.err.println("==========");
		for(Export e : exports) {
			System.err.printf("    %s, %s inherits %s\n", e.getEClass().getName(), e.getName(), e.getParentName());
		}
	}

	protected final String errorsToString(Diagnostic chain) {
		StringBuilder builder = new StringBuilder();
		for(Diagnostic d : chain) {
			builder.append("Diagnostic: ");
			// remove the "found in:" part as the order is not stable
			String msg = d.getMessage();
			int idx = msg.indexOf("found in:");
			if(idx != -1)
				msg = msg.substring(0, idx);
			builder.append(msg);
			builder.append("\n");
		}
		return builder.toString();
	}

	protected ValidationOptions getValidationOptions() {
		return getValidationOptions(PuppetTarget.getDefault().getComplianceLevel());
	}

	@SuppressWarnings("serial")
	protected ValidationOptions getValidationOptions(ComplianceLevel complianceLevel) {
		ValidationOptions options = new ValidationOptions();
		options.setPlatformURI(PuppetTarget.forComplianceLevel(complianceLevel, false).getPlatformURI());
		options.setEncodingProvider(new IEncodingProvider() {
			@Override
			public String getEncoding(URI file) {
				return "UTF-8";
			}
		});
		options.setModuleValidationAdvisor(new DefaultModuleValidationAdvisor() {
			@Override
			public ValidationPreference getDeprecatedKey() {
				return ValidationPreference.IGNORE;
			}

			@Override
			public ValidationPreference getMissingForgeRequiredFields() {
				return ValidationPreference.IGNORE;
			}

			@Override
			public ValidationPreference getModulefileExists() {
				return ValidationPreference.IGNORE;
			}
		});
		return options;
	}

	public ValidationService getValidationService() {
		return injector.getInstance(ValidationService.class);
	}

	@Before
	public void setUp() {
		RubyHelper.setRubyServicesFactory(JRubyServices.FACTORY);
		ValidationOptions options = getValidationOptions();
		new PPDiagnosticsSetup(options).createInjectorAndDoEMFRegistration();
		injector = Guice.createInjector(GsonModule.INSTANCE, new ForgeModule(), new ValidationModule());
	}
}
