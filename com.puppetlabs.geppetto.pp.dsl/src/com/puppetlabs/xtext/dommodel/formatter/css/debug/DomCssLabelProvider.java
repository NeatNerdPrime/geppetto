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

import java.util.Collections;

import org.eclipse.xtext.util.Exceptions;
import org.eclipse.xtext.util.PolymorphicDispatcher;
import org.eclipse.xtext.util.PolymorphicDispatcher.ErrorHandler;

import com.puppetlabs.xtext.dommodel.formatter.ILayoutManager;
import com.puppetlabs.xtext.dommodel.formatter.css.IStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.AlignedSeparatorIndex;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.AlignmentStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.DedentStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.IndentStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.LayoutManagerStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.LineBreakStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.SpacingStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.StyleNameStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.TokenTextStyle;
import com.puppetlabs.xtext.dommodel.formatter.css.StyleFactory.WidthStyle;

/**
 * A label provider for debugging labels.
 */
public class DomCssLabelProvider implements IStringProvider {
	private final PolymorphicDispatcher<String> textDispatcher = new PolymorphicDispatcher<String>(
		"_string", 1, 1, Collections.singletonList(this), new ErrorHandler<String>() {
			@Override
			public String handle(Object[] params, Throwable e) {
				return handleTextError(params, e);
			}
		});

	protected String _string(AlignedSeparatorIndex o) {
		return "alignedSeparatorIndex: " + valueString(o);
	}

	protected String _string(AlignmentStyle o) {
		return "align: " + valueString(o);
	}

	protected String _string(DedentStyle o) {
		return "dedent: " + valueString(o);
	}

	protected String _string(ILayoutManager o) {
		return o.getClass().getSimpleName();
	}

	protected String _string(IndentStyle o) {
		return "indent: " + valueString(o);
	}

	protected String _string(LayoutManagerStyle o) {
		return "layout: " + valueString(o);
	}

	protected String _string(LineBreakStyle o) {
		return "break: " + valueString(o);
	}

	protected String _string(SpacingStyle o) {
		return "spacing: " + valueString(o);
	}

	protected String _string(StyleNameStyle o) {
		return "styleName: " + valueString(o);
	}

	protected String _string(TokenTextStyle o) {
		return "tokenText: " + valueString(o);
	}

	protected String _string(WidthStyle o) {
		return "width: " + valueString(o);
	}

	private String getDefaultText(Object element) {
		return element.toString();
	}

	protected String handleTextError(Object[] params, Throwable e) {
		if(e instanceof NullPointerException) {
			return getDefaultText(params[0]);
		}
		return Exceptions.throwUncheckedException(e);
	}

	@Override
	public String string(Object element) {
		String text = textDispatcher.invoke(element);
		if(text != null) {
			return text;
		}
		return null;
	}

	protected String valueString(IStyle<?> style) {
		if(style.isFunction())
			return "f(node)";
		return string(style.getValue(null));
	}
}
