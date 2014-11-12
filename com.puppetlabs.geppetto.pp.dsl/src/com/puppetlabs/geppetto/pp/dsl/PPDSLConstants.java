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
package com.puppetlabs.geppetto.pp.dsl;

import org.eclipse.xtext.resource.IEObjectDescription;

import com.puppetlabs.geppetto.pp.DefinitionArgument;
import com.puppetlabs.geppetto.pp.dsl.linking.PPTask;
import com.puppetlabs.geppetto.pp.pptp.PuppetType;

/**
 * Constants for PP DSL
 */
public interface PPDSLConstants {
	public static final String DSL_PLUGIN_NAME = "com.puppetlabs.geppetto.pp.dsl";

	public static final String PP_DEBUG_LINKER = DSL_PLUGIN_NAME + "/debug/linker";

	public static final String PPTP_LANGUAGE_NAME = "com.puppetlabs.geppetto.pp.dsl.PPTP";

	public static final String PPTP_EXTENSION = "pptp";

	/**
	 * Key used in IEObjectDescription data for parent name (i.e. inheritance).
	 */
	public static final String PARENT_NAME_DATA = "com.puppetlabs.geppetto.pp.dsl.parent";

	/**
	 * Property key for Resource Properties. This key should refer to an EMF URI that denotes the
	 * root to use when relativizing URI/paths in reported diagnostic.
	 */
	public static final String RESOURCE_PROPERTY__ROOT_URI = "com.puppetlabs.geppetto.pp.dsl.resource.rootUri";

	/**
	 * Property key for Resource Properties. This key should refer to a Collection of {@link PPTask}.
	 */
	public static final String RESOURCE_PROPERTY__TASK_LIST = "com.puppetlabs.geppetto.pp.dsl.resource.taskList";

	/**
	 * Property key for Puppet Types. This key should refer to a Map of String => {@link PuppetType} associations.
	 */
	public static final String RESOURCE_PROPERTY__TYPE_MAP = "com.puppetlabs.geppetto.pp.dsl.resource.typeMap";

	/**
	 * If present in an {@link IEObjectDescription} for a PPTP Parameter data and set to true, then this
	 * parameter is a PP DSL namevar.
	 */
	public static final String PARAMETER_NAMEVAR = "com.puppetlabs.geppetto.pptp.parameter.namevar";

	/**
	 * If present in an {@link IEObjectDescription} for a PPTP 'Element' data and set to true, this entry
	 * is deprecated.
	 */
	public static final String ELEMENT_DEPRECATED = "com.puppetlabs.geppetto.pptp.element.deprecated";

	/**
	 * If present in an {@link IEObjectDescription} for a {@link DefinitionArgument} this data element
	 * contains the source text for the default value expression.
	 */
	public static final String DEFAULT_EXPRESSION_DATA = "com.puppetlabs.geppetto.pp.dsl.default";

	/**
	 * If present in an {@link IEObjectDescription} for a PPTP 'TPVariable' data the name is a prefix, and the rest of
	 * the name must match the given regular expression pattern mapped to this key.
	 */
	public static final String VARIABLE_PATTERN = "com.puppetlabs.geppetto.pptp.variable.pattern";

	/**
	 * Published as data in the build index for a HostClassDefinition
	 */
	public static final String CLASS_ARG_COUNT = "com.puppetlabs.geppetto.pp.dsl.class.argcount";

}
