/**
 * Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   itemis AG - initial API and implementation
 *   Puppet Labs
 *
 */
package com.puppetlabs.geppetto.pp.dsl.ui.editor.findrefs;

import com.puppetlabs.geppetto.pp.dsl.ui.editor.findrefs.PPReferenceFinder.IPPQueryData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

import com.google.inject.Inject;

/**
 * Adaption of class with similar name in Xtext. Adapted to PP linking based on IEObjectDescription.
 */
public class PPReferenceQuery implements ISearchQuery {

	@Inject
	private PPReferenceFinder finder;

	@Inject
	protected EditorResourceAccess localContextProvider;

	private PPReferenceSearchResult searchResult;

	private IPPQueryData queryData;

	public PPReferenceQuery() {
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	protected PPReferenceSearchResult createSearchResult() {
		return new PPReferenceSearchResult(this);
	}

	@Override
	public String getLabel() {
		return queryData.getLabel();
	}

	@Override
	public ISearchResult getSearchResult() {
		return searchResult;
	}

	public void init(IPPQueryData queryData) {
		this.queryData = queryData;
		this.searchResult = createSearchResult();
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		searchResult.reset();
		finder.findAllReferences(queryData, localContextProvider, searchResult, monitor);
		searchResult.finish();
		return (monitor.isCanceled())
			? Status.CANCEL_STATUS
			: Status.OK_STATUS;
	}
}
