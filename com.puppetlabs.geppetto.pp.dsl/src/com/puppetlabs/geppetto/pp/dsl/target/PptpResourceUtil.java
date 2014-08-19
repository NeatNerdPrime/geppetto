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
package com.puppetlabs.geppetto.pp.dsl.target;

import java.net.URL;

import org.eclipse.emf.common.util.URI;

/**
 * Utility class to obtain pptp resource URIs
 */
public class PptpResourceUtil {
	/**
	 * Returns a URI suitable to load the pptp with a call to {@link #loadResource(URI)}.
	 * Currently, returns {@link #getPuppet_2_7_19()}.
	 *
	 * @return
	 */
	// public static URI getDefaultPptpResourceURI() {
	// return getPuppet_2_7_19();
	// }

	public static URI getFacter_1_6() {
		return getURI("facter-1.6.pptp");
	}

	public static URI getURI(String pathAsString) {
		URL resource = PptpResourceUtil.class.getResource(pathAsString);
		if(resource == null)
			throw new IllegalStateException("Couldn't find resource on classpath. Path was '" + pathAsString + "'");
		return URI.createURI(resource.toString(), true);
	}
}
