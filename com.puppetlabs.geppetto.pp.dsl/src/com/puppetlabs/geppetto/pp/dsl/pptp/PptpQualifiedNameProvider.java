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
package com.puppetlabs.geppetto.pp.dsl.pptp;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;

import com.google.inject.Inject;
import com.puppetlabs.geppetto.pp.pptp.Function;
import com.puppetlabs.geppetto.pp.pptp.MetaVariable;
import com.puppetlabs.geppetto.pp.pptp.NameSpace;
import com.puppetlabs.geppetto.pp.pptp.Parameter;
import com.puppetlabs.geppetto.pp.pptp.Property;
import com.puppetlabs.geppetto.pp.pptp.Provider;
import com.puppetlabs.geppetto.pp.pptp.PuppetDistribution;
import com.puppetlabs.geppetto.pp.pptp.TPVariable;

/**
 * A Puppet Qualified Name provider.
 */
public class PptpQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {

	public static QualifiedName splice(QualifiedName a, QualifiedName b) {
		return a == null
			? b
			: a.append(b);
	}

	@Inject
	private IQualifiedNameConverter converter;

	/**
	 * The fully qualified name of the closest named parent.
	 *
	 * @param o
	 * @return
	 */
	QualifiedName getParentsFullyQualifiedName(EObject o) {
		for(EObject tmp = o.eContainer(); tmp != null; tmp = tmp.eContainer()) {
			QualifiedName n = getFullyQualifiedName(tmp);
			if(n != null)
				return n;
		}
		return null;
	}

	QualifiedName qualifiedName(Function o) {
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}

	QualifiedName qualifiedName(MetaVariable o) {
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}

	QualifiedName qualifiedName(NameSpace o) {
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}

	QualifiedName qualifiedName(Parameter o) {
		// stripping of $ is done by PPQualifiedNameConverter
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}

	QualifiedName qualifiedName(Property o) {
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}

	QualifiedName qualifiedName(Provider o) {
		return QualifiedName.create(o.getTypeName(), o.getName());
	}

	QualifiedName qualifiedName(PuppetDistribution o) {
		return null;
	}

	QualifiedName qualifiedName(TPVariable o) {
		return splice(getParentsFullyQualifiedName(o), converter.toQualifiedName(o.getName()));
	}
}
