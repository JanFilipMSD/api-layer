/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.oauth2;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.integration.authentication.pat.ValidateRequestModel;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;

import static io.restassured.RestAssured.given;

public class OktaOauth2Test {

    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_OAUTH2_TOKEN);

    @Test
    @Tag("OktaOauth2Test")
    void givenValidAccessToken_thenValidate() {
        String accessToken = SecurityUtils.oAuth2AccessToken();
        ValidateRequestModel requestBody = new ValidateRequestModel();
        requestBody.setToken(accessToken);
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post(VALIDATE_ENDPOINT)
            .then().statusCode(200);
    }
}
