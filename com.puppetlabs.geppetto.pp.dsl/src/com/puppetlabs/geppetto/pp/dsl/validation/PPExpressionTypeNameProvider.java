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
package com.puppetlabs.geppetto.pp.dsl.validation;

import com.puppetlabs.geppetto.common.tracer.IStringProvider;

import com.google.inject.Singleton;

/**
 * A very simple translator of Expression type to a label.
 *
 */
@Singleton
public class PPExpressionTypeNameProvider implements IStringProvider {
	@Override
	public String doToString(Object o) {
		// *very* simple implementation
		return o.getClass().getSimpleName().replaceAll("Impl", "");
	}

}
