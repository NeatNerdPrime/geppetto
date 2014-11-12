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
package com.puppetlabs.geppetto.validation.runner;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.puppetlabs.geppetto.forge.model.Dependency;
import com.puppetlabs.geppetto.forge.model.Metadata;
import com.puppetlabs.geppetto.forge.model.Type;
import com.puppetlabs.geppetto.semver.VersionRange;

public class MetadataInfo {
	public static class Resolution {
		public final Dependency dependency;

		public final MetadataInfo metadata;

		Resolution(Dependency d, MetadataInfo mi) {
			this.dependency = d;
			this.metadata = mi;
		}

		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();
			toString(bld);
			return bld.toString();
		}

		public void toString(StringBuilder bld) {
			dependencyToString(dependency, bld);
			bld.append("->");
			Metadata md = metadata.getMetadata();
			md.getName().toString(bld);
			bld.append('/');
			md.getVersion().toString(bld);
		}
	}

	public static void circularityLabel(List<MetadataInfo> circularity, StringBuilder result) {
		for(MetadataInfo mi : circularity) {
			mi.getMetadata().getName().toString(result);
			result.append("->");
		}
		circularity.get(0).getMetadata().getName().toString(result);
	}

	private static void dependencyToString(Dependency dep, StringBuilder bld) {
		dep.getName().toString(bld);
		VersionRange vr = dep.getVersionRequirement();
		if(vr != null) {
			bld.append('(');
			vr.toString(bld);
			bld.append(')');
		}
	}

	private final Metadata metadata;

	private final File file;

	private final List<Resolution> resolvedDependencies;

	private final List<Dependency> unresolvedDependencies;

	private final List<Type> types;

	private final boolean roleFlag;

	public MetadataInfo(Metadata data, File f, boolean roleFlag) {
		this.metadata = data;
		this.file = f;
		this.resolvedDependencies = Lists.newArrayList();
		this.unresolvedDependencies = Lists.newArrayList();
		this.types = Lists.newArrayList();
		this.roleFlag = roleFlag;
	}

	public void addResolvedDependency(Dependency d, MetadataInfo mi) {
		resolvedDependencies.add(new Resolution(d, mi));
	}

	public void addType(Type type) {
		types.add(type);
	}

	/**
	 * @param d
	 */
	public void addUnresolvedDependency(Dependency d) {
		unresolvedDependencies.add(d);

	}

	public File getFile() {
		return file;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Collection<Resolution> getResolvedDependencies() {
		return Collections.unmodifiableList(resolvedDependencies);
	}

	public Collection<Type> getTypes() {
		return Collections.unmodifiableList(types);
	}

	public Collection<Dependency> getUnresolvedDependencies() {
		return Collections.unmodifiableList(unresolvedDependencies);
	}

	/**
	 * Returns true if this Metadatainfo represents a puppet module describing a
	 * "role"
	 *
	 * @return true if this instance represents a role.
	 */
	public boolean isRole() {
		return roleFlag;
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		toString(bld);
		return bld.toString();
	}

	public void toString(StringBuilder bld) {
		metadata.getName().toString(bld);
		bld.append('/');
		metadata.getVersion().toString(bld);
		for(Dependency dep : unresolvedDependencies) {
			bld.append("\n\tUnresolved dependency: ");
			dependencyToString(dep, bld);
		}
		for(Resolution res : resolvedDependencies) {
			bld.append("\n\tResolved dependency: ");
			res.toString(bld);
		}
		bld.append('\n');
	}
}
