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
package utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import models.Authentication;
import models.Owner;
import models.UserMembership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Application;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

/**
 * Authenticates users with session cookie or github enterprise's OAuth2 provider.
 *
 * @author Brandon Arp (barp at groupon dot com)
 */
@Singleton
public class AuthN extends Security.Authenticator {
    /**
     * Public constructor.
     *
     * @param application the running application
     * @param configuration application configuration
     */
    @Inject
    public AuthN(final Application application, final Config configuration) {
        _application = application;
        _configuration = configuration;
    }

    @Override
    public Optional<String> getUsername(final Http.Request request) {
        final boolean useDefaultLogin = _application.isDev() && _configuration.getBoolean("auth.useDefaultLogin");
        if (request.session().data().containsKey("auth-id")) {
            LOGGER.debug("Found auth id in session cookie");
            final String authId = request.session().data().get("auth-id");
            if (TOKEN_CACHE.getIfPresent(authId) != null) {
                return Optional.of(authId);
            } else {
                final Authentication authentication = Authentication.findByUserName(authId);
                if (authentication != null) {
                    TOKEN_CACHE.put(authId, authentication.getToken());
                    return Optional.of(authId);
                } else if (useDefaultLogin) {
                    // Dev mode, we dont need a token for the user
                    // If the user has an existing group, then we can assume the database wasn't just reset
                    if (getOrganizations(authId).size() == 0) {
                        LOGGER.debug(String.format("Did not find orgs for user=%s, storing the defaults", authId));
                        initializeAuthenticatedSession(request, authId, _configuration.getStringList("dev.defaultGroups"));
                    }
                    return Optional.of(authId);
                } else {
                    LOGGER.debug("Did not find authentication in database, sending user through auth flow");
                }
            }
        } else if (useDefaultLogin) {
            LOGGER.debug("Did not find auth id in sesion cookie, using defaults");
            final String userName = _configuration.getString("dev.defaultUser");
            initializeAuthenticatedSession(request, userName, _configuration.getStringList("dev.defaultGroups"));
            return Optional.of(userName);
        }

        return Optional.empty();
    }

    @Override
    public Result onUnauthorized(final Http.Request request) {
        return redirect(controllers.routes.Authentication.auth(request.uri()));
    }

    /**
     * Starts a session and sets the session cookies.
     *
     * @param request play request context
     * @param userName the user name
     * @param organizations the list of organizations the user is a part of
     */
    public static void initializeAuthenticatedSession(final Http.Request request, final String userName, final List<String> organizations) {
        final List<Owner> orgList = Lists.newArrayList();
        for (final String organization : organizations) {
            Owner org = Owner.getByName(organization);
            try {
                if (org == null) {
                    org = new Owner();
                    org.setOrgName(organization);
                    org.save();
                }
            } catch (final PersistenceException e) {
                LOGGER.warn("Unable to create organization", e);
                org = Owner.getByName(organization);
            }
            orgList.add(org);

            try {
                UserMembership membership = UserMembership.getByUserAndOrg(userName, organization);
                if (membership == null) {
                    membership = new UserMembership();
                    membership.setOrg(org);
                    membership.setUserName(userName);
                    membership.save();
                }
            } catch (final PersistenceException e) {
                LOGGER.warn("Unable to create membership", e);
            }
        }
        USER_ORGS.put(userName, orgList);
        request.session().adding("auth-id", userName);
        LOGGER.info("added user " + userName + " to session.");
    }

    /**
     * Gets a list of organizations for a user.
     *
     * @param userName the user name
     * @return a list of organizations the user is an owner in
     */
    public static List<Owner> getOrganizations(final String userName) {
        final List<Owner> owners = USER_ORGS.getIfPresent(userName);
        if (owners != null) {
            return owners;
        } else {
            final List<Owner> ownerList = UserMembership.getOrgsForUser(userName);
            USER_ORGS.put(userName, ownerList);
            return ownerList;
        }
    }

    /**
     * Log out the user.
     *
     * @param ctx the play request context
     */
    public static void logout(final Http.Request ctx) {
        ctx.session().data().clear();
    }

    /**
     * Saves an oAuth token for a user.
     *
     * @param userName the user name
     * @param token the user's token
     */
    public static void storeToken(final String userName, final String token) {
        try {
            Authentication auth = Authentication.findByUserName(userName);
            if (auth == null) {
                auth = new Authentication();
                auth.setUserName(userName);
            }
            auth.setToken(token);
            auth.save();
        } catch (final PersistenceException e) {
            LOGGER.warn("Unable to save token", e);
        }
        TOKEN_CACHE.put(userName, token);
    }

    private final Application _application;
    private final Config _configuration;

    private static final Cache<String, String> TOKEN_CACHE = CacheBuilder.newBuilder().build();
    private static final Cache<String, List<Owner>> USER_ORGS = CacheBuilder.newBuilder().build();
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthN.class);
}
