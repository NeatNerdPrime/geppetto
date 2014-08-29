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
package com.puppetlabs.geppetto.puppetlint.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.puppetlabs.geppetto.common.os.OsUtil;
import com.puppetlabs.geppetto.common.os.StreamUtil.OpenBAStream;
import com.puppetlabs.geppetto.puppetlint.PuppetLintRunner;

/**
 * A PuppetLintRunner implementation that uses an external command. This runner will require that Ruby is installed
 */
public class ExternalPuppetLintRunner implements PuppetLintRunner {
	private static String getPuppetLintExecutable() {
		String puppetLint = System.getenv("PUPPET_LINT_EXECUTABLE");
		if(puppetLint == null)
			puppetLint = "puppet-lint";
		return puppetLint;
	}

	private static final Pattern issuePattern = Pattern.compile(
		"^(ERROR|WARNING)\\s+([a-zA-Z0-9_-]+)\\s+#([^#]+)#:(\\d+)\\s+(.*)$", Pattern.MULTILINE);

	private static final Pattern versionPattern = Pattern.compile("^[A-Za-z0-9_.-]+\\s+(\\S+)\\s*$", Pattern.MULTILINE);

	@Override
	public String getVersion() throws IOException {
		// Verify that puppet-lint is installed. If not, then refuse to install this bundle

		OpenBAStream out = new OpenBAStream();
		OpenBAStream err = new OpenBAStream();
		File home = new File(System.getProperty("user.home"));
		int exitCode = OsUtil.runProcess(home, out, err, getPuppetLintExecutable(), "--version");
		String outStr = out.toString(Charset.defaultCharset());
		if(exitCode != 0) {
			StringBuilder bld = new StringBuilder();
			bld.append("Got exit code ");
			bld.append(exitCode);
			bld.append(" when running puppet-lint.");
			outStr = outStr.trim();
			if(!outStr.isEmpty()) {
				bld.append(" Output \"");
				bld.append(outStr);
				bld.append('"');
			}

			String errStr = err.toString(Charset.defaultCharset()).trim();
			if(!errStr.isEmpty()) {
				bld.append(" Errors \"");
				bld.append(errStr);
				bld.append('"');
			}
			throw new IOException(bld.toString());
		}
		String version = outStr;
		Matcher m = versionPattern.matcher(version);
		if(m.find())
			version = m.group(1);
		return version;
	}

	@Override
	public List<Issue> run(File fileOrDirectory, Option... options) throws IOException {
		String pathToCheck = ".";
		if(fileOrDirectory.isFile()) {
			pathToCheck = fileOrDirectory.getName();
			fileOrDirectory = fileOrDirectory.getParentFile();
		}
		List<String> params = new ArrayList<String>();
		params.add(getPuppetLintExecutable());
		params.add("--log-format");
		params.add("%{KIND} %{check} #%{path}#:%{linenumber} %{message}");
		for(Option option : options)
			params.add("--" + option);
		params.add(pathToCheck);

		OpenBAStream out = new OpenBAStream();
		OpenBAStream err = new OpenBAStream();
		OsUtil.runProcess(fileOrDirectory, out, err, params.toArray(new String[params.size()]));
		List<Issue> issues = new ArrayList<Issue>();
		Matcher m = issuePattern.matcher(out.toString(Charset.defaultCharset()));
		while(m.find()) {
			String path = m.group(3);
			if(path.startsWith("./") || path.startsWith(".\\"))
				path = path.substring(2);
			issues.add(new PuppetLintIssue(path, Severity.valueOf(m.group(1)), m.group(2), m.group(5), Integer.parseInt(m.group(4))));
		}
		return issues;
	}
}
