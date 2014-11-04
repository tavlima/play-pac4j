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

import org.apache.commons.lang3.StringUtils;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.RedirectAction;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.CallbackHandler;
import org.pac4j.play.Config;
import org.pac4j.play.StorageHelper;
import org.pac4j.play.Utils;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http.Context;

/**
 * This controller is the Java controller to retrieve the user profile or the redirection url to start the authentication process.
 * 
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class JavaControllerHelper extends CallbackHandler {

	public JavaControllerHelper() {
		super();
		logger = Logger.of(getClass());
	}

	/**
	 * This method returns the url of the provider where the user must be redirected for authentication.
	 * The current requested url is saved into session to be restored after authentication.
	 * 
	 * @param clientName
	 * @return the url of the provider where to redirect the user
	 */
	public RedirectAction getRedirectAction(final String clientName) {
		return getRedirectAction(clientName, null);
	}

	/**
	 * This method returns the url of the provider where the user must be redirected for authentication.
	 * The input <code>targetUrl</code> (or the current requested url if <code>null</code>) is saved into session to be restored after
	 * authentication.
	 * 
	 * @param clientName
	 * @param targetUrl
	 * @return the url of the provider where to redirect the user
	 */
	public RedirectAction getRedirectAction(final String clientName, final String targetUrl) {
		// get or create session id
		String sessionId = StorageHelper.getOrCreateSessionId(Context.current());
		// requested url to save
		final String requestedUrlToSave = Utils.getOrElse(targetUrl, Controller.request().uri());
		logger.trace("requestedUrlToSave : {}", requestedUrlToSave);
		StorageHelper.saveRequestedUrl(sessionId, requestedUrlToSave);
		// clients
		Clients clients = Config.getClients();
		// no clients -> misconfiguration ?
		if (clients == null) {
			throw new TechnicalException("No client defined. Use Config.setClients(clients)");
		}
		// redirect to the provider for authentication
		JavaWebContext webContext = new JavaWebContext(Context.current());
		RedirectAction action = null;
		try {
			action = ((BaseClient) clients.findClient(clientName)).getRedirectAction(webContext, false, false);
		} catch (RequiresHttpAction e) {
			// should not happen
		}
		logger.trace("redirectAction : {}", action);
		return action;
	}

	/**
	 * This method returns the user profile if the user is authenticated or <code>null</code> otherwise.
	 * 
	 * @return the user profile if the user is authenticated or <code>null</code> otherwise
	 */
	public CommonProfile getUserProfile() {
		// get the session id
		// TODO verificar se nao vai quebrar (pegava a sessao, sem criar uma nova)
		final String sessionId = StorageHelper.getOrCreateSessionId(Context.current());
		logger.trace("sessionId for profile: {}", sessionId);
		if (StringUtils.isNotBlank(sessionId)) {
			// get the user profile
			final CommonProfile profile = StorageHelper.getProfile(sessionId);
			logger.trace("profile : {}", profile);
			return profile;
		}
		return null;
	}
}
