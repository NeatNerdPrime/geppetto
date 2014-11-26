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
package com.puppetlabs.geppetto.forge.v2.model;

import com.google.gson.annotations.Expose;
import com.puppetlabs.geppetto.forge.model.Entity;

/**
 * This is a simply entity for self referring URL representation.
 */
public class SelfLink extends Entity {
	private static final long serialVersionUID = 1L;

	@Expose
	private HalLink self;

	/**
	 * @return Link to self
	 */
	public HalLink getSelf() {
		return self;
	}

	/**
	 * @param self
	 *            the self to set
	 */
	public void setSelf(HalLink self) {
		this.self = self;
	}
}
