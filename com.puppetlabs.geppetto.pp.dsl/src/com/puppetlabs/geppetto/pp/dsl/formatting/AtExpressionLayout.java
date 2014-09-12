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
package com.puppetlabs.geppetto.pp.dsl.formatting;

import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;

import com.google.inject.Inject;
import com.puppetlabs.geppetto.pp.AtExpression;
import com.puppetlabs.geppetto.pp.dsl.services.PPGrammarAccess;
import com.puppetlabs.geppetto.pp.dsl.services.PPGrammarAccess.AtExpressionElements;
import com.puppetlabs.xtext.dommodel.DomModelUtils;
import com.puppetlabs.xtext.dommodel.IDomNode;
import com.puppetlabs.xtext.dommodel.formatter.ILayoutManager.ILayoutContext;
import com.puppetlabs.xtext.dommodel.formatter.css.IStyleFactory;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleSet;
import com.puppetlabs.xtext.textflow.ITextFlow;

/**
 * <p>
 * Performs semantic layout on a AtExpression in combination with text-fit check.
 * </p>
 * <p>
 * if the AtExpression list does not fit on the same line, line breaks are added to the whitespace after all commas except the optional end
 * comma.
 * </p>
 * <p>
 * The styling is assigned to the nodes directly to override all other rule based styling. Indentation is expected to be handled by default
 * rules.
 * </p>
 */
public class AtExpressionLayout extends AbstractListLayout {
	@Inject
	private IStyleFactory styles;

	@Inject
	private PPGrammarAccess grammarAccess;

	@Override
	protected AbstractElement getLastSignificantGrammarElement() {
		return grammarAccess.getAtExpressionAccess().getRightSquareBracketKeyword_1_3();
	}

	@Override
	protected boolean hasMoreThanOneElement(EObject semantic) {
		return ((AtExpression) semantic).getParameters().size() > 1;
	}

	@Override
	protected void markup(IDomNode node, final boolean breakAndAlign, final int clusterWidth, ITextFlow flow, ILayoutContext context) {

		Iterator<IDomNode> itor = node.treeIterator();

		AtExpressionElements access = grammarAccess.getAtExpressionAccess();

		while(itor.hasNext()) {
			IDomNode n = itor.next();
			EObject ge = n.getGrammarElement();
			if(ge == access.getLeftSquareBracketKeyword_1_1()) {
				IDomNode nextLeaf = DomModelUtils.nextWhitespace(n);
				if(DomModelUtils.isWhitespace(nextLeaf) && breakAndAlign)
					nextLeaf.getStyles().add(StyleSet.withStyles(styles.oneLineBreak()));
			}
			else if(breakAndAlign && ge == access.getCommaKeyword_1_2_1_0()) {
				IDomNode nextLeaf = DomModelUtils.nextWhitespace(n);
				if(DomModelUtils.isWhitespace(nextLeaf))
					nextLeaf.getStyles().add(StyleSet.withStyles(styles.oneLineBreak()));
			}
		}
	}

}
