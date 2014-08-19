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

import java.io.StringReader;

import com.puppetlabs.geppetto.pp.LiteralNameOrReference;
import com.puppetlabs.geppetto.pp.LiteralRegex;
import com.puppetlabs.geppetto.pp.PuppetManifest;
import com.puppetlabs.geppetto.pp.ResourceExpression;
import com.puppetlabs.geppetto.pp.SingleQuotedString;
import com.puppetlabs.geppetto.pp.dsl.parser.antlr.PPParser;
import com.puppetlabs.geppetto.pp.dsl.services.PPGrammarAccess;
import com.puppetlabs.geppetto.pp.dsl.validation.IPPDiagnostics;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.IParseResult;
import org.junit.Test;

/**
 * Tests Literals
 *
 */
public class TestLiterals extends AbstractPuppetTests {

	public final static String[] validNames = {
		"file", "File", "::File", "A::B::C", "class", "::or", "a::or", "::or-or" };

	public final static String[] invalidNames = { "if", "else", "%#", "define:" };

	// private String doubleQuote(String s) {
	// return '"' + s + '"';
	// }

	@Test
	public void test_Parse_LiteralNameOrReference_NotOk() {
		PPGrammarAccess ga = (PPGrammarAccess) getGrammarAccess();
		PPParser parser = (PPParser) getParser();
		for(String s : invalidNames) {
			IParseResult result = parser.parse(ga.getUnionNameOrReferenceRule(), new StringReader(s));
			assertTrue("Should have errors for: " + s, result.hasSyntaxErrors());
		}

	}

	@Test
	public void test_Parse_LiteralNameOrReference_Ok() {
		PPGrammarAccess ga = (PPGrammarAccess) getGrammarAccess();
		PPParser parser = (PPParser) getParser();
		for(String s : validNames) {
			IParseResult result = parser.parse(ga.getUnionNameOrReferenceRule(), new StringReader(s));
			assertFalse("Should not have errors for: " + s, result.hasSyntaxErrors());
			assertEquals("parsed should be same as input", s, result.getRootNode().getText());
		}
		for(String s : validNames) {
			if("class".equals(s))
				continue; // 'class' alone is not a valid value expression
			IParseResult result = parser.parse(ga.getExpressionRule(), new StringReader(s));
			assertFalse("Should not have errors for: " + s, result.hasSyntaxErrors());
			assertEquals("parsed should be same as input", s, result.getRootNode().getText());
			EObject root = result.getRootASTElement();
			// if("class".equals(s)) {
			// assertTrue("Should be ResourceExpression", root instanceof ResourceExpression);
			// }
			// else {
			assertTrue("Should be LiteralNameOrReference", root instanceof LiteralNameOrReference);
			assertEquals("Literal should be same as input", s, ((LiteralNameOrReference) root).getValue());
			// }
		}
		for(String s : validNames) {
			if("class".equals(s))
				continue; // 'class' alone is not a balid value expression
			IParseResult result = parser.parse(ga.getPuppetManifestRule(), new StringReader(s));
			assertFalse("Should not have errors for: " + s, result.hasSyntaxErrors());
			assertEquals("parsed should be same as input", s, result.getRootNode().getText());
			EObject root = result.getRootASTElement();
			assertTrue("Should be PuppetManifest", root instanceof PuppetManifest);
			PuppetManifest pm = (PuppetManifest) root;
			assertTrue("Manifest should have statements", pm.getStatements().size() > 0);
			EObject expr = pm.getStatements().get(0);
			// if("class".equals(s)) {
			// assertTrue("Should be ResourceExpression", expr instanceof ResourceExpression);
			// }
			// else {
			assertTrue("Should be LiteralNameOrReference", expr instanceof LiteralNameOrReference);
			assertEquals("Literal should be same as input", s, ((LiteralNameOrReference) expr).getValue());
			// }

		}

	}

	@Test
	public void test_Parse_LiteralsInResource_Smoketest() {
		PPGrammarAccess ga = (PPGrammarAccess) getGrammarAccess();
		PPParser parser = (PPParser) getParser();

		String s = "File { mode => 666 }";
		IParseResult result = parser.parse(ga.getPuppetManifestRule(), new StringReader(s));
		assertFalse("Should not have errors for: " + s, result.hasSyntaxErrors());
		assertEquals("parsed should be same as input", s, result.getRootNode().getText());
		EObject root = result.getRootASTElement();
		assertTrue("Should be PuppetManifest", root instanceof PuppetManifest);
		PuppetManifest pm = (PuppetManifest) root;
		assertTrue("Manifest should have statements", pm.getStatements().size() > 0);
		EObject expr = pm.getStatements().get(0);
		assertInstanceOf("Should be ResourceExpression", ResourceExpression.class, expr);
		ResourceExpression resourceExpression = (ResourceExpression) expr;
		assertInstanceOf(
			"Should be LiteralNameOrReference", LiteralNameOrReference.class, resourceExpression.getResourceExpr());
		LiteralNameOrReference name = (LiteralNameOrReference) resourceExpression.getResourceExpr();
		assertEquals("Literal should be same as input", "File", name.getValue());

	}

	@Test
	public void test_Validate_LiteralNameOrReference_NotOk() {
		final String[] keywords = {
				"and", "or", "case", "default", "define", "import", "if", "elsif", "else", "inherits", "node", "in",
				"undef", "true", "false" };
		for(String s : keywords) {
			LiteralNameOrReference n = createNameOrReference(s);
			tester.validator().checkLiteralNameOrReference(n);
			tester.diagnose().assertError(IPPDiagnostics.ISSUE__RESERVED_WORD);
		}
		final String[] invalidNames = { "%#", "::::", "::a::::" };
		for(String s : invalidNames) {
			LiteralNameOrReference n = createNameOrReference(s);
			tester.validator().checkLiteralNameOrReference(n);
			tester.diagnose().assertError(IPPDiagnostics.ISSUE__NOT_NAME_OR_REF);
		}
	}

	@Test
	public void test_Validate_LiteralNameOrReference_Ok() {
		for(String s : validNames) {
			LiteralNameOrReference n = createNameOrReference(s);
			tester.validator().checkLiteralNameOrReference(n);
			tester.diagnose().assertOK();
		}
	}

	@Test
	public void test_Validate_LiteralRegex_NotOk() {
		LiteralRegex r = pf.createLiteralRegex();
		r.setValue("/[a-zA-Z0-9]*");
		tester.validator().checkLiteralRegex(r);
		tester.diagnose().assertError(IPPDiagnostics.ISSUE__NOT_REGEX);

		r.setValue("/[a-zA-Z0-9]*\\/");
		tester.validator().checkLiteralRegex(r);
		tester.diagnose().assertError(IPPDiagnostics.ISSUE__NOT_REGEX);

		r.setValue("/[a-zA-Z0-9]*/i");
		tester.validator().checkLiteralRegex(r);
		tester.diagnose().assertError(IPPDiagnostics.ISSUE__UNSUPPORTED_REGEX_FLAGS);
	}

	/**
	 * Test regular expression ok states:
	 * TODO: add more tests - this is just a smoke-test
	 */
	@Test
	public void test_Validate_LiteralRegex_Ok() {
		LiteralRegex r = pf.createLiteralRegex();
		r.setValue("/[a-zA-Z0-9]*/");
		tester.validator().checkLiteralRegex(r);
		tester.diagnose().assertOK();

	}

	@Test
	public void test_Validate_SingleQuotedString_NotOk() {

		{ // -- unescaped single quote
			SingleQuotedString ls = pf.createSingleQuotedString();
			ls.setText("I have an unprotected single quote ' in me.");
			tester.validator().checkSingleQuotedString(ls);
			tester.diagnose().assertError(IPPDiagnostics.ISSUE__NOT_STRING);
		}
		// TODO: Test DQ string
		// {
		// // -- unescaped double quote
		// DoubleQuotedString ls = pf.createDoubleQuotedString();
		// ls.setLeadingText("I have an unprotected double quote \" in me.");
		// tester.diagnose().assertError(IPPDiagnostics.ISSUE__NOT_STRING);
		// }
	}

	@Test
	public void test_Validate_SingleQuotedString_Ok() {
		SingleQuotedString ls = pf.createSingleQuotedString();
		ls.setText("I am a single quoted string with a tab \\t char");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- control char
		ls.setText("I am a single quoted string with a tab \t");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- new line
		ls.setText("I am a single quoted string with a nl \n");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- TODO: test NBSP

		// Unicode escapes are not supported as specific escapes as any
		// escaped character is the character itself - \u1234 is simply u1234
		// Should not produce an error or warning for sq string

		// -- unicode escape \\u [hexdigit]{4,4}
ls.setText("\\u1a2b");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- hex escape \x[hexdigit]{2,3} is not supported
		ls.setText("\\x1a");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- octal escape \[0-7]{3,3}
		ls.setText("\\777");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- meta escape \M-[sourcecharexceptNL]
		ls.setText("\\M-A");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- control escape \c[sourcecharexceptNL] or \C-[sourcecharexceptNL]
		ls.setText("\\C-J");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		ls.setText("\\cJ");
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		// -- escaped backslash and quotes as well as any escaped character
		ls.setText("\\\\"); // i.e. '\\'
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		ls.setText("\\'"); // i.e. '\''
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();

		ls.setText("\\p"); // i.e. '\p'
		tester.validator().checkSingleQuotedString(ls);
		tester.diagnose().assertOK();
	}
}
