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

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Resource Expression</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.pp.ResourceExpression#getResourceExpr <em>Resource Expr</em>}</li>
 * <li>{@link com.puppetlabs.geppetto.pp.ResourceExpression#getResourceData <em>Resource Data</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.puppetlabs.geppetto.pp.PPPackage#getResourceExpression()
 * @model
 * @generated
 */
public interface ResourceExpression extends Expression {
	/**
	 * Returns the value of the '<em><b>Resource Data</b></em>' containment reference list.
	 * The list contents are of type {@link com.puppetlabs.geppetto.pp.ResourceBody}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Resource Data</em>' containment reference list isn't clear, there really should be
	 * more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Resource Data</em>' containment reference list.
	 * @see com.puppetlabs.geppetto.pp.PPPackage#getResourceExpression_ResourceData()
	 * @model containment="true"
	 * @generated
	 */
	EList<ResourceBody> getResourceData();

	/**
	 * Returns the value of the '<em><b>Resource Expr</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Resource Expr</em>' containment reference isn't clear, there really should be more of
	 * a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Resource Expr</em>' containment reference.
	 * @see #setResourceExpr(Expression)
	 * @see com.puppetlabs.geppetto.pp.PPPackage#getResourceExpression_ResourceExpr()
	 * @model containment="true"
	 * @generated
	 */
	Expression getResourceExpr();

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.pp.ResourceExpression#getResourceExpr
	 * <em>Resource Expr</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Resource Expr</em>' containment reference.
	 * @see #getResourceExpr()
	 * @generated
	 */
	void setResourceExpr(Expression value);

} // ResourceExpression
