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
package org.pac4j.play;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.cache.Cache;
import play.mvc.Http.Context;
import play.mvc.Http.Session;

/**
 * This class is an helper to store/retrieve objects (from cache).
 * 
 * @author Jerome Leleu
 * @since 1.1.0
 */
public final class StorageHelper {
	
	private static final Logger logger = LoggerFactory.getLogger(StorageHelper.class);

	private static final String CTX_SESSION_ID = StorageHelper.class.getCanonicalName() + "." + "sessionid";

	/**
	 * Get a session identifier.
	 * 
	 * @param ctx context
	 * @return the session identifier
	 */
	public static String getSessionId(final Context ctx) {
		// get current sessionId
		String sessionId = null;

		String[] authHeaders = ctx.request().headers().get(Constants.HTTP_HEADER);

		if (authHeaders != null && authHeaders.length > 0) {
			sessionId = authHeaders[0];
			logger.debug("'{}' header found: {}", Constants.HTTP_HEADER, sessionId);

		} else {
			sessionId = ctx.session().get(Constants.SESSION_ID);
		}

		return sessionId;
	}
	
	/**
	 * Clear session id
	 * 
	 * @param ctx context
	 */
	public static void clearSessionId(final Context ctx) {
		ctx.session().remove(Constants.SESSION_ID);
	}

	private static String getOrCreateSessionId(Map<String,String[]> headers, Session session) {
		String sessionId = null;

		String[] authHeaders = headers.get(Constants.HTTP_HEADER);

		if (authHeaders != null && authHeaders.length > 0) {
			sessionId = authHeaders[0];
			logger.debug("sessionId found ({}): {}", Constants.HTTP_HEADER, sessionId);

		} else {
			sessionId = session.get(Constants.SESSION_ID);

			if (sessionId != null) {
				logger.debug("sessionId found (Cookie): {}", sessionId);
			}
		}

		// if null, generate a new one
		if (sessionId == null) {
			// generate id for session
			sessionId = generateSessionId();
			logger.debug("sessionId created: {}", sessionId);
		}

		// save it to session
		session.put(Constants.SESSION_ID, sessionId);

		return sessionId;
	}

	/**
	 * Get (or create) a session identifier.
	 * 
	 * @param ctx context
	 * @return the session identifier
	 */
	public static String getOrCreateSessionId(final Context ctx) {
		String ret = (String) ctx.args.get(CTX_SESSION_ID);

		if (ret == null) {
			ret = getOrCreateSessionId(ctx.request().headers(), ctx.session());

			ctx.args.put(CTX_SESSION_ID, ret);
		}

		return ret;
	}
	
	/**
	 * Generate a session identifier.
	 * 
	 * @return a session identifier
	 */
	public static String generateSessionId() {
		return java.util.UUID.randomUUID().toString();
	}
	
	/**
	 * Get the profile from storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @return the user profile
	 */
	public static CommonProfile getProfile(final String sessionId) {
		if (sessionId != null) {
			return (CommonProfile) get(sessionId);
		}
		return null;
	}
	
	/**
	 * Save a user profile in storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param profile the user profile
	 */
	public static void saveProfile(final String sessionId, final CommonProfile profile) {
		if (sessionId != null) {
			save(sessionId, profile, Config.getProfileTimeout());
		}
	}
	
	/**
	 * Remove a user profile from storage.
	 * 
	 * @param sessionId PAC4J session id
	 */
	public static void removeProfile(final String sessionId) {
		if (sessionId != null) {
			remove(sessionId);
		}
	}
	
	/**
	 * Get a requested url from storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param clientName client name
	 * @return the requested url
	 */
	public static String getRequestedUrl(final String sessionId, final String clientName) {
		return (String) get(sessionId, clientName + Constants.SEPARATOR + Constants.REQUESTED_URL);
	}
	
	/**
	 * Save a requested url to storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param requestedUrl the requested URL
	 */
	public static void saveRequestedUrl(final String sessionId, final String requestedUrl) {
		// TODO check if should be removed from Cache on logout
		save(sessionId, Constants.REQUESTED_URL, requestedUrl);
	}
	
	/**
	 * Get an object from storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param key key to retrieve
	 * @return the object
	 */
	public static Object get(final String sessionId, final String key) {
		if (sessionId != null) {
			return get(sessionId + Constants.SEPARATOR + key);
		}
		return null;
	}
	
	/**
	 * Get an object from storage.
	 * 
	 * @param key key to restore
	 * @return the object
	 */
	static Object get(final String key) {
		return Cache.get(getCacheKey(key));
	}

	/**
	 * Save an object in storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param key key to store
	 * @param value object to store
	 */
	public static void save(final String sessionId, final String key, final Object value) {
		save(sessionId, key, value, Config.getSessionTimeout());
	}

	/**
	 * Save an object in storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param key key to store
	 * @param value object to store
	 * @param timeout cache validity
	 */
	public static void save(final String sessionId, final String key, final Object value, final int timeout) {
		if (sessionId != null) {
			if (value != null) {
				save(sessionId + Constants.SEPARATOR + key, value, Config.getSessionTimeout());
			} else {
				remove(sessionId + Constants.SEPARATOR + key);
			}
		}
	}

	/**
	 * Save an object in storage.
	 * 
	 * @param key key to store
	 * @param value object to store
	 * @param timeout cache validity
	 */
	private static void save(final String key, final Object value, final int timeout) {
		Cache.set(getCacheKey(key), value, timeout);
	}

	/**
	 * Remove an object in storage.
	 * 
	 * @param sessionId PAC4J session id
	 * @param key key to remove
	 */
	public static void remove(final String sessionId, final String key) {
		remove(sessionId + Constants.SEPARATOR + key);
	}

	/**
	 * Remove an object from storage.
	 * 
	 * @param key key to remove
	 */
	static void remove(final String key) {
		Cache.remove(getCacheKey(key));
	}

	static String getCacheKey(final String key) {
		return (StringUtils.isNotBlank(Config.getCacheKeyPrefix()))
				? Config.getCacheKeyPrefix() + ":" + key
				: key;
	}
}
