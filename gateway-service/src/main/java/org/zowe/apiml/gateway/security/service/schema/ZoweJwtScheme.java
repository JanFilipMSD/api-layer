/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpRequest;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.Cookies;

import java.util.Optional;


@Component
@AllArgsConstructor
public class ZoweJwtScheme implements IAuthenticationScheme {


    private AuthSourceService authSourceService;
    private AuthConfigurationProperties configurationProperties;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public Optional<AuthSource> getAuthSource() throws AuthSchemeException {
        return authSourceService.getAuthSourceFromRequest();
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource)
        throws AuthSchemeException {
        if (authSource == null || authSource.getRawSource() == null) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.schema.missingAuthentication");
        }

        String jwt;
        AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
            if (parsedAuthSource == null) {
                throw new IllegalStateException("Error occurred while parsing authenticationSource");
            }
            jwt = authSourceService.getJWT(authSource);
        } catch (TokenNotValidException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.invalidToken");
        } catch (TokenExpireException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.expiredToken");
        }

        final long defaultExpirationTime = System.currentTimeMillis() + configurationProperties.getTokenProperties().getExpirationInSeconds() * 1000L;
        final long expirationTime = parsedAuthSource.getExpiration() != null ? parsedAuthSource.getExpiration().getTime() : defaultExpirationTime;
        final long expireAt = Math.min(defaultExpirationTime, expirationTime);

        return new ZoweJwtAuthCommand(expireAt, jwt);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZoweJwtAuthCommand extends JwtCommand {

        public static final long serialVersionUID = -885301934611866658L;
        Long expireAt;
        String jwt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (jwt != null) {
                final RequestContext context = RequestContext.getCurrentContext();
                JwtCommand.setCookie(context, configurationProperties.getCookieProperties().getCookieName(), jwt);
            }
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            if (jwt != null) {
                Cookies cookies = Cookies.of(request);
                JwtCommand.createCookie(cookies, configurationProperties.getCookieProperties().getCookieName(), jwt);
            }
        }
    }
}
