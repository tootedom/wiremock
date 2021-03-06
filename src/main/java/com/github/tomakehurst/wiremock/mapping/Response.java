/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.mapping;

import static com.github.tomakehurst.wiremock.http.ServletContainerUtils.getUnderlyingSocketFrom;
import static com.google.common.base.Charsets.UTF_8;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.google.common.base.Optional;

public class Response {

	private int status;
	private byte[] body = new byte[0];
	private HttpHeaders headers = new HttpHeaders();
	private boolean configured = true;
	private Fault fault;
	private boolean fromProxy = false;
	
	public static Response notConfigured() {
		Response response = new Response(HTTP_NOT_FOUND);
		response.setWasConfigured(false);
		return response;
	}
	
	public Response(int status) {
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
	
	public void setBody(String body) {
		if (body == null) {
			return;
		}
		
		Optional<String> encoding = getEncodingFromHeaderIfAvailable();
		if (encoding.isPresent()) {
			this.body = body.getBytes(Charset.forName(encoding.get()));
		} else {
			this.body = body.getBytes(UTF_8);
		}
	}
	
	public void setBody(String body, String charset) {
		if (body == null) {
			return;
		}
		
		this.body = body.getBytes(Charset.forName(charset));
	}
	
	public void setBody(byte[] body) {
		if (body == null) {
			return;
		}
		
		this.body = body;
	}
	
	public String getBodyAsString() {
		Optional<String> encoding = getEncodingFromHeaderIfAvailable();
		if (encoding.isPresent()) {
			return new String(body, Charset.forName(encoding.get()));
		} else {
			return new String(body, UTF_8);
		}
	}
	
	public HttpHeaders getHeaders() {
		return headers;
	}
	
	public void addHeader(String key, String value) {
		headers.put(key, value);
	}
	
	public void addHeaders(Map<String, String> newHeaders) {
		if (newHeaders != null) {
			headers.putAll(newHeaders);
		}
	}
	
	public void applyTo(HttpServletResponse httpServletResponse) {
		if (fault != null) {
			fault.apply(httpServletResponse, getUnderlyingSocketFrom(httpServletResponse));
			return;
		}
		
		httpServletResponse.setStatus(status);
		for (Map.Entry<String, String> header: headers.entrySet()) {
			httpServletResponse.addHeader(header.getKey(), header.getValue());
		}
		
		writeAndTranslateExceptions(httpServletResponse, body);
	}
	
	private static void writeAndTranslateExceptions(HttpServletResponse httpServletResponse, byte[] content) {
		try {	
			httpServletResponse.getOutputStream().write(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Optional<String> getEncodingFromHeaderIfAvailable() {
		if (!headers.containsKey(ContentTypeHeader.KEY)) {
			return Optional.absent();
		}
		
		ContentTypeHeader contentTypeHeader = new ContentTypeHeader(headers.get(ContentTypeHeader.KEY));
		return contentTypeHeader.encodingPart();
	}

	public boolean wasConfigured() {
		return configured;
	}

	public void setWasConfigured(boolean configured) {
		this.configured = configured;
	}

	public void setFault(Fault fault) {
		this.fault = fault;
	}

    public boolean isFromProxy() {
        return fromProxy;
    }

    public void setFromProxy(boolean fromProxy) {
        this.fromProxy = fromProxy;
    }

    @Override
    public String toString() {
        return "Response [status=" + status + ", body=" + Arrays.toString(body) + ", headers=" + headers
                + ", configured=" + configured + ", fault=" + fault + ", fromProxy=" + fromProxy + "]";
    }

}
