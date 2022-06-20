/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SuccessfulAccessTokenHandler implements AuthenticationSuccessHandler {

    private final AccessTokenProvider accessTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Object expirationTime = request.getAttribute("expirationTime");
        String validity = expirationTime == null || expirationTime.equals("") ? "0" : request.getAttribute("expirationTime").toString();
        String token = accessTokenProvider.getToken(authentication.getPrincipal().toString(), Integer.parseInt(validity));
        response.getWriter().print(token);
        response.getWriter().flush();
        response.getWriter().close();
        if (!response.isCommitted()) {
            throw new IOException("Authentication response has not been committed.");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenRequest {
        private int validity;
        private Set<String> scopes;
    }
}