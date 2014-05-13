/////////////////////////////////////////////////////////////
// DoAuthorization.java
// gooru-api
// Created by Gooru on 2014
// Copyright (c) 2014 Gooru. All rights reserved.
// http://www.goorulearning.org/
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
/////////////////////////////////////////////////////////////
package org.ednovo.gooru.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.ednovo.gooru.core.api.model.ApiKey;
import org.ednovo.gooru.core.api.model.GooruAuthenticationToken;
import org.ednovo.gooru.core.api.model.Organization;
import org.ednovo.gooru.core.api.model.SessionContextSupport;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.api.model.UserCredential;
import org.ednovo.gooru.core.api.model.UserToken;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.security.AuthenticationDo;
import org.ednovo.gooru.domain.service.apitracker.ApiTrackerService;
import org.ednovo.gooru.domain.service.oauth.OAuthService;
import org.ednovo.gooru.domain.service.redis.RedisService;
import org.ednovo.gooru.domain.service.user.UserService;
import org.ednovo.gooru.infrastructure.persistence.hibernate.OrganizationSettingRepository;
import org.ednovo.gooru.infrastructure.persistence.hibernate.UserTokenRepository;
import org.ednovo.goorucore.application.serializer.ExcludeNullTransformer;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import flexjson.JSONSerializer;

@Component
public class DoAuthorization  {

	@Autowired
	private UserService userService;
	
	@Autowired
	private ApiTrackerService apiTrackerService;
	
	@Autowired
	private OrganizationSettingRepository organizationSettingRepository;
	
	@Autowired
	private UserTokenRepository userTokenRepository;
	
	@Autowired
	private RedisService redisService;
	
	@Autowired
	private OAuthService oAuthService;
	
	private static final String SESSION_TOKEN_KEY = "authenticate_";
	
	private static final Logger logger = LoggerFactory.getLogger(DoAuthorization.class);
	
	public User doFilter(String sessionToken, String pinToken, String apiKeyToken, HttpServletRequest request, HttpServletResponse response, Authentication auth, String oAuthToken) { 
		if (pinToken != null) { 
			sessionToken = pinToken;
		}
		User user = null;
		//boolean isSussess = true;
		AuthenticationDo authentication = null;
		UserToken	userToken = null;
		String key = null;
		String data = null;
		String skipCache = request.getParameter("skipCache");
		
	    if(oAuthToken != null){
			try { 
				key = SESSION_TOKEN_KEY  + oAuthToken;
				 data = getRedisService().getValue(key);
				if (data != null && (skipCache == null || skipCache.equals("0"))) { 
					authentication = JsonDeserializer.deserialize(data, AuthenticationDo.class);
				}
			}  catch(Exception e) { 
				logger.error("Failed to  get  value from redis server");
			}
			if (authentication == null || authentication.getUserToken() == null)  {
		    	try {
					user = oAuthService.getUserByOAuthAccessToken(oAuthToken);
				} catch (Exception e) {
					logger.error("OAuth Authentication failed --- " + e);
				}
				userToken = userToken == null ? new UserToken() : userToken;
				userToken.setUser(user);
			} else { 
				userToken = authentication.getUserToken();
			}
			if(userToken == null) {
				throw new AccessDeniedException("Invalid oauth access token : " + oAuthToken);
			} else { 
				user = userToken.getUser();
			}
	    } else if(sessionToken != null) {
			try { 
				key = SESSION_TOKEN_KEY  + sessionToken;
				 data = getRedisService().getValue(key);
				if (data != null && (skipCache == null || skipCache.equals("0"))) { 
					authentication = JsonDeserializer.deserialize(data, AuthenticationDo.class);
				}
			}  catch(Exception e) { 
				logger.error("Failed to  get  value from redis server");
			}
			if (authentication == null || authentication.getUserToken() == null)  {
		      userToken = userTokenRepository.findByToken(sessionToken);
			} else { 
				userToken = authentication.getUserToken();
			}
			if(userToken == null) {
				throw new AccessDeniedException("Invalid session token : " + sessionToken);
			} else { 
				user = userToken.getUser();
			}
			
			String token = redisService.getValue(sessionToken);
			if(token == null && userToken.getScope().equalsIgnoreCase("expired")){
				response.setStatus(HttpStatus.SC_FORBIDDEN);
				throw new AccessDeniedException("error:Session is Expired.");
			}
			else if(sessionToken != null){
		        Organization organization = null;
		        if(userToken.getApiKey() != null){
		        	organization = userToken.getApiKey().getOrganization();
		        }
				redisService.addSessionEntry(sessionToken, organization);
			}
		} else if(apiKeyToken != null) {
			key = SESSION_TOKEN_KEY  + apiKeyToken;
			data = getRedisService().getValue(key);
			if (data != null && (skipCache == null || skipCache.equals("0"))) { 
				authentication = JsonDeserializer.deserialize(data, AuthenticationDo.class);
			}
			if (authentication == null)  {
				ApiKey apiKey = apiTrackerService.getApiKey(apiKeyToken);
				if (apiKey == null) {
					throw new AccessDeniedException("Invalid ApiKey : " + apiKeyToken);
				} else {
					String anonymousUid = organizationSettingRepository.getOrganizationSetting(Constants.ANONYMOUS, apiKey.getOrganization().getPartyUid());
					user = userService.findByGooruId(anonymousUid);
					userToken = userToken == null ? new UserToken() : userToken;
					userToken.setUser(user);
				}
			}
		} else { 
			throw new AccessDeniedException("Session token or api key is mandatory.");
		}
		if (authentication == null)  {
			authentication = new AuthenticationDo();
			authentication.setUserToken(userToken);		
		}
		if (authentication.getUserToken().getUser() == null) {
			throw new AccessDeniedException("Invalid session token : " + sessionToken);
		} 
		// check token expires
		if (authentication.getUserToken().getUser() != null && (auth == null || hasRoleChanged(auth, authentication.getUserToken().getUser()))) {
			doAuthentication(request, response, authentication.getUserToken().getUser(), authentication.getUserToken().getToken(), skipCache, authentication, key);
		}
		JSONObject session = new JSONObject();
			try {
				session.put("sessionToken", sessionToken);
				session.put("organizationUId",  authentication.getUserToken().getUser().getOrganization().getPartyUid());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			SessionContextSupport.putLogParameter("session", session.toString());
		
		
		if(oAuthToken != null){
			SessionContextSupport.putLogParameter("oauthAccessToken", oAuthToken);
		}

		// set to request so that controllers can read it.
		request.setAttribute(Constants.USER, authentication.getUserToken().getUser());
		
		return authentication.getUserToken().getUser();
	}
	
	private Authentication doAuthentication(HttpServletRequest request, HttpServletResponse response, User user, String sessionToken, String skipCache,  AuthenticationDo authentication, String key) {
		Authentication auth = null;
		if (user != null) { 
			UserCredential  userCredential = null;
			if (authentication.getUserCredential() == null || !(skipCache == null || skipCache.equals("0"))) {
				userCredential = userService.getUserCredential(user, sessionToken, skipCache, request.getParameter("sharedSecretKey"));				
				authentication.setUserCredential(userCredential);
			} else { 
				userCredential = authentication.getUserCredential();
			}
			try {
				getRedisService().putValue(key, new JSONSerializer().transform(new ExcludeNullTransformer(), void.class).include(new String[] {"*.operationAuthorities","*.userRoleSet", "*.partyOperations", "*.subOrganizationUids", "*.orgPermits", "*.partyPermits", "*.customFields", "*.identities", "*.meta"}).exclude("*.class").serialize(authentication), 1800);
			} catch (Exception e) { 
				logger.error("Failed to  put  value from redis server");	
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Authorize User: First Name-" + user.getFirstName() + "; Last Name-" + user.getLastName() + "; Email-" + user.getUserId());
			}
			auth = new GooruAuthenticationToken(user.getPartyUid(), null, userCredential);
			SecurityContextHolder.getContext().setAuthentication(auth);

		}
		return auth;
	}

	private boolean hasRoleChanged(Authentication auth, User user) {
		if (!user.getPartyUid().equals((String) auth.getPrincipal())) {
			return true;
		}
		return false;
	}
	
	
	public RedisService getRedisService() {
		return redisService;
	}
}
