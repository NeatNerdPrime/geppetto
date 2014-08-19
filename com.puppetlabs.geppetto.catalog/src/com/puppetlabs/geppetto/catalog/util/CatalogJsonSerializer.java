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
package com.puppetlabs.geppetto.catalog.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

import com.puppetlabs.geppetto.catalog.Catalog;
import com.puppetlabs.geppetto.catalog.CatalogEdge;
import com.puppetlabs.geppetto.catalog.CatalogResource;
import com.puppetlabs.geppetto.catalog.CatalogResourceParameter;
import com.puppetlabs.geppetto.catalog.impl.CatalogEdgeImpl;
import com.puppetlabs.geppetto.catalog.impl.CatalogImpl;
import com.puppetlabs.geppetto.catalog.impl.CatalogMetadataImpl;
import com.puppetlabs.geppetto.catalog.impl.CatalogResourceImpl;
import com.puppetlabs.geppetto.catalog.impl.CatalogResourceParameterImpl;
import com.puppetlabs.geppetto.common.os.StreamUtil;
import org.eclipse.emf.common.util.EList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class CatalogJsonSerializer {

	public static abstract class ContainerDeserializer<T> implements JsonDeserializer<T> {
		protected static <T> void deserializeInto(JsonElement json, EList<T> result, Class<? extends T> cls,
				JsonDeserializationContext context) {
			JsonArray jsonTypes = json.getAsJsonArray();
			Iterator<JsonElement> elements = jsonTypes.iterator();
			while(elements.hasNext()) {
				@SuppressWarnings("unchecked")
				T elem = (T) context.deserialize(elements.next(), cls);
				result.add(elem);
			}
		}
	}

	public static class EListSerializer<T> implements JsonSerializer<EList<T>> {

		@Override
		public JsonElement serialize(EList<T> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
			JsonArray result = new JsonArray();
			for(T elem : src)
				result.add(context.serialize(elem));
			return result;
		}
	}

	public static Gson getGSon() {
		return gsonBuilder.create();
	}

	/**
	 * Loads a Catalog model from a catalog JSON file.
	 *
	 */
	public static Catalog load(File jsonFile) throws IOException {
		Reader reader = new BufferedReader(new FileReader(jsonFile));
		try {
			Gson gson = getGSon();
			CatalogImpl md = gson.fromJson(reader, CatalogImpl.class);
			return md;
		}
		finally {
			StreamUtil.close(reader);
		}
	}

	/**
	 * Loads a Catalog model from a catalog JSON input stream.
	 *
	 */
	public static Catalog load(InputStream jsonStream) throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(jsonStream));
		try {
			Gson gson = getGSon();
			CatalogImpl md = gson.fromJson(reader, CatalogImpl.class);
			return md;
		}
		finally {
			StreamUtil.close(reader);
		}
	}

	/**
	 * Loads a Catalog model from a catalog JSON string.
	 *
	 */
	public static Catalog load(String jsonString) throws IOException {
		Gson gson = getGSon();
		CatalogImpl md = gson.fromJson(jsonString, CatalogImpl.class);
		return md;
	}

	private static final GsonBuilder gsonBuilder;

	static {
		GsonBuilder bld = new GsonBuilder();
		bld.setPrettyPrinting();
		bld.excludeFieldsWithoutExposeAnnotation();

		// CatalogResource
		bld.registerTypeAdapter(new TypeToken<EList<CatalogResource>>() {
		}.getType(), new EListSerializer<CatalogResource>());
		bld.registerTypeAdapter(new TypeToken<CatalogResourceImpl>() {
		}.getType(), new CatalogResourceImpl.JsonAdapter());

		// CatalogEdge
		bld.registerTypeAdapter(new TypeToken<EList<CatalogEdge>>() {
		}.getType(), new EListSerializer<CatalogEdge>());
		bld.registerTypeAdapter(new TypeToken<CatalogEdgeImpl>() {
		}.getType(), new CatalogEdgeImpl.JsonAdapter());

		// List of Strings
		bld.registerTypeAdapter(new TypeToken<EList<String>>() {
		}.getType(), new EListSerializer<String>());

		// Catalog
		bld.registerTypeAdapter(new TypeToken<CatalogImpl>() {
		}.getType(), new CatalogImpl.JsonAdapter());

		// CatalogMetadata
		bld.registerTypeAdapter(new TypeToken<CatalogMetadataImpl>() {
		}.getType(), new CatalogMetadataImpl.JsonAdapter());

		// CatalogResourceParameter
		bld.registerTypeAdapter(new TypeToken<CatalogResourceParameterImpl>() {
		}.getType(), new CatalogResourceParameterImpl.JsonAdapter());
		bld.registerTypeAdapter(new TypeToken<EList<CatalogResourceParameter>>() {
		}.getType(), new EListSerializer<CatalogResourceParameter>());

		gsonBuilder = bld;
	}
}
