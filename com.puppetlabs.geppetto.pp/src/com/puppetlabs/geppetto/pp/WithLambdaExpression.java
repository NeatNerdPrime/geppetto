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
 * A representation of the model object '<em><b>With Lambda Expression</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.pp.WithLambdaExpression#getLambda <em>Lambda</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.puppetlabs.geppetto.pp.PPPackage#getWithLambdaExpression()
 * @model
 * @generated
 */
public interface WithLambdaExpression extends ParameterizedExpression {
	/**
	 * Returns the value of the '<em><b>Lambda</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Lambda</em>' containment reference isn't clear, there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Lambda</em>' containment reference.
	 * @see #setLambda(Lambda)
	 * @see com.puppetlabs.geppetto.pp.PPPackage#getWithLambdaExpression_Lambda()
	 * @model containment="true"
	 * @generated
	 */
	Lambda getLambda();

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.pp.WithLambdaExpression#getLambda <em>Lambda</em>}'
	 * containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Lambda</em>' containment reference.
	 * @see #getLambda()
	 * @generated
	 */
	void setLambda(Lambda value);

} // WithLambdaExpression
