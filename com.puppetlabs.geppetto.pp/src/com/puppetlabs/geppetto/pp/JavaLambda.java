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
package com.puppetlabs.geppetto.pp;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Java Lambda</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.pp.JavaLambda#isFarrow <em>Farrow</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.puppetlabs.geppetto.pp.PPPackage#getJavaLambda()
 * @model
 * @generated
 */
public interface JavaLambda extends Lambda {
	/**
	 * Returns the value of the '<em><b>Farrow</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Farrow</em>' attribute isn't clear, there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Farrow</em>' attribute.
	 * @see #setFarrow(boolean)
	 * @see com.puppetlabs.geppetto.pp.PPPackage#getJavaLambda_Farrow()
	 * @model
	 * @generated
	 */
	boolean isFarrow();

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.pp.JavaLambda#isFarrow <em>Farrow</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Farrow</em>' attribute.
	 * @see #isFarrow()
	 * @generated
	 */
	void setFarrow(boolean value);

} // JavaLambda
