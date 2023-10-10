/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service.scheme;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpBasicPassticketTest {

    @Test
    void givenHttpBasicPassticketInstance_whenGetAuthenticationScheme_thenReturnProperType() {
        assertEquals(AuthenticationScheme.HTTP_BASIC_PASSTICKET, new HttpBasicPassticket().getAuthenticationScheme());
    }

    @Test
    void givenRouteDefinition_whenApply_thenFulfillFilterFactorArgs() {
        RouteDefinition routeDefinition = new RouteDefinition();
        Authentication authentication = new Authentication();
        authentication.setApplid("applid");

        new HttpBasicPassticket().apply(routeDefinition, authentication);

        assertEquals(1, routeDefinition.getFilters().size());
        FilterDefinition filterDefinition = routeDefinition.getFilters().get(0);
        assertEquals("applid", filterDefinition.getArgs().get("applicationName"));
        assertEquals("PassticketFilterFactory", filterDefinition.getName());
    }

}