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
package com.puppetlabs.geppetto.forge.v2;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.puppetlabs.geppetto.forge.client.ForgeHttpModule;
import com.puppetlabs.geppetto.forge.client.GsonModule;
import com.puppetlabs.geppetto.forge.model.Constants;
import com.puppetlabs.geppetto.forge.model.MetadataRepository;
import com.puppetlabs.geppetto.forge.v2.service.ReleaseService;

/**
 * This is the main entry point to the API. Sample usage:
 *
 * <pre>
 * // Obtain preferences in some way. Command line options, property settings, etc.
 * ForgeAPIPreferences prefs = ...;
 * 
 * Injector injector = Guice.createInjector(new ForgeHttpModule(prefs));
 * 
 * // Create a new forge instance
 * ForgeAPI forge = new ForgeAPI(injector);
 * 
 * // Use the forge instance to create a service.
 * DefaultModuleService moduleService = forge.createModuleService();
 * 
 * // Use the service
 * List&lt;Release&gt; stdLibReleases = moduleService.getReleases("puppetlabs", "stdlib", null);
 * </pre>
 */
public class ForgeAPI {
	private final Injector injector;

	/**
	 * Creates a default read-only client. Since this client has no credentials, it will not
	 * be able to publish.
	 */
	public ForgeAPI() {
		this.injector = Guice.createInjector(GsonModule.INSTANCE, new ForgeHttpModule() {

			@Override
			protected String getBaseURL() {
				return Constants.FORGE_SERVICE_BASE_URL;
			}
		});
	}

	/**
	 * Create a new instance based on a set of Guice modules.
	 *
	 * @param modules
	 *            Guice modules that provides the needed bindings.
	 */
	public ForgeAPI(Module... modules) {
		this.injector = Guice.createInjector(modules);
	}

	/**
	 * Creates a {@link Gson} instance that is configured in accordance with
	 * the API.
	 *
	 * @return A new instance.
	 */
	public Gson createGson() {
		return injector.getInstance(Gson.class);
	}

	/**
	 * Creates a service that can be used when interacting with the ForgeAPI as
	 * a metadata repository. This service is used by the transitive dependency
	 * resolver.
	 *
	 * @return The new metadata service
	 */
	public MetadataRepository createMetadataRepository() {
		return injector.getInstance(MetadataRepository.class);
	}

	/**
	 * Creates a new service that can be used when creating, updating, browsing, or
	 * deleting module releases.
	 *
	 * @return The new release service.
	 */
	public ReleaseService createReleaseService() {
		return injector.getInstance(ReleaseService.class);
	}
}
