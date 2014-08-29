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
package com.puppetlabs.geppetto.pp.pptp.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.puppetlabs.geppetto.pp.pptp.Function;
import com.puppetlabs.geppetto.pp.pptp.INamed;
import com.puppetlabs.geppetto.pp.pptp.PPTPPackage;
import com.puppetlabs.geppetto.pp.pptp.TargetElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Function</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.impl.FunctionImpl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.impl.FunctionImpl#getName <em>Name</em>}</li>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.impl.FunctionImpl#isDeprecated <em>Deprecated</em>}</li>
 * <li>{@link com.puppetlabs.geppetto.pp.pptp.impl.FunctionImpl#isRValue <em>RValue</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FunctionImpl extends EObjectImpl implements Function {
	/**
	 * The default value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getDocumentation()
	 * @generated
	 * @ordered
	 */
	protected static final String DOCUMENTATION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getDocumentation()
	 * @generated
	 * @ordered
	 */
	protected String documentation = DOCUMENTATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #isDeprecated() <em>Deprecated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #isDeprecated()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DEPRECATED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isDeprecated() <em>Deprecated</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #isDeprecated()
	 * @generated
	 * @ordered
	 */
	protected boolean deprecated = DEPRECATED_EDEFAULT;

	/**
	 * The default value of the '{@link #isRValue() <em>RValue</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #isRValue()
	 * @generated
	 * @ordered
	 */
	protected static final boolean RVALUE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isRValue() <em>RValue</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @see #isRValue()
	 * @generated
	 * @ordered
	 */
	protected boolean rValue = RVALUE_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	protected FunctionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public int eBaseStructuralFeatureID(int derivedFeatureID, Class<?> baseClass) {
		if(baseClass == INamed.class) {
			switch(derivedFeatureID) {
				case PPTPPackage.FUNCTION__NAME:
					return PPTPPackage.INAMED__NAME;
				default:
					return -1;
			}
		}
		if(baseClass == TargetElement.class) {
			switch(derivedFeatureID) {
				case PPTPPackage.FUNCTION__DEPRECATED:
					return PPTPPackage.TARGET_ELEMENT__DEPRECATED;
				default:
					return -1;
			}
		}
		return super.eBaseStructuralFeatureID(derivedFeatureID, baseClass);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public int eDerivedStructuralFeatureID(int baseFeatureID, Class<?> baseClass) {
		if(baseClass == INamed.class) {
			switch(baseFeatureID) {
				case PPTPPackage.INAMED__NAME:
					return PPTPPackage.FUNCTION__NAME;
				default:
					return -1;
			}
		}
		if(baseClass == TargetElement.class) {
			switch(baseFeatureID) {
				case PPTPPackage.TARGET_ELEMENT__DEPRECATED:
					return PPTPPackage.FUNCTION__DEPRECATED;
				default:
					return -1;
			}
		}
		return super.eDerivedStructuralFeatureID(baseFeatureID, baseClass);
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
			case PPTPPackage.FUNCTION__DOCUMENTATION:
				return getDocumentation();
			case PPTPPackage.FUNCTION__NAME:
				return getName();
			case PPTPPackage.FUNCTION__DEPRECATED:
				return isDeprecated();
			case PPTPPackage.FUNCTION__RVALUE:
				return isRValue();
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
			case PPTPPackage.FUNCTION__DOCUMENTATION:
				return DOCUMENTATION_EDEFAULT == null
					? documentation != null
					: !DOCUMENTATION_EDEFAULT.equals(documentation);
			case PPTPPackage.FUNCTION__NAME:
				return NAME_EDEFAULT == null
					? name != null
					: !NAME_EDEFAULT.equals(name);
			case PPTPPackage.FUNCTION__DEPRECATED:
				return deprecated != DEPRECATED_EDEFAULT;
			case PPTPPackage.FUNCTION__RVALUE:
				return rValue != RVALUE_EDEFAULT;
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
			case PPTPPackage.FUNCTION__DOCUMENTATION:
				setDocumentation((String) newValue);
				return;
			case PPTPPackage.FUNCTION__NAME:
				setName((String) newValue);
				return;
			case PPTPPackage.FUNCTION__DEPRECATED:
				setDeprecated((Boolean) newValue);
				return;
			case PPTPPackage.FUNCTION__RVALUE:
				setRValue((Boolean) newValue);
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
		return PPTPPackage.Literals.FUNCTION;
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
			case PPTPPackage.FUNCTION__DOCUMENTATION:
				setDocumentation(DOCUMENTATION_EDEFAULT);
				return;
			case PPTPPackage.FUNCTION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case PPTPPackage.FUNCTION__DEPRECATED:
				setDeprecated(DEPRECATED_EDEFAULT);
				return;
			case PPTPPackage.FUNCTION__RVALUE:
				setRValue(RVALUE_EDEFAULT);
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
	public String getDocumentation() {
		return documentation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public boolean isDeprecated() {
		return deprecated;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public boolean isRValue() {
		return rValue;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setDeprecated(boolean newDeprecated) {
		boolean oldDeprecated = deprecated;
		deprecated = newDeprecated;
		if(eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PPTPPackage.FUNCTION__DEPRECATED, oldDeprecated, deprecated));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setDocumentation(String newDocumentation) {
		String oldDocumentation = documentation;
		documentation = newDocumentation;
		if(eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PPTPPackage.FUNCTION__DOCUMENTATION, oldDocumentation, documentation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if(eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PPTPPackage.FUNCTION__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated
	 */
	@Override
	public void setRValue(boolean newRValue) {
		boolean oldRValue = rValue;
		rValue = newRValue;
		if(eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, PPTPPackage.FUNCTION__RVALUE, oldRValue, rValue));
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
		result.append(" (documentation: ");
		result.append(documentation);
		result.append(", name: ");
		result.append(name);
		result.append(", deprecated: ");
		result.append(deprecated);
		result.append(", rValue: ");
		result.append(rValue);
		result.append(')');
		return result.toString();
	}

} // FunctionImpl
