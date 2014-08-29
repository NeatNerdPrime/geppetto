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
package com.puppetlabs.xtext.dommodel.formatter.css.debug;

import java.util.Map;

import com.puppetlabs.geppetto.common.tracer.IStringProvider;
import com.puppetlabs.geppetto.common.tracer.ITracer;
import com.puppetlabs.geppetto.common.util.BundleAccess;
import com.puppetlabs.xtext.dommodel.IDomNode;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleSet;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The FormattingTracer is a singleton that performs tracing if the debug option "debug/formatter" is
 * set (in .options, or via launch configuration). If not turned on, all of the formatters operations
 * have no effect.
 * The FormattingTracer also implements ITracer which delegates to a configurable tracer named {@link #DEBUG_FORMATTER}.
 */
@Singleton
public class FormattingTracer implements ITracer {

	/**
	 * Access to runtime configurable debug trace.
	 */
	private final ITracer tracer;

	/**
	 * TODO: This is obviously not generic - should reflect the name of the plugin
	 * providing the formatter.
	 */
	public static final String PLUGIN_NAME = "com.puppetlabs.geppetto.pp.dsl";

	/**
	 * Name of option in formatter providing plugin that turns on debugging/tracing
	 * of formatting.
	 */
	public static final String DEBUG_FORMATTER = PLUGIN_NAME + "/debug/formatter";

	private final boolean tracing;

	private final Map<IDomNode, StyleSet> effectiveStyleMap;

	@Inject
	public FormattingTracer(BundleAccess bundleAccess, @Named(DEBUG_FORMATTER) ITracer tracer) {
		this.tracer = tracer;
		tracing = bundleAccess.inDebugMode() && Boolean.parseBoolean(bundleAccess.getDebugOption(DEBUG_FORMATTER));
		effectiveStyleMap = new MapMaker().weakKeys().makeMap();
	}

	public StyleSet getEffectiveStyle(IDomNode node) {
		return isTracing()
			? effectiveStyleMap.get(node)
			: null;
	}

	@Override
	public IStringProvider getStringProvider() {
		return tracer.getStringProvider();
	}

	@Override
	public boolean isTracing() {
		return tracing;
	}

	public void recordEffectiveStyle(IDomNode node, StyleSet styles) {
		if(isTracing())
			effectiveStyleMap.put(node, styles);
	}

	@Override
	public void trace(String message, Object... objects) {
		tracer.trace(message, objects);
	}
}
