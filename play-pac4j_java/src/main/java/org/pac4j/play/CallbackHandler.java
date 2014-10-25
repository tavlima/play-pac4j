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
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.play.java.JavaWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * This controller is the class to finish the authentication process and logout the user.
 * <p />
 * Public methods : {@link #callback()}, {@link #logoutAndOk()} and {@link #logoutAndRedirect()} must be used in the routes file.
 * 
 * @author Jerome Leleu
 * @since 1.0.0
 */
public class CallbackHandler {

    private final Logger logger;
	private Clients clientsGroup;

    public CallbackHandler() {
		logger = LoggerFactory.getLogger(getClass());
        clientsGroup = Config.getClients();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String callbackHandler(final Context ctx) throws HTTPActionRequiredException, TechnicalException {
		// web context
    	final JavaWebContext webContext = new JavaWebContext(ctx);
    	
    	// get the client from its type
    	final BaseClient client = (BaseClient) clientsGroup.findClient(webContext);
    	logger.debug("client : {}", client);

    	Credentials credentials = null;

        try {
            credentials = client.getCredentials(webContext);
            logger.debug("credentials : {}", credentials);

        } catch (final RequiresHttpAction e) {
            // requires some specific HTTP action
            final int code = webContext.getResponseStatus();
            logger.debug("requires HTTP action : {}", code);

            if (code == HttpConstants.UNAUTHORIZED || code == HttpConstants.TEMP_REDIRECT || code == HttpConstants.OK) {
            	throw new HTTPActionRequiredException(code, webContext.getResponseContent());

            } else {
            	final String message = "Unsupported HTTP action : " + code;
            	logger.error(message);
            	throw new TechnicalException(message);
            }
        }

        // get user profile
        final CommonProfile profile = client.getUserProfile(credentials, webContext);
        logger.debug("profile : {}", profile);

        // get or create sessionId
        final String sessionId = StorageHelper.getOrCreationSessionId(Context.current());
        logger.debug("session : {}", sessionId);

        // save user profile only if it's not null
        if (profile != null) {
            StorageHelper.saveProfile(sessionId, profile);
        }

        // get requested url
        final String requestedUrl = StorageHelper.getRequestedUrl(sessionId, client.getName());

		return requestedUrl;
	}

    /**
     * This method logouts the authenticated user.
     */
    public void logout() {
        // get the session id
        final String sessionId = StorageHelper.getSessionId(Context.current());
        logger.debug("sessionId for logout : {}", sessionId);
        if (StringUtils.isNotBlank(sessionId)) {
            // remove user profile from cache
            StorageHelper.removeProfile(sessionId);
            logger.debug("remove user profile for sessionId : {}", sessionId);
        }
        StorageHelper.clearSessionId(Context.current());
    }

    /**
     * This method logouts the authenticated user and send him to a blank page.
     * 
     * @return the redirection to the blank page
     */
    public Result logoutAndOk() {
        logout();
        return Controller.ok();
    }

    /**
     * This method logouts the authenticated user and send him to the url defined in the
     * {@link Constants#REDIRECT_URL_LOGOUT_PARAMETER_NAME} parameter name or to the <code>defaultLogoutUrl</code>.
     * This parameter is matched against the {@link Config#getLogoutUrlPattern()}.
     * 
     * @return the redirection to the "logout url"
     */
    public Result logoutAndRedirect() {
        logout();
        // parameters in url
        final Map<String, String[]> parameters = Controller.request().queryString();
        final String[] values = parameters.get(Constants.REDIRECT_URL_LOGOUT_PARAMETER_NAME);
        String value = null;
        if (values != null && values.length == 1) {
            String value0 = values[0];
            // check the url pattern
            if (Config.getLogoutUrlPattern().matcher(value0).matches()) {
                value = value0;
            }
        }
        return Controller.redirect(Utils.getOrElse(value, Config.getDefaultLogoutUrl()));
    }
}
