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
package com.puppetlabs.geppetto.common.tracer;

import com.google.inject.ImplementedBy;

/**
 * A provider of String labels.
 */
@ImplementedBy(DefaultTracer.DefaultStringProvider.class)
public interface IStringProvider {
	public String doToString(Object o);
}
