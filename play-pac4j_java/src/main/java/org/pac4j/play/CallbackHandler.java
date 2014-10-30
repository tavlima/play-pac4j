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
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.play.java.JavaWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;

public class CallbackHandler {

	private static final String CTX_WEBCONTEXT = CallbackHandler.class.getCanonicalName() + "." + "webcontext";
	private static final String CTX_CLIENT = CallbackHandler.class.getCanonicalName() + "." + "client";
	private static final String CTX_CREDENTIALS = CallbackHandler.class.getCanonicalName() + "." + "credentials";
	private static final String CTX_PROFILE = CallbackHandler.class.getCanonicalName() + "." + "profile";
	private static final String CTX_REQUESTED_URL = CallbackHandler.class.getCanonicalName() + "." + "requestedurl";;

	private final Logger logger;

    public CallbackHandler() {
		logger = LoggerFactory.getLogger(getClass());
	}

    public JavaWebContext getWebContext(final Context ctx) {
    	JavaWebContext ret = (JavaWebContext) ctx.args.get(CTX_WEBCONTEXT);

    	if (ret == null) {
    		ret = new JavaWebContext(ctx);
    		ctx.args.put(CTX_WEBCONTEXT, ret);
    	}

    	return ret;
    }

    @SuppressWarnings("rawtypes")
	public BaseClient getClient(final Context ctx) {
    	BaseClient ret = (BaseClient) ctx.args.get(CTX_CLIENT);

    	if (ret == null) {
    		ret = (BaseClient) Config.getClients().findClient(getWebContext(ctx));
    		ctx.args.put(CTX_CLIENT, ret);
    	}

    	logger.debug("client : {}", ret);

    	return ret;
    }

	public Credentials getCredentials(final Context ctx) throws RequiresHttpAction {
    	Credentials ret = (Credentials) ctx.args.get(CTX_CREDENTIALS);

    	if (ret == null) {
			ret = getClient(ctx).getCredentials(getWebContext(ctx));
    		ctx.args.put(CTX_CREDENTIALS, ret);
    	}

    	logger.debug("credentials : {}", ret);

    	return ret;
    }

	public CommonProfile getProfile(final Context ctx) throws RequiresHttpAction {
		CommonProfile ret = null;
		
		BaseClient<?, ?> client = getClient(ctx);

		if (FacebookClient.class.isInstance(client)) {
			String accessToken = getWebContext(ctx).getRequestParameter("access_token");

			if (accessToken != null) {
				ret = FacebookClient.class.cast(client).getUserProfile(accessToken);
			}
		}

		if (ret == null) {
			ret = getProfile(ctx, getCredentials(ctx));
		}
		
		return ret;
	}

	@SuppressWarnings("unchecked")
	public CommonProfile getProfile(final Context ctx, final Credentials credentials) {
		CommonProfile ret = (CommonProfile) ctx.args.get(CTX_PROFILE);

    	if (ret == null) {
    		ret = getClient(ctx).getUserProfile(credentials, getWebContext(ctx));
    		ctx.args.put(CTX_PROFILE, ret);
    	}

    	logger.debug("profile : {}", ret);

    	return ret;
	}

	public String getRequestedUrl(final Context ctx) {
		String ret = (String) ctx.args.get(CTX_REQUESTED_URL);

		if (ret == null) {
			String sessionId = StorageHelper.getOrCreationSessionId(ctx);

			ret = StorageHelper.getRequestedUrl(sessionId, getClient(ctx).getName());

			ctx.args.put(CTX_REQUESTED_URL, ret);
		}

		logger.debug("requested_url : {}", ret);

		return ret;
	}

	public CommonProfile callbackHandler(final Context ctx) throws HTTPActionRequiredException, TechnicalException {
		CommonProfile ret = null;
		
        try {
        	// get user profile
            ret = getProfile(ctx);

            // get or create sessionId
            final String sessionId = StorageHelper.getOrCreationSessionId(Context.current());
            logger.debug("session : {}", sessionId);

            // save user profile only if it's not null
            if (ret != null) {
                StorageHelper.saveProfile(sessionId, ret);
            }

        } catch (final RequiresHttpAction e) {
        	/*
        	 * Isso aqui costumava usar o webContext direto, antes de eu fazer da forma como esta (salvando no contexto)
        	 * Nao sei se vai prejudicar essa obtencao do response status. Acho que nao. -- Thiago
        	 * 
        	 * requires some specific HTTP action
        	 */
            final int code = getWebContext(ctx).getResponseStatus();
            logger.debug("requires HTTP action : {}", code);

            if (code == HttpConstants.UNAUTHORIZED || code == HttpConstants.TEMP_REDIRECT || code == HttpConstants.OK) {
            	throw new HTTPActionRequiredException(code, getWebContext(ctx).getResponseContent());

            } else {
            	final String message = "Unsupported HTTP action : " + code;
            	logger.error(message);
            	throw new TechnicalException(message);
            }
        }

		return ret;
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
