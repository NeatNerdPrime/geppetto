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
package com.puppetlabs.geppetto.forge.util;

import java.io.File;

import com.puppetlabs.geppetto.forge.FilePosition;
import com.puppetlabs.geppetto.forge.model.Dependency;

public class DependencyWithPosition extends Dependency implements FilePosition {
	private final int offset;

	private final int length;

	private final int line;

	private final File file;

	public DependencyWithPosition(FilePosition pos) {
		this.offset = pos.getOffset();
		this.length = pos.getLength();
		this.line = pos.getLine();
		this.file = pos.getFile();
	}

	public DependencyWithPosition(int offset, int length, int line, File file) {
		this.offset = offset;
		this.length = length;
		this.line = line;
		this.file = file;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public int getLine() {
		return line;
	}

	@Override
	public int getOffset() {
		return offset;
	}

}
