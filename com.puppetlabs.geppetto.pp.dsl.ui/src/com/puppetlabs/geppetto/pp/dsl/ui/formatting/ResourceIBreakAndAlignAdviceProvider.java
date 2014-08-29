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
package com.puppetlabs.geppetto.pp.dsl.ui.formatting;

import org.eclipse.core.resources.IResource;

import com.google.inject.Inject;
import com.puppetlabs.geppetto.pp.dsl.formatting.IBreakAndAlignAdvice;
import com.puppetlabs.geppetto.pp.dsl.formatting.IBreakAndAlignAdvice.WhenToApply;
import com.puppetlabs.geppetto.pp.dsl.formatting.IBreakAndAlignAdvice.WhenToApplyForDefinition;
import com.puppetlabs.geppetto.pp.dsl.ui.preferences.data.BreakAndAlignPreferences;
import com.puppetlabs.xtext.ui.resource.PlatformResourceSpecificProvider;

/**
 * A {@link Provider} of {@link IBreakAndAlignAdvice} that can look up information specific to the current
 * resource.
 */
public class ResourceIBreakAndAlignAdviceProvider extends PlatformResourceSpecificProvider<IBreakAndAlignAdvice> {
	@Inject
	private BreakAndAlignPreferences formatterPreferences;

	@Override
	protected IBreakAndAlignAdvice dataForResource(IResource resource) {
		final int clusterSize = formatterPreferences.getClusterSize(resource);
		final WhenToApplyForDefinition definitionParameters = formatterPreferences.getDefinitionParametersAdvice(resource);
		final WhenToApply hashes = formatterPreferences.getHashesAdvice(resource);
		final WhenToApply lists = formatterPreferences.getListsAdvice(resource);
		final boolean compact = formatterPreferences.isCompactCases(resource);
		final boolean alignCases = formatterPreferences.isAlignCases(resource);
		final boolean compactResource = formatterPreferences.isCompactResources(resource);
		final boolean alignAssignments = formatterPreferences.isAlignAssignments(resource);

		return new IBreakAndAlignAdvice() {

			@Override
			public int clusterSize() {
				return clusterSize;
			}

			@Override
			public boolean compactCasesWhenPossible() {
				return compact;
			}

			@Override
			public boolean compactResourceWhenPossible() {
				return compactResource;
			}

			@Override
			public WhenToApplyForDefinition definitionParameterListAdvice() {
				return definitionParameters;
			}

			@Override
			public WhenToApply hashesAdvice() {
				return hashes;
			}

			@Override
			public boolean isAlignAssignments() {
				return alignAssignments;
			}

			@Override
			public boolean isAlignCases() {
				return alignCases;
			}

			@Override
			public WhenToApply listsAdvice() {
				return lists;
			}
		};
	}

}
