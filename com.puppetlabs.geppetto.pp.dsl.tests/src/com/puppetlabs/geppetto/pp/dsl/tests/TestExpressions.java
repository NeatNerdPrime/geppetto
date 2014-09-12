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

import static org.eclipse.xtext.junit4.validation.AssertableDiagnostics.errorCode;
import static org.eclipse.xtext.junit4.validation.AssertableDiagnostics.warningCode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.junit4.validation.AssertableDiagnostics;
import org.eclipse.xtext.resource.XtextResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.puppetlabs.geppetto.pp.AppendExpression;
import com.puppetlabs.geppetto.pp.AssignmentExpression;
import com.puppetlabs.geppetto.pp.AtExpression;
import com.puppetlabs.geppetto.pp.CollectExpression;
import com.puppetlabs.geppetto.pp.EqualityExpression;
import com.puppetlabs.geppetto.pp.Expression;
import com.puppetlabs.geppetto.pp.HostClassDefinition;
import com.puppetlabs.geppetto.pp.ImportExpression;
import com.puppetlabs.geppetto.pp.LiteralBoolean;
import com.puppetlabs.geppetto.pp.LiteralNameOrReference;
import com.puppetlabs.geppetto.pp.LiteralRegex;
import com.puppetlabs.geppetto.pp.MatchingExpression;
import com.puppetlabs.geppetto.pp.PuppetManifest;
import com.puppetlabs.geppetto.pp.RelationshipExpression;
import com.puppetlabs.geppetto.pp.ResourceExpression;
import com.puppetlabs.geppetto.pp.VariableExpression;
import com.puppetlabs.geppetto.pp.VirtualCollectQuery;
import com.puppetlabs.geppetto.pp.VirtualNameOrReference;
import com.puppetlabs.geppetto.pp.dsl.validation.IPPDiagnostics;

/**
 * Tests for expressions not covered by separate test classes.
 */
public class TestExpressions extends AbstractPuppetTests implements AbstractPuppetTests.SerializationTestControl {

	private PrintStream savedOut;

	// @formatter:off
	static final String Sample_Relationship = "file { 'file1':\n" + //
		"} -> file { 'file2':\n" + //
		"} -> file { 'file3':\n" + //
		"}\n";

	static final String Sample_Assignment1 = "$x = true\n";

	static final String Sample_Assignment2 = "$x[a] = true\n";

	static final String Sample_Append = "$x += true\n";

	static final String Sample_Match1 = "$x =~ /[a-z]*/\n";

	static final String Sample_Match2 = "$x !~ /[a-z]*/\n";

	static final String Sample_ClassDefinition = "class testClass {\n}\n";

	static final String Sample_If = //
	"if $a == 1 {\n" + //
		"  true\n" + //
		"} else {\n" + //
		"  false\n" + //
		"}\n\n" + //
		"if $a == 1 {\n" + //
		"  true\n" + //
		"} elsif $b < -3 {\n" + //
		"  false\n" + //
		"} else {\n" + //
		"  true\n" + //
		"}\n";

	private String doubleQuote(String s) {
		return '"' + s + '"';
	}

	/**
	 * Sends System.out to dev/null since there are many warnings about unknown variables (ignored unless
	 * explicitly tested for).
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		savedOut = System.out;
		OutputStream sink = new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
				// do nothing
			}

		};
		System.setOut(new PrintStream(sink));
	}

	@Override
	public boolean shouldTestSerializer(XtextResource resource) {
		return false;
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		System.setOut(savedOut);
	}

	/**
	 * No matter how formatter tries to add linewrapping there is none in the formatted result.
	 *
	 * @see PPFormatter#assignmentExpressionConfiguration(FormattingConfig c)
	 */
	@Test
	public void test_Format_AssignmentExpression() throws Exception {
		String code = "$a = 1\n$b = 2\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", code, s);
	}

	@Test
	public void test_Parse_MatchingExpression() throws Exception {
		String code = "$a =~ /[a-z]*/\n";
		XtextResource r = getResourceFromString(code);
		String s = serialize(r.getContents().get(0));
		assertEquals("serialization should produce same result", code, s);
	}

	@Test
	public void test_ParseCallWithEndComma() throws Exception {
		String code = "$a = shellquote(1,2,3,)";
		XtextResource r = getResourceFromString(code);
		tester.validate(r.getContents().get(0)).assertOK();
	}

	@Test
	public void test_Regexp_Array() throws Exception {
		String code = "$x = [/foo/, /bar/]";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Regexp_Assign() throws Exception {
		String code = "$x = /foo/";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Regexp_CallParam() throws Exception {
		String code = "my_func(/foo/, 'string')";
		XtextResource r = getResourceFromStringAndExpect(code, 1);
		resourceErrorDiagnostics(r).assertDiagnostic(IPPDiagnostics.ISSUE__UNKNOWN_FUNCTION_REFERENCE);
	}

	@Test
	public void test_Regexp_Equals() throws Exception {
		String code = "$x = /foo/\n$eq = $x == /foo/";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Regexp_Hash() throws Exception {
		String code = "$x = { pattern => /foo/ }";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Regexp_NotEquals() throws Exception {
		String code = "$x = /foo/\n$neq = $x != /foo/";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Regexp_Selector() throws Exception {
		String code = "$x = $y ? /foo/ => 'Match to foo'";
		XtextResource r = getResourceFromString(code);
		resourceErrorDiagnostics(r).assertOK();
	}

	@Test
	public void test_Serialize_AppendExpression() {
		PuppetManifest pp = pf.createPuppetManifest();
		AppendExpression ae = pf.createAppendExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		b.setValue(true);
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		String s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Append, s);
	}

	// Not relevant since new serializer always pretty prints
	// public void test_Serialize_IfExpression1() throws Exception {
	// String code = "if$a==1{true}else{false}if$a==1{true}elsif$b< -3{false}else{true}";
	// XtextResource r = getResourceFromString(code);
	// String s = serialize(r.getContents().get(0));
	//
	// // Broken in Xtext 2.0 - produces a semi formatted result, should leave string alone
	// assertEquals("serialization should produce same result as input", code, s);
	// }

	@Test
	public void test_Serialize_AssignmentExpression() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ae = pf.createAssignmentExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		b.setValue(true);
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		String s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Assignment1, s);

		AtExpression at = pf.createAtExpression();
		at.setLeftExpr(v);
		at.getParameters().add(createNameOrReference("a"));
		ae.setLeftExpr(at);
		tester.validate(pp).assertOK();
		s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Assignment2, s);
	}

	/**
	 * No matter how formatter tries to add linewrapping there is none in the formatted result.
	 *
	 * @see PPFormatter#functionCallConfiguration(FormattingConfig c)
	 */
	@Test
	public void test_Serialize_CallAndDefine() throws Exception {
		String code = "class a {\n}\n$a = include('a')\ndefine b {\n}\n";
		String fmt = "class a {\n}\n$a = include('a')\n\ndefine b {\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);
	}

	@Test
	public void test_Serialize_CaseExpression() throws Exception {
		String code = "case $a {present : { $x=1 $y=2 } absent,foo: {$x=2 $y=2}}";
		String fmt = "case $a {\n  present     : {\n    $x = 1\n    $y = 2\n  }\n  absent, foo : {\n    $x = 2\n    $y = 2\n  }\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);
	}

	@Test
	public void test_Serialize_Definition() throws Exception {
		String code = "define a {$a=10 $b=20}";
		String fmt = "define a {\n  $a = 10\n  $b = 20\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));

		assertEquals("serialization should produce specified result", fmt, s);
	}

	/**
	 * Due to issues in the (old) formatter, this test may hit a bug that inserts whitespace
	 * between quotes and string - no workaround found - needs to be fixed in Xtext formatter.
	 * Also see {@link #test_Serialize_DoubleQuotedString_2()}
	 *
	 * @see #test_Serialize_DoubleQuotedString_2() for a non failing tests.
	 * @throws Exception
	 */
	@Test
	public void test_Serialize_DoubleQuotedString_1() throws Exception {
		String original = "before${var}/after${1 + 2}$$${$var}";
		String code = doubleQuote(original) + "\n";
		XtextResource r = getResourceFromString(code);
		EObject result = r.getContents().get(0);
		assertTrue("Should be a PuppetManifest", result instanceof PuppetManifest);
		result = ((PuppetManifest) result).getStatements().get(0);

		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("Serialization of interpolated string should produce same result", code, s);
	}

	/**
	 * Formatter did not switch back to non hidden state after import "".
	 * If changed to '' string it behaved differently.
	 */
	@Test
	public void test_Serialize_DqStringFollowedByDefine() throws Exception {
		String code = "import \"foo\"\ndefine b {\n  $a = 1\n}\n";
		String fmt = "import \"foo\"\n\ndefine b {\n  $a = 1\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);
	}

	/**
	 * Formatter seems to not switch back to non hidden state interpolation.
	 */
	@Test
	public void test_Serialize_DqStringInterpolation() throws Exception {
		String code = "$a = \"a${1}b\"\nclass a {\n}\n";
		String fmt = "$a = \"a${1}b\"\n\nclass a {\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		// System.out.println(NodeModelUtils.compactDump(r.getParseResult().getRootNode(), false));
		assertEquals("serialization should produce specified result", fmt, s);
	}

	/**
	 * Without interpolation formatting does the right thing.
	 */
	@Test
	public void test_Serialize_DqStringNoInterpolation() throws Exception {
		String code = "$a = \"ab\"\nclass a {\n}\n";
		String fmt = "$a = \"ab\"\n\nclass a {\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		// System.out.println(NodeModelUtils.compactDump(r.getParseResult().getRootNode(), false));

		assertEquals("serialization should produce specified result", fmt, s);
	}

	@Test
	public void test_Serialize_HostClassDefinition() throws Exception {
		String code = "class a {$a=1 $b=2}";
		String fmt = "class a {\n  $a = 1\n  $b = 2\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);

	}

	@Test
	public void test_Serialize_HostClassExpression() {
		PuppetManifest pp = pf.createPuppetManifest();
		HostClassDefinition cd = pf.createHostClassDefinition();
		pp.getStatements().add(cd);
		cd.setClassName("testClass");

		String s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_ClassDefinition, s);

	}

	@Test
	public void test_Serialize_IfExpression2() throws Exception {
		String code = "if$a==1{true}else{ false }if$a==1{true}elsif$b< -3{false}else{true}";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", Sample_If, s);

	}

	@Test
	public void test_Serialize_IfExpression3() throws Exception {
		String code = "if$a==1{$x=1 $y=2}elsif $a==2 {$x=3 $y=4}else{ $x=5 $y=6 }";
		String fmt = "if $a == 1 {\n  $x = 1\n  $y = 2\n} elsif $a == 2 {\n  $x = 3\n  $y = 4\n} else {\n  $x = 5\n  $y = 6\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);

	}

	/**
	 * No matter how formatter tried to add linewrapping there was none in the formatted result.
	 *
	 * @see PPFormatter#importExpressionConfiguration(FormattingConfig c)
	 * @see #test_Serialize_ImportExpression2() - for different failing result
	 */
	@Test
	public void test_Serialize_ImportExpression1() throws Exception {
		String code = "import \"a\"\nimport \"b\"\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", code, s);
	}

	/**
	 * No matter how formatter tried to add linewrapping there was none in the formatted result.
	 * Note that result was different than in {@link #test_Serialize_ImportExpression1()} due to issue
	 * with the different use of hidden() for DQ string.
	 *
	 * @see PPFormatter#importExpressionConfiguration(FormattingConfig c)
	 * @see #test_Serialize_ImportExpression1() - for different failing result
	 */
	@Test
	public void test_Serialize_ImportExpression2() throws Exception {
		String code = "import 'a'\nimport 'b'\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", code, s);
	}

	@Test
	public void test_Serialize_ImportExpressionDq() throws Exception {
		String code = "import \"a\"\nimport \"b\"\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));

		// DEBUG
		// System.out.println(NodeModelUtils.compactDump(r.getParseResult().getRootNode(), false));

		assertEquals("serialization should produce specified result", code, s);
	}

	@Test
	public void test_Serialize_ImportExpressionSq() throws Exception {
		String code = "import 'a'\nimport 'b'\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		// DEBUG
		// System.out.println(NodeModelUtils.compactDump(r.getParseResult().getRootNode(), false));

		assertEquals("serialization should produce specified result", code, s);
	}

	@Test
	public void test_Serialize_MatchingExpression() {
		PuppetManifest pp = pf.createPuppetManifest();
		MatchingExpression me = pf.createMatchingExpression();
		LiteralRegex regex = pf.createLiteralRegex();
		regex.setValue("/[a-z]*/");
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		me.setLeftExpr(v);
		me.setOpName("=~");
		me.setRightExpr(regex);
		pp.getStatements().add(me);

		String s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Match1, s);

		me.setOpName("!~");
		s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Match2, s);
	}

	@Test
	public void test_Serialize_NodeDefinition() throws Exception {
		String code = "node a {$a=1 $b=2}";
		String fmt = "node a {\n  $a = 1\n  $b = 2\n}\n";
		XtextResource r = getResourceFromString(code);
		String s = serializeFormatted(r.getContents().get(0));
		assertEquals("serialization should produce specified result", fmt, s);

	}

	@Test
	public void test_Serialize_RelationshipExpression() {
		// -- serialize file { 'file1': } -> file{'file2': } -> file{'file3' : }
		PuppetManifest pp = pf.createPuppetManifest();
		EList<Expression> statements = pp.getStatements();

		RelationshipExpression rel1 = pf.createRelationshipExpression();
		RelationshipExpression rel2 = pf.createRelationshipExpression();
		ResourceExpression r1 = createResourceExpression("file", "file1");
		ResourceExpression r2 = createResourceExpression("file", "file2");
		ResourceExpression r3 = createResourceExpression("file", "file3");

		rel1.setOpName("->");
		rel1.setLeftExpr(r1);
		rel1.setRightExpr(r2);
		rel2.setOpName("->");
		rel2.setLeftExpr(rel1);
		rel2.setRightExpr(r3);

		statements.add(rel2);

		String s = serializeFormatted(pp);
		assertEquals("serialization should produce specified result", Sample_Relationship, s);

	}

	/**
	 * Tests append Notok states:
	 * - $x[a] += expr
	 * - a += expr
	 */
	@Test
	public void test_Validate_AppendExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		AppendExpression ae = pf.createAppendExpression();
		pp.getStatements().add(ae);

		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		AtExpression at = pf.createAtExpression();
		at.setLeftExpr(v);
		at.getParameters().add(createNameOrReference("a"));

		ae.setLeftExpr(at);
		ae.setRightExpr(b);

		tester.validate(pp).assertAll(
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__NOT_APPENDABLE),
			AssertableDiagnostics.warningCode(IPPDiagnostics.ISSUE__DEPRECATED_PLUS_EQUALS));

		ae.setLeftExpr(createNameOrReference("a"));
		tester.validate(pp).assertAll(
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__NOT_APPENDABLE),
			AssertableDiagnostics.warningCode(IPPDiagnostics.ISSUE__DEPRECATED_PLUS_EQUALS));

	}

	/**
	 * Tests append Notok states:
	 * - $0 += expr
	 */
	@Test
	public void test_Validate_AppendExpression_NotOk_Decimal() {
		PuppetManifest pp = pf.createPuppetManifest();
		AppendExpression ae = pf.createAppendExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$0");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertAll(
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__ASSIGNMENT_DECIMAL_VAR),
			AssertableDiagnostics.warningCode(IPPDiagnostics.ISSUE__DEPRECATED_PLUS_EQUALS));
	}

	/**
	 * Tests append Notok states:
	 * - $a::b += expr
	 */
	@Test
	public void test_Validate_AppendExpression_NotOk_Scope() {
		PuppetManifest pp = pf.createPuppetManifest();
		AppendExpression ae = pf.createAppendExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$a::b");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertAll(
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__ASSIGNMENT_OTHER_NAMESPACE),
			AssertableDiagnostics.warningCode(IPPDiagnostics.ISSUE__DEPRECATED_PLUS_EQUALS));
	}

	/**
	 * Test that append gives a deprecation warning.
	 * - $x += expr
	 */
	@Test
	public void test_Validate_AppendExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		AppendExpression ae = pf.createAppendExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertWarning(IPPDiagnostics.ISSUE__DEPRECATED_PLUS_EQUALS);
	}

	@Test
	public void test_Validate_AssignmentExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ae = pf.createAssignmentExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		LiteralNameOrReference v = createNameOrReference("x");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__NOT_ASSIGNABLE);

	}

	/**
	 * Tests that chained assignemt is not ok
	 * - $x = $y = expr
	 */
	@Test
	public void test_Validate_AssignmentExpression_NotOk_Chained() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ax = pf.createAssignmentExpression();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		ax.setLeftExpr(v);

		AssignmentExpression ay = pf.createAssignmentExpression();
		VariableExpression y = pf.createVariableExpression();
		y.setVarName("$y");
		ay.setLeftExpr(y);

		LiteralBoolean b = pf.createLiteralBoolean();
		ay.setRightExpr(b);
		ax.setRightExpr(ay);
		pp.getStatements().add(ax);

		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__ASSIGNMENT_CHAINED);
	}

	/**
	 * Tests assignment not ok states:
	 * - $0 = expr
	 */
	@Test
	public void test_Validate_AssignmentExpression_NotOk_Decimal() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ae = pf.createAssignmentExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$0");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__ASSIGNMENT_DECIMAL_VAR);
	}

	/**
	 * Tests assignment not ok states:
	 * - $a::b = expr
	 */
	@Test
	public void test_Validate_AssignmentExpression_NotOk_Scope() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ae = pf.createAssignmentExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$a::b");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__ASSIGNMENT_OTHER_NAMESPACE);
	}

	/**
	 * Tests assignemt ok states:
	 * - $x = expr
	 * - $x[expr] = expr
	 */
	@Test
	public void test_Validate_AssignmentExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression ae = pf.createAssignmentExpression();
		LiteralBoolean b = pf.createLiteralBoolean();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		ae.setLeftExpr(v);
		ae.setRightExpr(b);
		pp.getStatements().add(ae);

		tester.validate(pp).assertOK();

		AtExpression at = pf.createAtExpression();
		at.setLeftExpr(v);
		at.getParameters().add(createNameOrReference("a"));
		ae.setLeftExpr(at);
		tester.validate(pp).assertOK();
	}

	@Test
	public void test_Validate_ImportExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		ImportExpression ip = pf.createImportExpression();
		pp.getStatements().add(ip);

		tester.validate(ip).assertAll(
			errorCode(IPPDiagnostics.ISSUE__REQUIRED_EXPRESSION), warningCode(IPPDiagnostics.ISSUE__DEPRECATED_IMPORT));
	}

	@Test
	public void test_Validate_ImportExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		ImportExpression ip = pf.createImportExpression();
		ip.getValues().add(createSqString("somewhere/*.pp"));
		pp.getStatements().add(ip);

		tester.validate(ip).assertWarning(IPPDiagnostics.ISSUE__DEPRECATED_IMPORT);
		ip.getValues().add(createSqString("nowhere/*.pp"));
		tester.validate(ip).assertWarning(IPPDiagnostics.ISSUE__DEPRECATED_IMPORT);
	}

	@Test
	public void test_Validate_Manifest_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		VariableExpression v = pf.createVariableExpression();
		pp.getStatements().add(v);
		v.setVarName("$x");
		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__NOT_TOPLEVEL);
	}

	@Test
	public void test_Validate_Manifest_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		AssignmentExpression a = pf.createAssignmentExpression();
		VariableExpression v = pf.createVariableExpression();
		pp.getStatements().add(a);
		v.setVarName("$x");
		a.setLeftExpr(v);
		LiteralNameOrReference value = createNameOrReference("10");
		a.setRightExpr(value);
		tester.validate(pp).assertOK();
	}

	@Test
	public void test_Validate_MatchingExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		MatchingExpression me = pf.createMatchingExpression();
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		VariableExpression v2 = pf.createVariableExpression();
		v2.setVarName("$y");

		me.setLeftExpr(v);
		me.setOpName("=~");
		me.setRightExpr(v2);
		pp.getStatements().add(me);

		tester.validate(me).assertError(IPPDiagnostics.ISSUE__UNSUPPORTED_EXPRESSION);

		me.setOpName("~=");
		tester.validate(me).assertAll(
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__ILLEGAL_OP),
			AssertableDiagnostics.errorCode(IPPDiagnostics.ISSUE__UNSUPPORTED_EXPRESSION));
	}

	@Test
	public void test_Validate_MatchingExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		MatchingExpression me = pf.createMatchingExpression();
		LiteralRegex regex = pf.createLiteralRegex();
		regex.setValue("/[a-z]*/");
		VariableExpression v = pf.createVariableExpression();
		v.setVarName("$x");
		me.setLeftExpr(v);
		me.setOpName("=~");
		me.setRightExpr(regex);
		pp.getStatements().add(me);

		tester.validate(me).assertOK();

		me.setOpName("!~");
		tester.validate(me).assertOK();
	}

	@Test
	public void test_Validate_RelationshipExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		// -- check file { 'file1': } -> file{'file2': } -> file{'file3' : }
		RelationshipExpression rel1 = pf.createRelationshipExpression();
		pp.getStatements().add(rel1);

		ResourceExpression r1 = createResourceExpression("file", "file1");
		LiteralNameOrReference r2 = createNameOrReference("a");

		rel1.setOpName("->");
		rel1.setLeftExpr(r1);
		rel1.setRightExpr(r2);

		tester.validate(pp).assertError(IPPDiagnostics.ISSUE__UNSUPPORTED_EXPRESSION);
	}

	/**
	 * Test that the four different relationship expressions operands can be used between
	 * the allowed operands.
	 */
	@Test
	public void test_Validate_RelationshipExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		// -- check file { 'file1': } -> file{'file2': } -> file{'file3' : }
		RelationshipExpression rel1 = pf.createRelationshipExpression();
		RelationshipExpression rel2 = pf.createRelationshipExpression();
		pp.getStatements().add(rel2);
		ResourceExpression r1 = createResourceExpression("file", "'file1'");
		ResourceExpression r2 = createResourceExpression("file", "'file2'");
		ResourceExpression r3 = createResourceExpression("file", "'file3'");

		rel1.setOpName("->");
		rel1.setLeftExpr(r1);
		rel1.setRightExpr(r2);
		rel2.setOpName("->");
		rel2.setLeftExpr(rel1);
		rel2.setRightExpr(r3);

		tester.validator().checkRelationshipExpression(rel2);
		tester.diagnose().assertOK();

		// -- check the other operators
		rel2.setOpName("<-");
		tester.validator().checkRelationshipExpression(rel2);
		tester.diagnose().assertOK();

		rel2.setOpName("<~");
		tester.validator().checkRelationshipExpression(rel2);
		tester.diagnose().assertOK();

		rel2.setOpName("~>");
		tester.validator().checkRelationshipExpression(rel2);
		tester.diagnose().assertOK();

		// -- check the other possible left/right expressions
		// - virtual
		// - resource reference
		// - collect expression
		AtExpression at = pf.createAtExpression();
		at.setLeftExpr(createNameOrReference("x"));
		at.getParameters().add(createNameOrReference("a"));
		r1.setResourceExpr(at); // resource reference

		VirtualNameOrReference vn = pf.createVirtualNameOrReference();
		vn.setValue("y");
		vn.setExported(true);
		r2.setResourceExpr(vn);

		CollectExpression ce = pf.createCollectExpression();
		ce.setClassReference(createNameOrReference("User"));
		EqualityExpression predicate = pf.createEqualityExpression();
		predicate.setLeftExpr(createNameOrReference("name"));
		predicate.setOpName("==");
		predicate.setRightExpr(createNameOrReference("Luke"));

		VirtualCollectQuery q = pf.createVirtualCollectQuery();
		q.setExpr(predicate);
		ce.setQuery(q);

		rel2.setRightExpr(ce);

		tester.validator().checkRelationshipExpression(rel2);
		tester.diagnose().assertOK();

	}

	@Test
	public void test_Validate_VariableExpression_NotOk() {
		PuppetManifest pp = pf.createPuppetManifest();
		VariableExpression v = pf.createVariableExpression();
		pp.getStatements().add(v);

		// name is null
		tester.validate(v).assertError(IPPDiagnostics.ISSUE__NOT_VARNAME);

		v.setVarName("");
		tester.validate(v).assertError(IPPDiagnostics.ISSUE__NOT_VARNAME);

		v.setVarName("x");
		tester.validate(v).assertError(IPPDiagnostics.ISSUE__NOT_VARNAME);

		// period is allowed in names, but not in variables
		v.setVarName("$3.4");
		tester.validate(v).assertError(IPPDiagnostics.ISSUE__NOT_VARNAME);
	}

	@Test
	public void test_Validate_VariableExpression_Ok() {
		PuppetManifest pp = pf.createPuppetManifest();
		VariableExpression v = pf.createVariableExpression();
		pp.getStatements().add(v);
		v.setVarName("$x");

		tester.validate(v).assertOK();

		v.setVarName("$abc123");
		tester.validate(v).assertOK();

		v.setVarName("$0");
		tester.validate(v).assertOK();

		v.setVarName("$3_4");
		tester.validate(v).assertWarning(IPPDiagnostics.ISSUE__DEPRECATED_VARIABLE_NAME);

		v.setVarName("$Abc123");
		tester.validate(v).assertWarning(IPPDiagnostics.ISSUE__DEPRECATED_VARIABLE_NAME);
	}

}
