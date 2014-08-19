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
package com.puppetlabs.geppetto.forge.model;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.Expose;

/**
 */
public class Type extends NamedTypeItem {
	@Expose
	private List<NamedTypeItem> properties = Collections.emptyList();

	@Expose
	private List<NamedTypeItem> parameters = Collections.emptyList();

	@Expose
	private List<NamedTypeItem> providers = Collections.emptyList();

	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(!(o instanceof Type))
			return false;
		Type ot = (Type) o;
		return super.equals(o) && properties.equals(ot.properties) && parameters.equals(ot.parameters) &&
				providers.equals(ot.providers);
	}

	/**
	 * @return the parameters
	 */
	public List<NamedTypeItem> getParameters() {
		return parameters;
	}

	/**
	 * @return the properties
	 */
	public List<NamedTypeItem> getProperties() {
		return properties;
	}

	/**
	 * @return the providers
	 */
	public List<NamedTypeItem> getProviders() {
		return providers;
	}

	/**
	 * @param parameters
	 *            the parameters to set
	 */
	public void setParameters(List<NamedTypeItem> parameters) {
		this.parameters = asUnmodifiableList(parameters);
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(List<NamedTypeItem> properties) {
		this.properties = asUnmodifiableList(properties);
	}

	/**
	 * @param providers
	 *            the providers to set
	 */
	public void setProviders(List<NamedTypeItem> providers) {
		this.providers = asUnmodifiableList(providers);
		;
	}
}
