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
package com.puppetlabs.geppetto.diagnostic;

import java.io.File;

public class DetailedFileDiagnostic extends FileDiagnostic {
	private static final long serialVersionUID = 1L;

	private int offset;

	private int length;

	public DetailedFileDiagnostic(int severity, DiagnosticType type, String message, File file) {
		super(severity, type, message, file);
	}

	@Override
	public boolean appendLocationLabel(StringBuilder builder, boolean withOffsets) {
		int offset = getOffset();
		int length = getLength();
		boolean lineAppended = super.appendLocationLabel(builder, withOffsets);

		if(withOffsets && offset >= 0) {
			if(!lineAppended)
				builder.append('-'); // Indicates unknown line
			builder.append("(");
			builder.append(offset);
			if(length >= 0) {
				builder.append(",");
				builder.append(length);
			}
			builder.append(")");
			return true;
		}
		return lineAppended;
	}

	/**
	 * Length is the length (from offset) that is the textual representation
	 * that the diagnostic apply to , or 0 if there where no visible textual
	 * representation. A length of -1 indicates that length is irrelevant.
	 *
	 * @return the value of the '<em>length</em>' attribute.
	 */
	public int getLength() {
		return length;
	}

	/**
	 * The offset from the beginning of the parsed text, or -1 if offset is not
	 * available.
	 *
	 * @return the value of the '<em>offset</em>' attribute.
	 */
	public int getOffset() {
		return offset;
	}

	public void setLength(Integer newLength) {
		length = newLength == null
				? 0
				: newLength.intValue();
	}

	public void setOffset(Integer newOffset) {
		offset = newOffset == null
				? 0
				: newOffset.intValue();
	}

}
