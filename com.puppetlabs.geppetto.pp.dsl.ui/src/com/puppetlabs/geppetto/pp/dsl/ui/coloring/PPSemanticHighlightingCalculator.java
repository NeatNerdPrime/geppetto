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
package com.puppetlabs.geppetto.pp.dsl.ui.coloring;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.puppetlabs.geppetto.pp.AttributeOperation;
import com.puppetlabs.geppetto.pp.CollectExpression;
import com.puppetlabs.geppetto.pp.Expression;
import com.puppetlabs.geppetto.pp.ExpressionTE;
import com.puppetlabs.geppetto.pp.LiteralList;
import com.puppetlabs.geppetto.pp.LiteralNameOrReference;
import com.puppetlabs.geppetto.pp.PPPackage;
import com.puppetlabs.geppetto.pp.ParenthesisedExpression;
import com.puppetlabs.geppetto.pp.PuppetManifest;
import com.puppetlabs.geppetto.pp.ResourceBody;
import com.puppetlabs.geppetto.pp.ResourceExpression;
import com.puppetlabs.geppetto.pp.dsl.PPDSLConstants;
import com.puppetlabs.geppetto.pp.dsl.adapters.ResourceDocumentationAdapter;
import com.puppetlabs.geppetto.pp.dsl.adapters.ResourceDocumentationAdapterFactory;
import com.puppetlabs.geppetto.pp.dsl.adapters.ResourcePropertiesAdapter;
import com.puppetlabs.geppetto.pp.dsl.adapters.ResourcePropertiesAdapterFactory;
import com.puppetlabs.geppetto.pp.dsl.linking.PPTask;
import com.puppetlabs.geppetto.pp.dsl.ppdoc.PPDocumentationParser;
import com.puppetlabs.geppetto.pp.dsl.ppdoc.PPDocumentationParser.DocNode;
import com.puppetlabs.geppetto.pp.dsl.services.PPGrammarAccess;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.BidiTreeIterable;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.syntaxcoloring.DefaultHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.eclipse.xtext.util.Exceptions;
import org.eclipse.xtext.util.PolymorphicDispatcher;

import com.google.inject.Inject;

/**
 * Highlighting for puppet.
 */
public class PPSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {
	private static class ZeroLengthFilteredAcceptorWrapper implements IHighlightedPositionAcceptor {
		private IHighlightedPositionAcceptor wrapped;

		public ZeroLengthFilteredAcceptorWrapper(IHighlightedPositionAcceptor wrapped) {
			this.wrapped = wrapped;
		}

		// @Override
		@Override
		public void addPosition(int offset, int length, String... id) {
			// FOR DEBUGGING
			// StringBuffer buf = new StringBuffer();
			// buf.append(offset);
			// buf.append(", ");
			// buf.append(length);
			// for(String s : id)
			// buf.append(", ").append(s);
			// buf.append("\n");
			// System.err.print(buf.toString());
			if(length == 0)
				return;
			if(length < 0)
				return;
			wrapped.addPosition(offset, length, id);
		}

	}

	@Inject
	private PPDocumentationParser docParser;

	private PPGrammarAccess grammarAccess;

	private AbstractRule ruleVariable;

	private AbstractRule ruleSqText;

	private AbstractRule ruleDQ_STRING;

	private AbstractRule ruleExpression;

	private AbstractRule ruleUNION_VARIABLE_OR_NAME;

	private AbstractRule ruleUnionNameOrReference;

	private final PolymorphicDispatcher<Void> highlightDispatcher = new PolymorphicDispatcher<Void>(
		"highlight", 2, 2, Collections.singletonList(this), new PolymorphicDispatcher.ErrorHandler<Void>() {
			@Override
			public Void handle(Object[] params, Throwable e) {
				handleError(params, e);
				return null;
			}
		});

	@Inject
	public PPSemanticHighlightingCalculator(IGrammarAccess grammarAccess) {
		this.grammarAccess = (PPGrammarAccess) grammarAccess;
		// get rules from grammar access to make comparisions of rule calls faster
		setupRules();
	}

	public void doHighlight(Object o, IHighlightedPositionAcceptor acceptor) {
		highlightDispatcher.invoke(o, acceptor);
	}

	protected void handleError(Object[] params, Throwable e) {
		Exceptions.throwUncheckedException(e);
	}

	public void highlight(AttributeOperation expr, IHighlightedPositionAcceptor acceptor) {
		List<INode> nodes = NodeModelUtils.findNodesForFeature(expr, PPPackage.Literals.ATTRIBUTE_OPERATION__KEY);
		for(INode n : nodes) {
			if(n instanceof ICompositeNode) {
				for(INode n2 : ((ICompositeNode) n).getLeafNodes()) {
					if(n2.getGrammarElement() instanceof Keyword)
						acceptor.addPosition(n2.getOffset(), n2.getLength(), DefaultHighlightingConfiguration.DEFAULT_ID);
				}
			}
		}
	}

	public void highlight(CollectExpression expr, IHighlightedPositionAcceptor acceptor) {
		Expression classReference = expr.getClassReference();
		if(classReference != null) {
			highlightObject(classReference, PPHighlightConfiguration.RESOURCE_REF_ID, acceptor);
		}
	}

	/**
	 * A default 'do nothing' highlighting handler
	 */
	public void highlight(EObject o, IHighlightedPositionAcceptor acceptor) {
		// DO NOTHING

		// Uncomment next For debugging, and seeing opportunities for syntax highlighting
		// System.err.println("Missing highlight() method for: "+ o.getClass().getSimpleName());
	}

	public void highlight(INode o, IHighlightedPositionAcceptor acceptor) {
		// what in grammar created this node
		EObject gElem = o.getGrammarElement();

		ruleCall: if(gElem instanceof RuleCall) {
			AbstractRule rule = ((RuleCall) gElem).getRule();
			// String ruleName = ((RuleCall) gElem).getRule().getName();

			if(rule == null)
				break ruleCall;

			// need to set default since keywords may be included
			// TODO: should be fixed by modifying the highligtinglexer
			if(ruleUnionNameOrReference == rule)
				acceptor.addPosition(o.getOffset(), o.getLength(), DefaultHighlightingConfiguration.DEFAULT_ID);

			if(ruleVariable == rule || ruleUNION_VARIABLE_OR_NAME == rule)
				acceptor.addPosition(o.getOffset(), o.getLength(), PPHighlightConfiguration.VARIABLE_ID);
			else if(ruleSqText == rule)
				acceptor.addPosition(o.getOffset(), o.getLength(), DefaultHighlightingConfiguration.STRING_ID);

			else if(/* ruleDQT_DOLLAR == rule || ruleDQT_QUOTE == rule || */ruleDQ_STRING == rule)
				acceptor.addPosition(o.getOffset(), o.getLength(), PPHighlightConfiguration.TEMPLATE_TEXT_ID);
			else if(ruleExpression == rule) {
				EObject semantic = o.getSemanticElement();
				if(semantic instanceof ExpressionTE) {
					Expression expr = ((ExpressionTE) semantic).getExpression();
					expr = ((ParenthesisedExpression) expr).getExpr();
					if(expr instanceof LiteralNameOrReference)
						acceptor.addPosition(
							o.getOffset(), ((LiteralNameOrReference) expr).getValue().length() + 3, PPHighlightConfiguration.VARIABLE_ID);

				}
			}
		}
		if(gElem instanceof Keyword) {
			if(((Keyword) gElem).getValue().equals("\""))
				acceptor.addPosition(o.getOffset(), o.getLength(), DefaultHighlightingConfiguration.STRING_ID);
		}
	}

	public void highlight(PuppetManifest model, IHighlightedPositionAcceptor acceptor) {
		TreeIterator<EObject> all = model.eAllContents();
		while(all.hasNext())
			doHighlight(all.next(), acceptor);
	}

	public void highlight(ResourceExpression expr, IHighlightedPositionAcceptor acceptor) {
		EObject resourceExpr = expr.getResourceExpr();
		if(resourceExpr != null) {
			TreeIterator<EObject> all = resourceExpr.eAllContents();
			int counter = 0;
			while(all.hasNext()) {
				counter++;
				EObject x = all.next();
				if(x instanceof LiteralNameOrReference)
					highlightObject(x, PPHighlightConfiguration.RESOURCE_REF_ID, acceptor);
			}
			if(counter < 1)
				highlightObject(resourceExpr, PPHighlightConfiguration.RESOURCE_REF_ID, acceptor);
		}
		for(ResourceBody body : expr.getResourceData()) {
			if(body.getNameExpr() != null) {
				Expression nameExpr = body.getNameExpr();
				ICompositeNode node = NodeModelUtils.getNode(nameExpr);
				int offset = node.getOffset();
				int length = node.getLength();
				// if the name is a list of names, skip the opening and closing brackets
				if(nameExpr instanceof LiteralList) {
					offset++;
					length -= Math.min(2, length);
				}
				if(node != null) {
					acceptor.addPosition(offset, length, PPHighlightConfiguration.RESOURCE_TITLE_ID);
				}
			}
		}
	}

	private String highlightIDForDocStyle(int docStyle) {
		switch(docStyle) {
			case PPDocumentationParser.HEADING_1:
				return PPHighlightConfiguration.DOC_HEADING1_ID;
			case PPDocumentationParser.HEADING_2:
				return PPHighlightConfiguration.DOC_HEADING2_ID;
			case PPDocumentationParser.HEADING_3:
				return PPHighlightConfiguration.DOC_HEADING3_ID;
			case PPDocumentationParser.HEADING_4:
				return PPHighlightConfiguration.DOC_HEADING4_ID;
			case PPDocumentationParser.HEADING_5:
				return PPHighlightConfiguration.DOC_HEADING5_ID;
			case PPDocumentationParser.BOLD:
				return PPHighlightConfiguration.DOC_BOLD_ID;
			case PPDocumentationParser.ITALIC:
				return PPHighlightConfiguration.DOC_ITALIC_ID;

			case PPDocumentationParser.FIXED: // fall through
			case PPDocumentationParser.VERBATIM:
				return PPHighlightConfiguration.DOC_FIXED_ID;
			case PPDocumentationParser.COMMENT:
				return DefaultHighlightingConfiguration.COMMENT_ID;
			case PPDocumentationParser.PLAIN:
				return PPHighlightConfiguration.DOC_PLAIN_ID;
			default: // fall through
				return PPHighlightConfiguration.DOCUMENTATION_ID;
		}
	}

	private void highlightObject(EObject semantic, String highlightID, IHighlightedPositionAcceptor acceptor) {
		INode node = NodeModelUtils.getNode(semantic);
		if(node == null) {
			// TODO: WARNING - no node
			return;
		}
		acceptor.addPosition(node.getOffset(), node.getLength(), highlightID);
	}

	public boolean isSpecialSpace(char c) {
		switch(c) {
			case '\u00A0': // NBSP
			case '\u1680': // OGHAM SPACE MARK");
			case '\u2000': // EN QUAD");
			case '\u2001': // EM QUAD");
			case '\u2002': // EN SPACE");
			case '\u2003': // EM SPACE");
			case '\u2004': // THREE-PER-EM SPACE");
			case '\u2005': // FOUR-PER-EM SPACE");
			case '\u2006': // SIX-PER-EM SPACE");
			case '\u2007': // FIGURE SPACE");
			case '\u2008': // PUNCTUATION SPACE");
			case '\u2009': // THIN SPACE");
			case '\u200A': // HAIR SPACE");
			case '\u200B': // ZERO WIDTH SPACE");
			case '\u202F': // NARROW NO-BREAK SPACE");
			case '\u3000': // IDEOGRAPHIC SPACE");
				return true;
		}
		return false;
	}

	@Override
	public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		if(resource == null)
			return;
		acceptor = new ZeroLengthFilteredAcceptorWrapper(acceptor);
		// highligting based on inspection of parser nodes
		provideNodeBasedHighlighting(resource, acceptor);

		// highlighting based on created model
		provideSemanticHighlighting(resource, acceptor);

		// highligting based on the text itself
		provideTextualHighlighting(resource, acceptor);

		provideResourceLevelHighlighting(resource, acceptor);
	}

	/**
	 * Iterate over parser nodes and provide highlighting based on rule calls.
	 *
	 * @param resource
	 * @param acceptor
	 */
	protected void provideNodeBasedHighlighting(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		BidiTreeIterable<INode> allNodes = resource.getParseResult().getRootNode().getAsTreeIterable();
		for(INode node : allNodes) {
			EObject gElem = node.getGrammarElement();
			if(gElem instanceof RuleCall) {
				highlight(node, acceptor);
			}
			else if(gElem instanceof Keyword) {
				highlight(node, acceptor);
			}
		}
	}

	protected void provideResourceLevelHighlighting(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		ResourcePropertiesAdapter adapter = ResourcePropertiesAdapterFactory.eINSTANCE.adapt(resource);
		@SuppressWarnings("unchecked")
		List<PPTask> taskList = (List<PPTask>) adapter.get(PPDSLConstants.RESOURCE_PROPERTY__TASK_LIST);
		if(taskList == null)
			return;
		for(PPTask task : taskList) {
			acceptor.addPosition(task.getOffset(), task.getLength(), PPHighlightConfiguration.TASK_ID);
		}

		ResourceDocumentationAdapter docAdapter = ResourceDocumentationAdapterFactory.eINSTANCE.adapt(resource);
		if(docAdapter != null) {
			Map<EObject, List<INode>> associations = docAdapter.getAssociations();
			for(List<INode> sequence : associations.values()) {
				List<DocNode> docNodes = docParser.parse(sequence);
				for(DocNode dn : docNodes) {
					acceptor.addPosition(dn.getOffset(), dn.getLength(), highlightIDForDocStyle(dn.getStyle()));
				}

			}
		}
	}

	/**
	 * Iterate over the generated model and provide highlighting
	 *
	 * @param resource
	 * @param acceptor
	 */
	private void provideSemanticHighlighting(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		EList<EObject> contents = resource.getContents();
		if(contents == null || contents.size() == 0)
			return; // nothing there at all - probably an empty file
		PuppetManifest model = (PuppetManifest) contents.get(0);
		doHighlight(model, acceptor);
	}

	public void provideTextualHighlighting(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		ICompositeNode root = resource.getParseResult().getRootNode();
		String text = root.getText();
		int limit = text.length();
		for(int i = 0; i < limit; i++)
			if(isSpecialSpace(text.charAt(i)))
				acceptor.addPosition(i, 1, PPHighlightConfiguration.SPECIAL_SPACE_ID);
	}

	/**
	 * Set up rules for faster comparison
	 */
	private void setupRules() {
		ruleVariable = grammarAccess.getDollarVariableRule();
		ruleSqText = grammarAccess.getSqTextRule();
		ruleDQ_STRING = grammarAccess.getDoubleStringCharactersRule();
		ruleExpression = grammarAccess.getTextExpressionRule();
		ruleUNION_VARIABLE_OR_NAME = grammarAccess.getUNION_VARIABLE_OR_NAMERule();
		ruleUnionNameOrReference = grammarAccess.getUnionNameOrReferenceRule();
	}
}
