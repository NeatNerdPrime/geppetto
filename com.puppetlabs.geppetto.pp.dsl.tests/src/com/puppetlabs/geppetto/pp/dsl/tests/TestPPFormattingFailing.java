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
package com.puppetlabs.geppetto.pp.dsl.tests;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.formatting.IIndentationInformation;
import org.eclipse.xtext.junit4.serializer.DebugSequenceAcceptor;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.serializer.diagnostic.ISerializationDiagnostic.Acceptor;
import org.eclipse.xtext.serializer.sequencer.IHiddenTokenSequencer;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.ReplaceRegion;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.puppetlabs.geppetto.pp.AssignmentExpression;
import com.puppetlabs.geppetto.pp.PuppetManifest;
import com.puppetlabs.geppetto.pp.dsl.formatting.PPSemanticLayout;
import com.puppetlabs.geppetto.pp.dsl.formatting.PPStylesheetProvider;
import com.puppetlabs.geppetto.pp.dsl.ppformatting.PPIndentationInformation;
import com.puppetlabs.geppetto.pp.dsl.tests.utils.DebugHiddenTokenSequencer;
import com.puppetlabs.xtext.dommodel.IDomNode;
import com.puppetlabs.xtext.dommodel.formatter.CSSDomFormatter;
import com.puppetlabs.xtext.dommodel.formatter.DomNodeLayoutFeeder;
import com.puppetlabs.xtext.dommodel.formatter.IDomModelFormatter;
import com.puppetlabs.xtext.dommodel.formatter.ILayoutManager;
import com.puppetlabs.xtext.dommodel.formatter.context.IFormattingContext;
import com.puppetlabs.xtext.dommodel.formatter.css.DomCSS;
import com.puppetlabs.xtext.serializer.DomBasedSerializer;

/**
 * Triggers problems with computation of the current hidden state during sequencing.
 *
 */
public class TestPPFormattingFailing extends AbstractPuppetTests {
	public static class DebugFormatter extends CSSDomFormatter {

		@Inject
		public DebugFormatter(Provider<DomCSS> domProvider, DomNodeLayoutFeeder feeder) {
			super(domProvider, feeder);
		}

		@Override
		public ReplaceRegion format(IDomNode dom, ITextRegion regionToFormat, IFormattingContext formattingContext,
				Acceptor errors) {
			// System.err.println("TestSemanticCssFormatter.DebugFormatter");
			// System.err.println(DomModelUtils.compactDump(dom, true));
			return super.format(dom, regionToFormat, formattingContext, errors);
		}

	}

	public static class DebugHiddenProvider implements Provider<IHiddenTokenSequencer> {

		@Override
		public IHiddenTokenSequencer get() {
			DebugHiddenTokenSequencer result = new DebugHiddenTokenSequencer();
			TestPPFormattingFailing.theDebugAcceptor = result;
			return result;
		}

	}

	// /**
	// * Advices a hidden token sequencer to *not* save and restore the hidden state when processing
	// * rulecalls.
	// * TODO: No longer needed
	// */
	// public static class NonSaveRestorHiddenStateAdvise implements IHiddenTokenSequencerAdvisor {
	//
	// @Override
	// public boolean shouldSaveRestoreState() {
	// return false;
	// }
	//
	// }
	public class TestDebugModule extends TestModule {

		@Override
		public void configureIHiddenTokenSequencer(Binder binder) {
			binder.bind(IHiddenTokenSequencer.class).toProvider(DebugHiddenProvider.class);
			// binder.bind(IHiddenTokenSequencerAdvisor.class).to(NonSaveRestorHiddenStateAdvise.class);
		}
	}

	public class TestModule extends PPTestModule {

		@Override
		public void configure(Binder binder) {
			super.configure(binder);
			binder.bind(ISerializer.class).to(DomBasedSerializer.class);
			binder.bind(IDomModelFormatter.class).to(DebugFormatter.class);
			// Want serializer to insert empty WS even if there is no node model
			// binder.bind(IHiddenTokenSequencer.class).to(
			// com.puppetlabs.xtext.serializer.acceptor.HiddenTokenSequencer.class);

			// Bind the default style sheet (TODO: should use a test specific sheet)
			binder.bind(DomCSS.class).toProvider(PPStylesheetProvider.class);
			// specific to pp (2 spaces).
			binder.bind(IIndentationInformation.class).to(PPIndentationInformation.class);
			// binder.bind(IIndentationInformation.class).to(IIndentationInformation.Default.class);

			binder.bind(ILayoutManager.class).annotatedWith(Names.named("Default")).to(PPSemanticLayout.class);

			// configureIHiddenTokenSequencer(binder);
		}

		public void configureIHiddenTokenSequencer(Binder binder) {
			// bind the real HiddenTokenSequencer
			binder.bind(IHiddenTokenSequencer.class).to(
				com.puppetlabs.xtext.serializer.acceptor.HiddenTokenSequencer.class);
			// advise it to not save / restore hidden state to trigger error
			// binder.bind(IHiddenTokenSequencerAdvisor.class).to(NonSaveRestorHiddenStateAdvise.class);

		}
	}

	public static DebugSequenceAcceptor theDebugAcceptor;

	private String doubleQuote(String s) {
		return '"' + s + '"';
	}

	@Override
	public Module getTestModule() {
		// Produces debug output for the hidden sequencer
		// return new TestSetupDebugOutput();

		// Runs with configuration that shows problem
		return new TestModule();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		with(getSetupInstance());
	}

	/**
	 * Don't want to test serializer as this test barfs on some PP constructs (with and without serialization
	 * is not the same result).
	 */
	@Override
	protected boolean shouldTestSerializer(XtextResource resource) {
		// true here (the default), just makes testing slower and it intermittently fails ?!?
		return false;
	}

	/**
	 * Due to issues in the formatter, this test may hit a bug that inserts whitespace
	 * between quotes and string - no workaround found - needs to be fixed in Xtext formatter.
	 * Also see {@link #test_Serialize_DoubleQuotedString_2()}
	 *
	 * @throws Exception
	 */
	@Test
	public void test_Serialize_DoubleQuotedString_1() throws Exception {
		// String original = "before${var}/after${1+2}$$${$var}";
		// String formatted = doubleQuote("before${var}/after${1 + 2}$$${$var}");
		String code = "$a = " + doubleQuote("${1+2}");
		String formatted = "$a = " + doubleQuote("${1 + 2}");
		formatted += "\n";
		XtextResource r = getResourceFromString(code);
		EObject result = r.getContents().get(0);
		assertTrue("Should be a PuppetManifest", result instanceof PuppetManifest);
		result = ((PuppetManifest) result).getStatements().get(0);
		assertTrue("Should be a DoubleQuotedString", result instanceof AssignmentExpression);

		String s = serializeFormatted(r.getContents().get(0));
		if(theDebugAcceptor != null)
			System.err.println(theDebugAcceptor.toString());
		assertEquals("Serialization of interpolated string should produce same result", formatted, s);
	}
}
