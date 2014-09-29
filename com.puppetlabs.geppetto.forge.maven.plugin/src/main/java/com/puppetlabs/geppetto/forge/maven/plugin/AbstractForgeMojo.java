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

import static com.puppetlabs.geppetto.injectable.CommonModuleProvider.getCommonModule;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.puppetlabs.geppetto.common.os.FileUtils;
import com.puppetlabs.geppetto.diagnostic.Diagnostic;
import com.puppetlabs.geppetto.forge.Forge;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.validation.ValidationService;
import com.puppetlabs.geppetto.validation.impl.ValidationModule;

/**
 * Goal which performs basic validation.
 */
public abstract class AbstractForgeMojo extends AbstractMojo {
	public static boolean isNull(String field) {
		if(field == null)
			return true;

		field = field.trim();
		if(field.length() == 0)
			return true;

		return "null".equals(field);
	}

	public static boolean isParentOrEqual(File dir, File subdir) {
		if(dir == null || subdir == null)
			return false;

		return dir.equals(subdir) || isParentOrEqual(dir, subdir.getParentFile());
	}

	public static Properties readForgeProperties() throws IOException {
		Properties props = new Properties();
		InputStream inStream = AbstractForgeMojo.class.getResourceAsStream("/forge.properties");
		if(inStream == null)
			throw new FileNotFoundException("Resource forge.properties");
		try {
			props.load(inStream);
			return props;
		}
		finally {
			inStream.close();
		}
	}

	static final String IMPORTED_MODULES_ROOT = "importedModules";

	static final Charset UTF_8 = Charset.forName("UTF-8");

	/**
	 * The directory where this plug-in will search for modules. The directory itself
	 * can be a module or it may be the root of a hierarchy where modules can be found.
	 */
	@Parameter(property = "forge.modules.root", defaultValue = "${project.basedir}")
	private String modulesRoot;

	@Component
	private MavenSession session;

	private transient File baseDir;

	private transient File buildDir;

	private transient File modulesDir;

	private transient Injector injector;

	private transient Logger log;

	protected void addModules(Diagnostic diagnostic, List<Module> modules) {
		modules.add(new ForgeMavenModule(getFileFilter(), session.getCurrentProject()));
		modules.add(new ValidationModule());
		modules.add(getCommonModule());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Diagnostic diagnostic = new LoggingDiagnostic(getLogger());
		try {
			List<Module> modules = new ArrayList<Module>();
			addModules(diagnostic, modules);
			if(diagnostic.getSeverity() <= Diagnostic.WARNING) {
				injector = Guice.createInjector(modules);
				invoke(diagnostic);
			}
		}
		catch(JsonParseException e) {
			throw new MojoFailureException(getActionName() + " failed: Invalid Json: " + e.getMessage(), e);
		}
		catch(RuntimeException e) {
			throw e;
		}
		catch(Exception e) {
			throw new MojoFailureException(getActionName() + " failed: " + e.getMessage(), e);
		}
		if(diagnostic.getSeverity() == Diagnostic.ERROR) {
			StringBuilder bld = new StringBuilder();
			diagnostic.toString(Diagnostic.ERROR, bld, 0);
			String msg = bld.toString();
			Exception e = diagnostic.getException();
			if(e == null)
				throw new MojoFailureException(msg);
			throw new MojoFailureException(msg, e);
		}
	}

	protected Collection<File> findModuleRoots() {
		return getForgeUtil().findModuleRoots(getModulesDir(), null);
	}

	protected abstract String getActionName();

	/**
	 * Returns the basedir as an absolute path string without any '..' constructs.
	 *
	 * @return The absolute path of basedir
	 */
	public File getBasedir() {
		if(baseDir == null) {
			MavenProject project = session.getCurrentProject();
			URI basedirURI;
			File pbd = project.getBasedir();
			if(pbd != null)
				basedirURI = pbd.toURI();
			else
				basedirURI = URI.create(session.getExecutionRootDirectory());
			try {
				baseDir = new File(basedirURI.normalize());
			}
			catch(IllegalArgumentException e) {
				// URI is not absolute. Try and make it relative to the current directory
				URI here = new File(".").getAbsoluteFile().toURI();
				baseDir = new File(here.resolve(basedirURI).normalize());
			}
		}
		return baseDir;
	}

	protected synchronized File getBuildDir() {
		if(buildDir == null) {
			Build build = session.getCurrentProject().getBuild();
			String buildDirStr = build == null
				? null
				: build.getDirectory();
			if(buildDirStr == null)
				buildDir = new File(getBasedir(), "target");
			else {
				File bd = new File(buildDirStr);
				buildDir = new File(bd.toURI().normalize());
			}
		}
		return buildDir;
	}

	/**
	 * Returns an exclusion filter that rejects everything beneath the build directory plus everything that
	 * the default exclusion filter would reject.
	 *
	 * @return <tt>true</tt> if the file can be accepted for inclusion
	 */
	protected FileFilter getFileFilter() {
		return new FileFilter() {
			@Override
			public boolean accept(File file) {
				return FileUtils.DEFAULT_FILE_FILTER.accept(file) && !isParentOrEqual(getBuildDir(), file);
			}
		};
	}

	protected Forge getForgeUtil() {
		return injector.getInstance(Forge.class);
	}

	protected Gson getGson() {
		return injector.getInstance(Gson.class);
	}

	protected Injector getInjector() {
		return injector;
	}

	protected Logger getLogger() {
		if(log == null) {
			log = LoggerFactory.getLogger(getClass());
		}
		return log;
	}

	protected Metadata getModuleMetadata(File moduleDirectory, Diagnostic diag) throws IOException {
		File mdJson = new File(moduleDirectory, Forge.METADATA_JSON_NAME);
		if(mdJson.exists())
			try {
				return getForgeUtil().loadJSONMetadata(mdJson);
			}
			catch(Exception e) {
				// We don't want metadata.json diagnostics at this point since xtext validation
				// will provide them later
				return null;
			}

		File moduleFile = new File(moduleDirectory, Forge.MODULEFILE_NAME);
		if(moduleFile.exists())
			return getForgeUtil().loadModulefile(moduleFile, diag);

		return null;
	}

	protected File getModulesDir() {
		if(modulesDir == null) {
			if(modulesRoot == null)
				modulesDir = getBasedir();
			else {
				File md = new File(modulesRoot);
				if(!md.isAbsolute())
					md = new File(getBasedir(), modulesRoot);
				modulesDir = new File(md.toURI().normalize());
			}
		}
		return modulesDir;
	}

	protected MavenProject getProject() {
		return session.getCurrentProject();
	}

	protected String getRelativePath(File file) {
		IPath rootPath = Path.fromOSString(getModulesDir().getAbsolutePath());
		IPath path = Path.fromOSString(file.getAbsolutePath());
		IPath relative = path.makeRelativeTo(rootPath);
		return relative.toPortableString();
	}

	protected ValidationService getValidationService() {
		return injector.getInstance(ValidationService.class);
	}

	protected abstract void invoke(Diagnostic result) throws Exception;

	public void setLogger(Logger log) {
		this.log = log;
	}
}
