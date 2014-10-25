/*
  Copyright 2012 - 2014 Jerome Leleu

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.play.java;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.context.BaseResponseContext;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.play.StorageHelper;

import play.mvc.Http.Context;
import play.mvc.Http.Session;

/**
 * This class is the Java web context for Play. "Session objects" are stored into cache.
 * 
 * @author Jerome Leleu
 * @since 1.1.0
 */
public class JavaWebContext extends BaseResponseContext {

    // play api does not expose the scheme, just return http for now
    private String scheme = "http";

	private Context context;

    public JavaWebContext(final Context context) {
        this.context = context;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getRequestHeader(final String name) {
        return context.request().getHeader(name);
    }

    public String getRequestMethod() {
        return context.request().method();
    }

    public String getRequestParameter(final String name) {
        final Map<String, String[]> parameters = getRequestParameters();
        final String[] values = parameters.get(name);

        if (values != null && values.length > 0) {
            return values[0];
        }

        return null;
    }

    public Map<String, String[]> getRequestParameters() {
        final Map<String, String[]> formParameters = context.request().body().asFormUrlEncoded();
        final Map<String, String[]> urlParameters = context.request().queryString();
        final Map<String, String[]> parameters = new HashMap<String, String[]>();

        if (formParameters != null) {
            parameters.putAll(formParameters);
        }

        if (urlParameters != null) {
            parameters.putAll(urlParameters);
        }

        return parameters;
    }

    public Object getSessionAttribute(final String key) {
        String sessionId = StorageHelper.getSessionId(Context.current());

        if (CommonHelper.isNotBlank(sessionId)) {
            return StorageHelper.get(sessionId, key);
        }

        return null;
    }

    public void setSessionAttribute(final String key, final Object value) {
        String sessionId = StorageHelper.getSessionId(Context.current());

        if (CommonHelper.isNotBlank(sessionId)) {
            StorageHelper.save(sessionId, key, value);
        }
    }

    @Override
    public void setResponseHeader(final String name, final String value) {
        context.response().setHeader(name, value);
    }

    public Session getSession() {
        return context.session();
    }

    public String getServerName() {
        String[] split = context.request().host().split(":");

        return split[0];
    }

    public int getServerPort() {
        String[] split = context.request().host().split(":");
        String portStr = (split.length > 1) ? split[1] : "80";

        return Integer.valueOf(portStr);
    }

    public String getScheme() {
        return this.scheme;
    }

    public String getFullRequestURL() {
        return getScheme() + "://" + context.request().host() + context.request().uri();
    }
}
