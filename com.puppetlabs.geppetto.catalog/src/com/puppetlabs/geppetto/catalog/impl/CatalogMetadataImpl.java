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
package com.puppetlabs.geppetto.catalog.impl;

import com.puppetlabs.geppetto.catalog.CatalogFactory;
import com.puppetlabs.geppetto.catalog.CatalogMetadata;
import com.puppetlabs.geppetto.catalog.CatalogPackage;
import com.puppetlabs.geppetto.catalog.util.CatalogJsonSerializer;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Metadata</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.catalog.impl.CatalogMetadataImpl#getApi_version <em>Api version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class CatalogMetadataImpl extends EObjectImpl implements CatalogMetadata {
	public static class JsonAdapter extends CatalogJsonSerializer.ContainerDeserializer<CatalogMetadata> implements
			JsonSerializer<CatalogMetadata> {

		private static String getString(JsonObject jsonObj, String key) {
			JsonElement json = jsonObj.get(key);
			if(json == null)
				return null;
			String value = json.getAsString();

			// unset values are null, not empty strings
			return value.length() == 0
				? null
				: value;
		}

		private static void putString(JsonObject jsonObj, String key, String value) {
			if(value == null)
				value = "";
			jsonObj.addProperty(key, value);
		}

		@Override
		public CatalogMetadata deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			final CatalogMetadata result = CatalogFactory.eINSTANCE.createCatalogMetadata();
			JsonObject jsonObj = json.getAsJsonObject();

			result.setApi_version(getString(jsonObj, "api_version"));
			return result;
		}

		@Override
		public JsonElement serialize(CatalogMetadata src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
			final JsonObject result = new JsonObject();
			final CatalogMetadataImpl cat = (CatalogMetadataImpl) src;

			putString(result, "api_version", cat.getApi_version());

			return result;
		}
	}

	/**
	 * The default value of the '{@link #getApi_version() <em>Api version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getApi_version()
	 * @generated
	 * @ordered
	 */
	protected static final String API_VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getApi_version() <em>Api version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getApi_version()
	 * @generated
	 * @ordered
	 */
	protected String api_version = API_VERSION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	protected CatalogMetadataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch(featureID) {
			case CatalogPackage.CATALOG_METADATA__API_VERSION:
				return getApi_version();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch(featureID) {
			case CatalogPackage.CATALOG_METADATA__API_VERSION:
				return API_VERSION_EDEFAULT == null
					? api_version != null
					: !API_VERSION_EDEFAULT.equals(api_version);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch(featureID) {
			case CatalogPackage.CATALOG_METADATA__API_VERSION:
				setApi_version((String) newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return CatalogPackage.Literals.CATALOG_METADATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch(featureID) {
			case CatalogPackage.CATALOG_METADATA__API_VERSION:
				setApi_version(API_VERSION_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public String getApi_version() {
		return api_version;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setApi_version(String newApi_version) {
		String oldApi_version = api_version;
		api_version = newApi_version;
		if(eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, CatalogPackage.CATALOG_METADATA__API_VERSION, oldApi_version, api_version));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public String toString() {
		if(eIsProxy())
			return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (api_version: ");
		result.append(api_version);
		result.append(')');
		return result.toString();
	}

} // CatalogMetadataImpl
