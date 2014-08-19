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
package com.puppetlabs.geppetto.pp.pptp;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Type Argument</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.TypeArgument#isRequired <em>Required</em>}</li>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.TypeArgument#isNamevar <em>Namevar</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.puppetlabs.geppetto.pp.pptp.PPTPPackage#getTypeArgument()
 * @model abstract="true"
 * @generated
 */
public interface TypeArgument extends TargetElement {
	/**
	 * Returns the value of the '<em><b>Namevar</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Namevar</em>' attribute isn't clear, there really should be more of a description
	 * here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Namevar</em>' attribute.
	 * @see #setNamevar(boolean)
	 * @see com.puppetlabs.geppetto.pp.pptp.PPTPPackage#getTypeArgument_Namevar()
	 * @model
	 * @generated
	 */
	boolean isNamevar();

	/**
	 * Returns the value of the '<em><b>Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Required</em>' attribute isn't clear, there really should be more of a description
	 * here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Required</em>' attribute.
	 * @see #setRequired(boolean)
	 * @see com.puppetlabs.geppetto.pp.pptp.PPTPPackage#getTypeArgument_Required()
	 * @model
	 * @generated
	 */
	boolean isRequired();

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.pp.pptp.TypeArgument#isNamevar <em>Namevar</em>}'
	 * attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Namevar</em>' attribute.
	 * @see #isNamevar()
	 * @generated
	 */
	void setNamevar(boolean value);

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.pp.pptp.TypeArgument#isRequired <em>Required</em>}'
	 * attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Required</em>' attribute.
	 * @see #isRequired()
	 * @generated
	 */
	void setRequired(boolean value);

} // TypeArgument
