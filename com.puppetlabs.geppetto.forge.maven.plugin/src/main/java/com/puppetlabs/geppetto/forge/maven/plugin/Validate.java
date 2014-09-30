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

import static com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference.IGNORE;
import static com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference.WARNING;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.FileDiagnostic;
import com.puppetlabs.geppetto.forge.Forge;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.module.dsl.validation.IModuleValidationAdvisor;
import com.puppetlabs.geppetto.pp.dsl.target.PuppetTarget;
import com.puppetlabs.geppetto.pp.dsl.validation.IPotentialProblemsAdvisor;
import com.puppetlabs.geppetto.pp.dsl.validation.IValidationAdvisor.ComplianceLevel;
import com.puppetlabs.geppetto.pp.dsl.validation.ValidationPreference;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner.Issue;
import com.puppetlabs.geppetto.puppetlint.PuppetLintService;
import com.puppetlabs.geppetto.ruby.RubyHelper;
import com.puppetlabs.geppetto.ruby.jrubyparser.JRubyServices;
import com.puppetlabs.geppetto.validation.FileType;
import com.puppetlabs.geppetto.validation.ValidationOptions;
import com.puppetlabs.geppetto.validation.ValidationService;
import com.puppetlabs.geppetto.validation.runner.IEncodingProvider;
import com.puppetlabs.geppetto.validation.runner.PPDiagnosticsSetup;

/**
 * The <tt>validate</tt> goal can perform a very elaborate analyzis of the module using the Geppetto platform. It is
 * also capable of running the <tt>puppet-lint</tt> gem.
 */
@Mojo(name = "validate", requiresProject = false, defaultPhase = LifecyclePhase.COMPILE)
public class Validate extends AbstractForgeServiceMojo {
	private static int getSeverity(Issue issue) {
		switch(issue.getSeverity()) {
			case ERROR:
				return Diagnostic.ERROR;
			default:
				return Diagnostic.WARNING;
		}
	}

	/**
	 * Location of the forge cache. Defaults to ${user.home}/.puppet/var/puppet-module/cache/&lt;MD5 hash of service
	 * URL&gt;
	 */
	@Parameter(property = "forge.cache.location")
	private String cacheLocation;

	/**
	 * Set to <tt>true</tt> to enable validation using puppet-lint
	 */
	@Parameter(property = "forge.validation.enablePuppetLint", defaultValue = "false")
	private boolean enablePuppetLintValidation = false;

	/**
	 * An advisor to validation. Different implementations of this class capture the validation rules specific
	 * to a puppet language version.
	 */
	@Parameter(property = "forge.validation.complianceLevel", defaultValue = "PUPPET_3_4")
	private ComplianceLevel complianceLevel = PuppetTarget.getDefault().getComplianceLevel();

	/**
	 * What environment to use during validation.
	 */
	@Parameter(property = "forge.validation.environment", defaultValue = "production")
	private String environment = "production";

	/**
	 * Check the module layout.
	 */
	@Parameter(property = "forge.validation.checkLayout", defaultValue = "true")
	private boolean checkLayout = true;

	/**
	 * If this is set to <tt>true</tt> then the validator will make an attempt to resolve and install all dependencies
	 * for the modules that are
	 * validated. Dependencies are resolved transitively and unresolved dependencies are considered to be validation
	 * errors.
	 */
	@Parameter(property = "forge.validation.checkModuleSemantics", defaultValue = "false")
	private boolean checkModuleSemantics = false;

	/**
	 * If this is set to <tt>true</tt> then the validator will check puppet cross references.
	 */
	@Parameter(property = "forge.validation.checkReferences", defaultValue = "false")
	private boolean checkReferences = false;

	/**
	 * How should assignment to variable $string be treated. Puppet bug http://projects.puppetlabs.com/issues/14093.
	 */
	@Parameter(property = "forge.validation.assignmentToVarNamedString", defaultValue = "WARNING")
	private ValidationPreference assignmentToVarNamedString = WARNING;

	/**
	 * How should assignment to variable $trusted be treated.
	 */
	@Parameter(property = "forge.validation.assignmentToVarNamedTrusted", defaultValue = "WARNING")
	private ValidationPreference assignmentToVarNamedTrusted = WARNING;

	/**
	 * How should mode attributes that are not strings but otherwise ok be treated
	 */
	@Parameter(property = "forge.validation.attributeIsNotString", defaultValue = "WARNING")
	private ValidationPreference attributeIsNotString = WARNING;

	/**
	 * Puppet interprets the strings "false" and "true" as boolean true when they are used in a boolean context.
	 * This validation preference flags them as "not a boolean value"
	 */
	@Parameter(property = "forge.validation.booleansInStringForm", defaultValue = "IGNORE")
	private ValidationPreference booleansInStringForm = IGNORE;

	/**
	 * How an (optional) default that is not placed last should be validated for a case expression.
	 */
	@Parameter(property = "forge.validation.caseDefaultShouldAppearLast", defaultValue = "IGNORE")
	private ValidationPreference caseDefaultShouldAppearLast = IGNORE;

	/**
	 * How to validate use of 'import' keyword that was deprecated in Puppet 3.5
	 */
	@Parameter(property = "forge.validation.importIsDeprecated", defaultValue = "WARNING")
	protected ValidationPreference deprecatedImport = WARNING;

	/**
	 * How to validate that use of node inheritance that is deprecated and will be invalid in Puppet &gt;= 4.0
	 */
	@Parameter(property = "forge.validation.deprecatedNodeInheritance", defaultValue = "WARNING")
	protected ValidationPreference deprecatedNodeInheritance = WARNING;

	/**
	 * How to validate that use of += operator is deprecated and will be invalid in Puppet &gt;= 4.0
	 */
	@Parameter(property = "forge.validation.deprecatedPlusEquals", defaultValue = "WARNING")
	protected ValidationPreference deprecatedPlusEquals = WARNING;

	/**
	 * How to validate that use of variable names that starts with a digit or upper case letter is deprecated and will be invalid in Puppet
	 * &gt;= 4.0
	 */
	@Parameter(property = "forge.validation.deprecatedVariableName", defaultValue = "WARNING")
	protected ValidationPreference deprecatedVariableName = WARNING;

	/**
	 * How to validate a dq string - style guide says single quoted should be used if possible.
	 */
	@Parameter(property = "forge.validation.dqStringNotRequired", defaultValue = "IGNORE")
	private ValidationPreference dqStringNotRequired = IGNORE;

	/**
	 * How to validate a dq string when it only contains a single interpolated variable.
	 */
	@Parameter(property = "forge.validation.dqStringNotRequiredVariable", defaultValue = "IGNORE")
	private ValidationPreference dqStringNotRequiredVariable = IGNORE;

	/**
	 * How the 'ensure' property should be validated if not placed first among a resource's properties.
	 */
	@Parameter(property = "forge.validation.ensureShouldAppearFirstInResource", defaultValue = "IGNORE")
	private ValidationPreference ensureShouldAppearFirstInResource = IGNORE;

	/**
	 * How to validate hyphens in non brace enclosed interpolations. In < 2.7 interpolation stops at a hyphen, but
	 * not in 2.7. Thus when using 2.6 code in 2.7 or vice versa, the result is different.
	 */
	@Parameter(property = "forge.validation.interpolatedNonBraceEnclosedHyphens", defaultValue = "WARNING")
	private ValidationPreference interpolatedNonBraceEnclosedHyphens = WARNING;

	/**
	 * How to validate a missing 'default' in switch type expressions i.e. 'case' and 'selector'
	 */
	@Parameter(property = "forge.validation.missingDefaultInSelector", defaultValue = "WARNING")
	private ValidationPreference missingDefaultInSelector = WARNING;

	/**
	 * How to 'validate' the presence of ML comments.
	 */
	@Parameter(property = "forge.validation.mlComments", defaultValue = "IGNORE")
	private ValidationPreference mlComments = IGNORE;

	/**
	 * How to validate right to left relationships ( e.g. a <- b and a <~ b)
	 */
	@Parameter(property = "forge.validation.rightToLeftRelationships", defaultValue = "IGNORE")
	private ValidationPreference rightToLeftRelationships = IGNORE;

	/**
	 * How an (almost required) default that is not placed last should be validated for a selector expression.
	 */
	@Parameter(property = "forge.validation.selectorDefaultShouldAppearLast", defaultValue = "IGNORE")
	private ValidationPreference selectorDefaultShouldAppearLast = IGNORE;

	/**
	 * How to validate unbraced interpolation.
	 */
	@Parameter(property = "forge.validation.unbracedInterpolation", defaultValue = "IGNORE")
	private ValidationPreference unbracedInterpolation = IGNORE;

	/**
	 * How to validate a literal resource title. Style guide says they should be single quoted.
	 */
	@Parameter(property = "forge.validation.unquotedResourceTitles", defaultValue = "IGNORE")
	private ValidationPreference unquotedResourceTitles = IGNORE;

	/**
	 * A list of {@link PuppetLintRunner.Option Option} declarations that controls the behavior
	 * of the puppetlint gem.
	 *
	 * @See {@link #enablePuppetLintValidation}
	 */
	@Parameter(property = "forge.lint.options")
	private String[] puppetLintOptions;

	/**
	 * How should circular module dependencies be reported
	 */
	@Parameter(property = "forge.validation.circularDependency", defaultValue = "WARNING")
	private ValidationPreference circularDependency = WARNING;

	/**
	 * How should version ranges that doesn't match the target version be reported
	 */
	@Parameter(property = "forge.validation.dependencyVersionMismatch", defaultValue = "WARNING")
	private ValidationPreference dependencyVersionMismatch = WARNING;

	/**
	 * How should deprecated keys be reported
	 */
	@Parameter(property = "forge.validation.deprecatedKey", defaultValue = "WARNING")
	private ValidationPreference deprecatedKey = WARNING;

	/**
	 * How should empty or missing fields required by Puppet Forge be reported
	 */
	@Parameter(property = "forge.validation.missingForgeRequiredFields", defaultValue = "WARNING")
	private ValidationPreference missingForgeRequiredFields = WARNING;

	/**
	 * How should ModuleNames that contains upper cased characters or owner that start with a digit be reported
	 */
	@Parameter(property = "forge.validation.moduleNameNotStrict", defaultValue = "WARNING")
	private ValidationPreference moduleNameNotStrict = WARNING;

	/**
	 * How should detection of multiple modules with the same name be reported
	 */
	@Parameter(property = "forge.validation.moduleRedefinition", defaultValue = "WARNING")
	private ValidationPreference moduleRedefinition = WARNING;

	/**
	 * How should existence of the deprecated Modulefile be reported
	 */
	@Parameter(property = "forge.validation.modulefileExists", defaultValue = "IGNORE")
	private ValidationPreference modulefileExists = IGNORE;

	/**
	 * How should existence of the deprecated Modulefile be reported in case the metadata.json is missing
	 */
	@Parameter(property = "forge.validation.modulefileExistsAndIsUsed", defaultValue = "WARNING")
	private ValidationPreference modulefileExistsAndIsUsed = WARNING;

	/**
	 * How should submodule directories be reported
	 */
	@Parameter(property = "forge.validation.unexpectedSubmodule", defaultValue = "WARNING")
	private ValidationPreference unexpectedSubmodule = WARNING;

	/**
	 * How should unrecognized keys be reported
	 */
	@Parameter(property = "forge.validation.unrecognizedKey", defaultValue = "WARNING")
	private ValidationPreference unrecognizedKey = WARNING;

	/**
	 * How should unresolved references be reported
	 */
	@Parameter(property = "forge.validation.unresolvedReference", defaultValue = "WARNING")
	private ValidationPreference unresolvedReference = WARNING;

	/**
	 * How to handle cases where expression validity cannot be asserted until runtime.
	 */
	@Parameter(property = "forge.validation.validityAssertedAtRuntime", defaultValue = "IGNORE")
	protected ValidationPreference validityAssertedAtRuntime = IGNORE;

	/**
	 * How should tags containing whitespace characters be reported
	 */
	@Parameter(property = "forge.validation.whitespaceInTag", defaultValue = "WARNING")
	private ValidationPreference whitespaceInTag = WARNING;

	private final IModuleValidationAdvisor moduleValidationAdvisor = new IModuleValidationAdvisor() {

		@Override
		public ValidationPreference getCircularDependency() {
			return circularDependency;
		}

		@Override
		public ValidationPreference getDependencyVersionMismatch() {
			return dependencyVersionMismatch;
		}

		@Override
		public ValidationPreference getDeprecatedKey() {
			return deprecatedKey;
		}

		@Override
		public ValidationPreference getMissingForgeRequiredFields() {
			return missingForgeRequiredFields;
		}

		@Override
		public ValidationPreference getModulefileExists() {
			return modulefileExists;
		}

		@Override
		public ValidationPreference getModulefileExistsAndIsUsed() {
			return modulefileExistsAndIsUsed;
		}

		@Override
		public ValidationPreference getModuleNameNotStrict() {
			return moduleNameNotStrict;
		}

		@Override
		public ValidationPreference getModuleRedefinition() {
			return moduleRedefinition;
		}

		@Override
		public ValidationPreference getUnexpectedSubmodule() {
			return unexpectedSubmodule;
		}

		@Override
		public ValidationPreference getUnrecognizedKey() {
			return unrecognizedKey;
		}

		@Override
		public ValidationPreference getUnresolvedReference() {
			return unresolvedReference;
		}

		@Override
		public ValidationPreference getWhitespaceInTag() {
			return whitespaceInTag;
		}
	};

	private final IPotentialProblemsAdvisor potentialProblemsAdvisor = new IPotentialProblemsAdvisor() {
		@Override
		public ValidationPreference assignmentToVarNamedString() {
			return assignmentToVarNamedString;
		}

		@Override
		public ValidationPreference assignmentToVarNamedTrusted() {
			return assignmentToVarNamedTrusted;
		}

		@Override
		public ValidationPreference attributeIsNotString() {
			return attributeIsNotString;
		}

		@Override
		public ValidationPreference booleansInStringForm() {
			return booleansInStringForm;
		}

		@Override
		public ValidationPreference caseDefaultShouldAppearLast() {
			return caseDefaultShouldAppearLast;
		}

		@Override
		public ValidationPreference deprecatedImport() {
			return deprecatedImport;
		}

		@Override
		public ValidationPreference deprecatedNodeInheritance() {
			return deprecatedNodeInheritance;
		}

		@Override
		public ValidationPreference deprecatedPlusEquals() {
			return deprecatedPlusEquals;
		}

		@Override
		public ValidationPreference deprecatedVariableName() {
			return deprecatedVariableName;
		}

		@Override
		public ValidationPreference dqStringNotRequired() {
			return dqStringNotRequired;
		}

		@Override
		public ValidationPreference dqStringNotRequiredVariable() {
			return dqStringNotRequiredVariable;
		}

		@Override
		public ValidationPreference ensureShouldAppearFirstInResource() {
			return ensureShouldAppearFirstInResource;
		}

		@Override
		public ValidationPreference interpolatedNonBraceEnclosedHyphens() {
			return interpolatedNonBraceEnclosedHyphens;
		}

		@Override
		public ValidationPreference missingDefaultInSelector() {
			return missingDefaultInSelector;
		}

		@Override
		public ValidationPreference mlComments() {
			return mlComments;
		}

		@Override
		public ValidationPreference rightToLeftRelationships() {
			return rightToLeftRelationships;
		}

		@Override
		public ValidationPreference selectorDefaultShouldAppearLast() {
			return selectorDefaultShouldAppearLast;
		}

		@Override
		public ValidationPreference unbracedInterpolation() {
			return unbracedInterpolation;
		}

		@Override
		public ValidationPreference unquotedResourceTitles() {
			return unquotedResourceTitles;
		}

		@Override
		public ValidationPreference validityAssertedAtRuntime() {
			return validityAssertedAtRuntime;
		}
	};

	public Validate() {
		super();
	}

	@Override
	protected void addModules(Diagnostic diagnostic, List<Module> modules) {
		super.addModules(diagnostic, modules);
		if(cacheLocation != null && diagnostic.getSeverity() <= Diagnostic.WARNING)
			modules.add(new AbstractModule() {
				@Override
				protected void configure() {
					bind(File.class).annotatedWith(Names.named(Forge.CACHE_LOCATION)).toInstance(new File(cacheLocation));
				}
			});
	}

	private Diagnostic convertPuppetLintDiagnostic(File moduleRoot, Issue issue) {
		FileDiagnostic diagnostic = new FileDiagnostic(getSeverity(issue), PuppetLintService.PUPPET_LINT, issue.getMessage(), new File(
			getRelativePath(new File(moduleRoot, issue.getPath()))));
		diagnostic.setLineNumber(issue.getLineNumber());
		return diagnostic;
	}

	private void geppettoValidation(Collection<File> moduleLocations, Diagnostic result) throws IOException {

		Collection<File> importedModuleLocations = null;
		if(checkModuleSemantics) {
			List<Metadata> metadatas = new ArrayList<Metadata>();
			for(File moduleRoot : moduleLocations) {
				Metadata md = getModuleMetadata(moduleRoot, result);
				if(md != null)
					metadatas.add(md);
			}
			File importedModulesDir = new File(getBuildDir(), IMPORTED_MODULES_ROOT);
			importedModuleLocations = getForge().downloadDependencies(metadatas, importedModulesDir, result);
		}
		if(importedModuleLocations == null)
			importedModuleLocations = Collections.emptyList();

		RubyHelper.setRubyServicesFactory(JRubyServices.FACTORY);
		ValidationOptions options = getValidationOptions(moduleLocations, importedModuleLocations);
		new PPDiagnosticsSetup(complianceLevel, options.getProblemsAdvisor()).createInjectorAndDoEMFRegistration();
		getValidationService().validate(result, options, getModulesDir(), new NullProgressMonitor());
	}

	@Override
	protected String getActionName() {
		return "Validation";
	}

	private String getSearchPath(Collection<File> moduleLocations, Collection<File> importedModuleLocations) {
		StringBuilder searchPath = new StringBuilder();

		searchPath.append("lib/*:environments/$environment/*");

		for(File moduleLocation : moduleLocations)
			searchPath.append(":" + getRelativePath(moduleLocation) + "/*");

		for(File importedModuleLocation : importedModuleLocations)
			searchPath.append(":" + importedModuleLocation.getPath() + "/*");
		return searchPath.toString();
	}

	private ValidationOptions getValidationOptions(Collection<File> moduleLocations, Collection<File> importedModuleLocations) {
		ValidationOptions options = new ValidationOptions();
		options.setCheckLayout(checkLayout);
		options.setCheckModuleSemantics(checkModuleSemantics);
		options.setCheckReferences(checkReferences);

		if(moduleLocations.size() == 1 && getModulesDir().equals(moduleLocations.iterator().next()))
			options.setFileType(FileType.MODULE_ROOT);
		else
			options.setFileType(FileType.PUPPET_ROOT);
		options.setFileFilter(getFileFilter());
		options.setPlatformURI(PuppetTarget.forComplianceLevel(complianceLevel, false).getPlatformURI());
		options.setEncodingProvider(new IEncodingProvider() {
			@Override
			public String getEncoding(URI file) {
				return UTF_8.name();
			}
		});

		options.setSearchPath(getSearchPath(moduleLocations, importedModuleLocations));
		options.setProblemsAdvisor(potentialProblemsAdvisor);
		options.setModuleValidationAdvisor(moduleValidationAdvisor);
		return options;
	}

	@Override
	protected void invoke(Diagnostic result) throws IOException {
		Collection<File> moduleRoots = findModuleRoots();
		if(moduleRoots.isEmpty()) {
			result.addChild(new Diagnostic(Diagnostic.ERROR, ValidationService.GEPPETTO, "No modules found in repository"));
			return;
		}

		if(checkLayout || checkModuleSemantics || checkReferences)
			geppettoValidation(moduleRoots, result);

		if(enablePuppetLintValidation)
			lintValidation(moduleRoots, result);
	}

	private void lintValidation(Collection<File> moduleLocations, Diagnostic result) throws IOException {
		PuppetLintRunner runner = PuppetLintService.getInstance().getPuppetLintRunner();
		getLog().debug("Performing puppet lint validation on all modules");
		boolean inverted = false;
		if(puppetLintOptions == null || puppetLintOptions.length == 0) {
			puppetLintOptions = new String[0];
			inverted = true;
		}
		for(File moduleRoot : moduleLocations) {
			for(PuppetLintRunner.Issue issue : runner.run(moduleRoot, inverted, puppetLintOptions)) {
				Diagnostic diag = convertPuppetLintDiagnostic(moduleRoot, issue);
				if(diag != null)
					result.addChild(diag);
			}
		}
	}
}
