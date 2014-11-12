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
package com.puppetlabs.geppetto.pp.dsl.pptp;

import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.generic.AbstractGenericResourceRuntimeModule;

import com.google.inject.Binder;
import com.puppetlabs.geppetto.injectable.CommonModuleProvider;
import com.puppetlabs.geppetto.pp.dsl.PPDSLConstants;
import com.puppetlabs.geppetto.pp.dsl.linking.PPQualifiedNameConverter;
import com.puppetlabs.geppetto.pp.dsl.linking.PPResourceDescriptionStrategy;
import com.puppetlabs.geppetto.pp.dsl.linking.PPSearchPath.ISearchPathProvider;
import com.puppetlabs.geppetto.pp.dsl.linking.PPSearchPathProvider;

/**
 * A runtime module for PPTP generic model.
 */
public class PptpRuntimeModule extends AbstractGenericResourceRuntimeModule {

	/**
	 * Binds resource description strategy that adds pptp data to descriptions.
	 *
	 * @return
	 */
	public Class<? extends IDefaultResourceDescriptionStrategy> bindIDefaultResourceDescriptionStrategy() {
		return PPResourceDescriptionStrategy.class;
	}

	/**
	 * Handles FQN <-> String conversion and defines "::" as the separator.
	 * (PPTP reuses the "PP" converter).
	 */
	public Class<? extends IQualifiedNameConverter> bindIQualifiedNameConverter() {
		return PPQualifiedNameConverter.class;
	}

	/**
	 * Handles creation of QualifiedNames for referenceable PPTP model elements.
	 */
	@Override
	public Class<? extends IQualifiedNameProvider> bindIQualifiedNameProvider() {
		return PptpQualifiedNameProvider.class;
	}

	public Class<? extends ISearchPathProvider> bindSearchPathProvider() {
		return PPSearchPathProvider.class;
	}

	public void configureCommon(Binder binder) {
		binder.install(CommonModuleProvider.getCommonModule());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.xtext.resource.generic.AbstractGenericResourceRuntimeModule#getFileExtensions()
	 */
	@Override
	protected String getFileExtensions() {
		return PPDSLConstants.PPTP_EXTENSION;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.xtext.resource.generic.AbstractGenericResourceRuntimeModule#getLanguageName()
	 */
	@Override
	protected String getLanguageName() {
		return PPDSLConstants.PPTP_LANGUAGE_NAME;
	}

}
