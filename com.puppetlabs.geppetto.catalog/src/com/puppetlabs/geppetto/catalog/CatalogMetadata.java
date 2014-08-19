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
package com.puppetlabs.geppetto.catalog;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Metadata</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.catalog.CatalogMetadata#getApi_version <em>Api version</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.puppetlabs.geppetto.catalog.CatalogPackage#getCatalogMetadata()
 * @model
 * @generated
 */
public interface CatalogMetadata extends EObject {
	/**
	 * Returns the value of the '<em><b>Api version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Api version</em>' attribute isn't clear, there really should be more of a description
	 * here...
	 * </p>
	 * <!-- end-user-doc -->
	 *
	 * @return the value of the '<em>Api version</em>' attribute.
	 * @see #setApi_version(String)
	 * @see com.puppetlabs.geppetto.catalog.CatalogPackage#getCatalogMetadata_Api_version()
	 * @model
	 * @generated
	 */
	String getApi_version();

	/**
	 * Sets the value of the '{@link com.puppetlabs.geppetto.catalog.CatalogMetadata#getApi_version
	 * <em>Api version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @param value
	 *            the new value of the '<em>Api version</em>' attribute.
	 * @see #getApi_version()
	 * @generated
	 */
	void setApi_version(String value);

} // CatalogMetadata
