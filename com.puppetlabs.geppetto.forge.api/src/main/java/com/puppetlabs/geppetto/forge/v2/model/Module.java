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
package com.puppetlabs.geppetto.forge.v2.model;

import static com.puppetlabs.geppetto.forge.model.ModuleName.safeName;
import static com.puppetlabs.geppetto.forge.model.ModuleName.safeOwner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.puppetlabs.geppetto.forge.model.ModuleName;

/**
 */
public class Module extends TimestampedEntity {
	@Expose
	private String name;

	@Expose
	private String description;

	@Expose
	private String home_page_url;

	@Expose
	private String source_url;

	@Expose
	private String issues_url;

	@Expose
	private String commit_feed_url;

	@Expose
	private Integer downloads;

	@Expose
	private User owner;

	@Expose
	private List<String> tags = Collections.emptyList();

	@Expose
	private FlatRelease current_release;

	@Expose
	private List<AnnotatedLink> releases;

	/**
	 * @return Commit Feed URL
	 */
	public String getCommitFeedURL() {
		return commit_feed_url;
	}

	/**
	 * @return Current release for module
	 */
	public FlatRelease getCurrentRelease() {
		return current_release;
	}

	/**
	 * @return Description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @return Total number of downloads
	 */
	public Integer getDownloads() {
		return downloads;
	}

	/**
	 * @return The full name as &quot;&lt;owner&gt;-&lt;module&gt;&quot;
	 */
	public ModuleName getFullName() {
		if(owner != null && name != null) {
			String ownerName = owner.getUsername();
			if(ownerName != null)
				return ModuleName.create(safeOwner(ownerName), safeName(name, false), false);
		}
		return null;
	}

	/**
	 * @return Home Page URL
	 */
	public String getHomePageURL() {
		return home_page_url;
	}

	/**
	 * @return Issues URL
	 */
	public String getIssuesURL() {
		return issues_url;
	}

	/**
	 * @return Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Owner
	 */
	public User getOwner() {
		return owner;
	}

	/**
	 * @return the releases
	 */
	public List<AnnotatedLink> getReleases() {
		return releases;
	}

	/**
	 * @return Source URL
	 */
	public String getSourceURL() {
		return source_url;
	}

	/**
	 * @return List of tag names
	 */
	public List<String> getTags() {
		return new ArrayList<String>(tags);
	}

	/**
	 * @param commitFeedURL
	 *            Commit Feed URL
	 */
	public void setCommitFeedURL(String commitFeedURL) {
		this.commit_feed_url = commitFeedURL;
	}

	/**
	 * @param currentRelease
	 *            Current release for module
	 */
	public void setCurrentRelease(FlatRelease currentRelease) {
		this.current_release = currentRelease;
	}

	/**
	 * @param description
	 *            Description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param downloads
	 *            Total number of downloads
	 */
	public void setDownloads(Integer downloads) {
		this.downloads = downloads;
	}

	/**
	 * @param homePageURL
	 *            Home Page URL
	 */
	public void setHomePageURL(String homePageURL) {
		this.home_page_url = homePageURL;
	}

	/**
	 * @param issuesURL
	 *            Issues URL
	 */
	public void setIssuesURL(String issuesURL) {
		this.issues_url = issuesURL;
	}

	/**
	 * @param name
	 *            Name of module
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param owner
	 *            Name of owner
	 */
	public void setOwner(User owner) {
		this.owner = owner;
	}

	/**
	 * @param releases
	 *            the releases to set
	 */
	public void setReleases(List<AnnotatedLink> releases) {
		this.releases = releases;
	}

	/**
	 * @param sourceURL
	 *            Source URL
	 */
	public void setSourceURL(String sourceURL) {
		this.source_url = sourceURL;
	}

	/**
	 * @param tags
	 *            Tag names
	 */
	public void setTags(List<String> tags) {
		this.tags = tags == null
				? Collections.<String> emptyList()
						: tags;
	}
}
