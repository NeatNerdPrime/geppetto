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

/**
 * Interface for debugging and tracing output.
 */
public interface ITracer {

	public IStringProvider getStringProvider();

	public boolean isTracing();

	public void trace(String message, Object... objects);
}
