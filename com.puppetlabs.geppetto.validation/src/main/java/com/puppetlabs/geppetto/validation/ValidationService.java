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
package com.puppetlabs.geppetto.validation;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;

import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.diagnostic.DiagnosticType;
import com.puppetlabs.geppetto.validation.runner.BuildResult;

/**
 */
public interface ValidationService {
	DiagnosticType PP_LINKING = new DiagnosticType("PP", ValidationService.class.getName());

	DiagnosticType PP_SYNTAX = new DiagnosticType("PP_SYNTAX", ValidationService.class.getName());

	DiagnosticType MODULE = new DiagnosticType("MODULE", ValidationService.class.getName());

	DiagnosticType INTERNAL_ERROR = new DiagnosticType("INTERNAL_ERROR", ValidationService.class.getName());

	DiagnosticType RUBY_SYNTAX = new DiagnosticType("RUBY_SYNTAX", ValidationService.class.getName());

	DiagnosticType RUBY = new DiagnosticType("RUBY", ValidationService.class.getName());

	DiagnosticType CATALOG_PARSER = new DiagnosticType("CATALOG_PARSER", ValidationService.class.getName());

	DiagnosticType CATALOG = new DiagnosticType("CATALOG", ValidationService.class.getName());

	/**
	 * Performs validation and reports diagnostics for all files given by source under the control of options. If a set
	 * of examinedFiles is given the diagnostics reported is limited to this set. An empty set is the same as reporting
	 * diagnostics for all files in source.
	 * The options may be null, in which case a syntax check is performed on everything in the intersection of source
	 * and examinedFiles.
	 * The examinedFiles may contain individual regular files or directories. Directories in examinedFiles are validated
	 * as modules. One special case is when examinedFiles contains the source and source is a Directory - this is
	 * interpreted as source is a PUPPET-ROOT, and that validation for everything in the non-modules part is wanted (and
	 * possibly for a select set of modules).
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param source
	 *            A String containing PP code to be validated. Errors are reported for a fictious file "unnamed.pp".
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	BuildResult validate(Diagnostic diagnostics, ValidationOptions options, File source, IProgressMonitor monitor);

	/**
	 * Validates PP syntax for code given in code parameter.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param code
	 *            A String containing PP code to be validated. Errors are reported for a fictious file "unnamed.pp".
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	Resource validate(Diagnostic diagnostics, ValidationOptions options, String code, IProgressMonitor monitor);

	/**
	 * Validates validity of a file (typically a .pp file). It may or may not include diagnostics that related to
	 * linking depending on the implementation of the validator. The file parameter is a reference to a regular file.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param sourceFile
	 *            A File containing .pp code (must end with .pp).
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	void validateManifest(Diagnostic diagnostics, ValidationOptions options, File sourceFile, IProgressMonitor monitor);

	/**
	 * Validates PP syntax for code given in code parameter.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param code
	 *            A String containing PP code to be validated. Errors are reported for a fictious file "unnamed.pp".
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	Resource validateManifest(Diagnostic diagnostics, ValidationOptions options, String code, IProgressMonitor monitor);

	/**
	 * Statically validates all files contained in a Puppet module. The File parameter is a reference to the top level
	 * folder containing the module.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param moduleDirectory
	 *            A File referencing a directory containing a Puppet Module.
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	BuildResult validateModule(Diagnostic diagnostics, ValidationOptions options, File moduleDirectory, IProgressMonitor monitor);

	/**
	 * Performs static com.puppetlabs.geppetto.validation and catalog production for a given node, it's factor data, a
	 * site.pp file, and a directory of modules.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param catalogRoot
	 *            A File referencing a directory/folder where a set of subfolders each conform to Puppet Module rules.
	 * @param factorData
	 *            A File containing Factor data for the node for which the catalog should be produced/validated.
	 * @param siteFile
	 *            A File referencing a site.pp (typical name) that maps hosts to puppet nodes.
	 * @param nodeName
	 *            The name of the node (does not have to be the same as the hostname).
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	void validateRepository(Diagnostic diagnostics, ValidationOptions options, File catalogRoot, File factorData, File siteFile,
			String nodeName, IProgressMonitor monitor);

	/**
	 * Statically validates all modules contained in a catalog (a directory of Puppet modules). The file parameter is a
	 * reference to the top level directory.
	 *
	 * @param diagnostics
	 *            DiagnosticChain will receive calls to add Diagnostic instances for discovered problems/information.
	 * @param options
	 *            Options that controls various aspects of the validation
	 * @param catalogRoot
	 *            A File referencing a directory containing Puppet Module directories. <!-- end-model-doc -->
	 * @param monitor
	 *            Monitor where progress is reported. Also used as cancellation mechanism
	 */
	BuildResult validateRepository(Diagnostic diagnostics, ValidationOptions options, File catalogRoot, IProgressMonitor monitor);

}
