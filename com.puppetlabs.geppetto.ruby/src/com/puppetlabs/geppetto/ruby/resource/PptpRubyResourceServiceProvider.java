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
package com.puppetlabs.geppetto.ruby.resource;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.resource.IResourceDescription.Manager;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.validation.IResourceValidator;

public class PptpRubyResourceServiceProvider implements IResourceServiceProvider {

	/**
	 * Returns true for .rb files that make a contribution to PPTP.
	 *
	 * This is the only difference from the default...
	 */
	@Override
	public boolean canHandle(URI uri) {
		return PptpRubyResource.detectLoadType(uri) != PptpRubyResource.LoadType.IGNORED;
	}

	@Override
	public <T> T get(Class<T> t) {
		return null;
	}

	@Override
	public org.eclipse.xtext.resource.IContainer.Manager getContainerManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IEncodingProvider getEncodingProvider() {
		return null;
	}

	@Override
	public Manager getResourceDescriptionManager() {
		return null;
	}

	@Override
	public IResourceValidator getResourceValidator() {
		return IResourceValidator.NULL;
	}

}
