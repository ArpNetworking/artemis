/**
 * Copyright 2015 Groupon.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import controllers.Authentication;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.libs.F;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import utils.AuthN;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Holds methods for authentication.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
public class StandardAuthentication extends Controller implements Authentication {
    /**
     * Public constructor.
     */
    @Inject
    public StandardAuthentication() {
    }

    @Override
    public F.Promise<Result> auth(final String redirectUrl) {
        final Configuration config = Configuration.root();
        final String baseURL = config.getString("auth.ghe.baseURL");
        final String clientId = config.getString("auth.ghe.clientId");

        final String state = new BigInteger(160, RANDOM).toString(32);
        // TODO(barp): This should go in the database or memcached
        URL_CACHE.put(state, redirectUrl);

        final URI uri;
        try {
            uri = apiUriBuilder(baseURL, "/login/oauth/authorize")
                    .addParameter("client_id", clientId)
                    .addParameter("scope", "user:email,read:org")
                    .addParameter("state", state)
                    .build();
        } catch (final URISyntaxException e) {
            LOGGER.error("Unable to build URI for GHE authentication", e);
            return F.Promise.pure(internalServerError());
        }

        LOGGER.info("Redirecting login to " + uri.toString());

        return F.Promise.pure(redirect(uri.toString()));
    }

    @Override
    public F.Promise<Result> finishAuth(final String code, final String state) {
        final Configuration config = Configuration.root();
        final String baseURL = config.getString("auth.ghe.baseURLApi");
        final String clientId = config.getString("auth.ghe.clientId");
        final String clientSecret = config.getString("auth.ghe.clientSecret");
        final String redirect = URL_CACHE.getIfPresent(state);
        LOGGER.info(String.format("Got an auth callback; code=%s, state=%s", code, state));

        if (redirect != null) {
            URL_CACHE.invalidate(state);

            //First, we need to get the access token to make the organizations API call
            final String relativePath = "/login/oauth/access_token";

            final URI tokenPostUri;
            try {
                tokenPostUri = apiUriBuilder(baseURL, relativePath)
                        .addParameter("client_id", clientId)
                        .addParameter("client_secret", clientSecret)
                        .addParameter("code", code)
                        .build();
            } catch (final URISyntaxException e) {
                LOGGER.error("Unable to build URI for GHE authentication", e);
                return F.Promise.<Result>pure(internalServerError());
            }
            return WS.client()
                    .url(tokenPostUri.toString())
                    .setHeader("Accept", "application/json")
                    .post("")
                    .flatMap(wsResponse -> {
                        final String response = wsResponse.getBody();
                        LOGGER.info(response);
                        final JsonNode tokenJson = wsResponse.asJson();
                        final String accessToken = tokenJson.get("access_token").asText();
                        LOGGER.info(String.format("Got access token; token=%s", accessToken));
                        return lookupUsername(baseURL, accessToken).flatMap(userName -> {
                                LOGGER.info(String.format("Found user name; name=%s", userName));
                                return lookupUserOrgs(baseURL, accessToken).map(orgs -> {
                                        LOGGER.info(String.format("Found orgs for user; user=%s, orgs=%s", userName, orgs));
                                        AuthN.initializeAuthenticatedSession(ctx(), userName, orgs);
                                        AuthN.storeToken(userName, accessToken);
                                        return redirect(redirect);
                                    }
                                );
                            }
                        );
                    });
        } else {
            LOGGER.info("URL_CACHE: " + URL_CACHE);
            for (final Map.Entry<String, String> entry : URL_CACHE.asMap().entrySet()) {
                LOGGER.info(String.format("entry:   %s :: %s", entry.getKey(), entry.getValue()));
            }
            return F.Promise.<Result>pure(unauthorized("Something went wrong. Please try logging in again."));
        }
    }

    private F.Promise<String> lookupUsername(final String baseURL, final String token) {
        return WS.client()
                .url(apiUri(baseURL, "user").toString())
                .setHeader("Accept", "application/json")
                .setHeader("Authorization", String.format("token %s", token))
                .get()
                .map(wsResponse -> {
                        //We have the user details, but still need to fetch the organizations
                        final JsonNode userJson = wsResponse.asJson();
                        return userJson.get("login").asText();
                    }
                );
    }

    private F.Promise<List<String>> lookupUserOrgs(final String baseURL, final String token) {
        return WS.client()
                .url(apiUri(baseURL, "user/orgs").toString())
                .setHeader("Authorization", String.format("token %s", token))
                .setHeader("Accept", "application/json")
                .get()
                .map(wsResponse -> {
                        final ArrayNode orgs = (ArrayNode) wsResponse.asJson();
                        final List<String> orgList = Lists.newArrayList();
                        for (final JsonNode org : orgs) {
                            final String orgName = org.get("login").asText();
                            orgList.add(orgName);
                        }

                        return orgList;
                    }
                );
    }

    private static URI apiUri(final String baseURL, final String relativePath) {
        try {
            return apiUriBuilder(baseURL, relativePath).build();
        } catch (final URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    private static URIBuilder apiUriBuilder(final String baseURL, final String relativePath) {
        final URI tokenUri;
        try {
            tokenUri = new URI(baseURL).resolve(relativePath);
        } catch (final URISyntaxException e) {
            LOGGER.error(String.format("Unable to parse baseURL for GHE authentication; baseURL=%s", baseURL), e);
            throw Throwables.propagate(e);
        }
        return new URIBuilder(tokenUri);
    }

    @Override
    public F.Promise<Result> logout() {
        AuthN.logout(ctx());
        return F.Promise.pure(redirect("/loggedout"));
    }

    private static final Cache<String, String> URL_CACHE = CacheBuilder.newBuilder().expireAfterWrite(90, TimeUnit.SECONDS).build();
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardAuthentication.class);
    private static final SecureRandom RANDOM = new SecureRandom();
}
