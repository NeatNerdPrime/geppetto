/**
 * This code was initially copied from the egit-github project
 *
 *  http://git.eclipse.org/c/egit/egit-github.git/
 *
 * and then adjusted for this API. The original contained this copyright:
 *
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *   Thomas Hallgren (Puppet Labs, Inc.) - Stackhammer changes
 */
package com.puppetlabs.geppetto.forge.client;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.puppetlabs.geppetto.common.annotations.Nullable;
import com.puppetlabs.geppetto.forge.client.Authenticator.AuthResponse;
import com.puppetlabs.geppetto.forge.model.Constants;

/**
 * Class responsible for all request and response processing
 */
public class ForgeHttpClient implements Constants, ForgeClient {
	static InputStream getStream(HttpEntity entity) throws IOException {
		if(entity == null)
			return null;

		return entity.getContent();
	}

	public static final Charset UTF_8 = Charset.forName("UTF-8");

	@Inject
	@Named(Constants.BASE_URL_NAME)
	private String baseURL;

	@Inject
	@Named(Constants.API_V1_URL_NAME)
	private String v1URL;

	@Inject
	@Named(Constants.API_V2_URL_NAME)
	private String v2URL;

	@Inject
	@Named(Constants.API_V3_URL_NAME)
	private String v3URL;

	@Inject
	private Gson gson;

	@Inject
	private HttpClient httpClient;

	@Inject(optional = true)
	@Nullable
	private Authenticator authenticator;

	private String userAgent = USER_AGENT;

	private transient String credentials;

	private transient HttpRequestBase currentRequest;

	@Override
	public synchronized void abortCurrentRequest() {
		if(currentRequest != null) {
			currentRequest.abort();
			currentRequest = null;
		}
	}

	private synchronized void abortRequest() {
		if(currentRequest != null)
			currentRequest.abort();
		currentRequest = null;
	}

	protected void assignJSONContent(HttpEntityEnclosingRequestBase request, Object params) {
		if(params != null) {
			request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON + "; charset=" + UTF_8.name()); //$NON-NLS-1$
			byte[] data = toJson(params).getBytes(UTF_8);
			request.setEntity(new ByteArrayEntity(data));
		}
	}

	@Override
	public void authenticate() throws IOException {
		if(credentials == null && authenticator != null) {
			AuthResponse auth = authenticator.authenticate(httpClient);
			this.credentials = "Bearer " + auth.getToken();
		}
	}

	protected void configureRequest(final HttpRequestBase request) {
		if(credentials != null)
			request.addHeader(HttpHeaders.AUTHORIZATION, credentials);
		else
			request.addHeader(HttpHeaders.USER_AGENT, userAgent);
	}

	private HttpGet createGetRequest(String urlStr, Map<String, String> params) {
		StringBuilder bld = new StringBuilder(urlStr);
		if(params != null && !params.isEmpty()) {
			List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
			for(Map.Entry<String, String> param : params.entrySet())
				pairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
			bld.append('?');
			bld.append(URLEncodedUtils.format(pairs, UTF_8.name()));
		}
		return new HttpGet(URI.create(bld.toString()));
	}

	/**
	 * Create full v1 URI from path
	 *
	 * @param path
	 * @return uri
	 */
	protected String createV1Uri(final String path) {
		return v1URL + path;
	}

	/**
	 * Create full v2 URI from path
	 *
	 * @param path
	 * @return uri
	 */
	protected String createV2Uri(final String path) {
		return v2URL + path;
	}

	@Override
	public void delete(final String uri) throws IOException {
		HttpDelete request = new HttpDelete(createV2Uri(uri));
		configureRequest(request);
		executeRequest(request, null);
	}

	private void doDownload(String urlStr, Map<String, String> params, final OutputStream output) throws IOException {
		HttpGet request = createGetRequest(urlStr, params);
		configureRequest(request);
		startRequest(request);
		try {
			httpClient.execute(request, new ResponseHandler<Void>() {
				@Override
				public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					StatusLine statusLine = response.getStatusLine();
					int code = statusLine.getStatusCode();
					if(code != HttpStatus.SC_OK)
						throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());

					HttpEntity entity = response.getEntity();
					entity.writeTo(output);
					return null;
				}
			});
		}
		finally {
			endRequest();
		}
	}

	private <V> V doGet(String urlStr, Map<String, String> params, Type type) throws IOException {
		HttpGet request = createGetRequest(urlStr, params);
		configureRequest(request);
		return executeRequest(request, type);
	}

	@Override
	public InputStream download(String urlStr, Map<String, String> params) throws IOException {
		HttpGet request = createGetRequest(v3URL + urlStr, params);
		configureRequest(request);
		startRequest(request);
		HttpResponse response = httpClient.execute(request);
		StatusLine statusLine = response.getStatusLine();
		int code = statusLine.getStatusCode();
		if(code != HttpStatus.SC_OK) {
			abortRequest();
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

		return new FilterInputStream(response.getEntity().getContent()) {
			@Override
			public void close() throws IOException {
				super.close();
				endRequest();
			}
		};
	}

	@Override
	public void download(String urlStr, Map<String, String> params, final OutputStream output) throws IOException {
		doDownload(v3URL + urlStr, params, output);
	}

	@Override
	public void downloadV2(String urlStr, Map<String, String> params, final OutputStream output) throws IOException {
		doDownload(v2URL + urlStr, params, output);
	}

	private synchronized void endRequest() {
		currentRequest = null;
	}

	protected <V> V executeRequest(final HttpRequestBase request, final Type type) throws IOException {
		startRequest(request);
		try {
			return httpClient.execute(request, new JSonResponseHandler<V>(gson, type));
		}
		finally {
			endRequest();
		}
	}

	@Override
	public <V> V get(String urlStr, Map<String, String> params, Type type) throws IOException {
		return doGet(v3URL + urlStr, params, type);
	}

	@Override
	public <V> V getBaseRelative(String urlStr, Map<String, String> params, Type type) throws IOException {
		return doGet(baseURL + urlStr, params, type);
	}

	@Override
	public <V> V getV1(String urlStr, Map<String, String> params, Type type) throws IOException {
		return doGet(v1URL + urlStr, params, type);
	}

	@Override
	public <V> V getV2(String urlStr, Map<String, String> params, Type type) throws IOException {
		return doGet(v2URL + urlStr, params, type);
	}

	@Override
	public <V> V patch(final String uri, final Object params, final Class<V> type) throws IOException {
		// HttpPatch is introduced in 4.2. We need to be compatible with 4.1 in order to
		// play nice with other Juno and Kepler features
		HttpPost request = new HttpPost(createV2Uri(uri)) {
			@Override
			public String getMethod() {
				return "PATCH";
			}
		};

		configureRequest(request);
		assignJSONContent(request, params);
		return executeRequest(request, type);
	}

	@Override
	public void post(String uri) throws IOException {
		postJSON(uri, null, null);
	}

	@Override
	public <V> V postJSON(final String uri, final Object params, final Class<V> type) throws IOException {
		HttpPost request = new HttpPost(createV2Uri(uri));
		configureRequest(request);
		assignJSONContent(request, params);
		return executeRequest(request, type);
	}

	@Override
	public <V> V postUpload(String uri, Map<String, String> stringParts, InputStream in, String mimeType, String fileName,
			final long fileSize, Class<V> type) throws IOException {
		HttpPost request = new HttpPost(createV2Uri(uri));
		configureRequest(request);

		MultipartEntity entity = new MultipartEntity();
		for(Map.Entry<String, String> entry : stringParts.entrySet())
			entity.addPart(entry.getKey(), StringBody.create(entry.getValue(), "text/plain", UTF_8));

		entity.addPart("file", new InputStreamBody(in, mimeType, fileName) {
			@Override
			public long getContentLength() {
				return fileSize;
			}
		});
		request.setEntity(entity);
		return executeRequest(request, type);
	}

	@Override
	public void put(String uri) throws IOException {
		put(uri, null, null);
	}

	@Override
	public <V> V put(final String uri, final Object params, final Class<V> type) throws IOException {
		HttpPut request = new HttpPut(createV2Uri(uri));
		configureRequest(request);
		assignJSONContent(request, params);
		return executeRequest(request, type);
	}

	@Override
	public ForgeClient setUserAgent(final String agent) {
		if(agent != null && agent.length() > 0)
			userAgent = agent;
		else
			userAgent = USER_AGENT;
		return this;
	}

	private synchronized void startRequest(HttpRequestBase request) {
		if(currentRequest != null)
			currentRequest.abort();
		currentRequest = request;
	}

	/**
	 * Convert object to a JSON string
	 *
	 * @param object
	 * @return JSON string
	 * @throws IOException
	 */
	protected String toJson(Object object) {
		return gson.toJson(object);
	}
}
