/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2am.micro.gw.tests.jwtValidator;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.wso2am.micro.gw.tests.IntegrationTest;
import org.wso2am.micro.gw.tests.common.BaseTestCase;
import org.wso2am.micro.gw.tests.common.model.API;
import org.wso2am.micro.gw.tests.common.model.ApplicationDTO;
import org.wso2am.micro.gw.tests.util.*;
import org.junit.platform.runner.JUnitPlatform;

import java.util.HashMap;
import java.util.Map;


@Category(IntegrationTest.class)
public class JwtTestCase extends BaseTestCase {

    protected String jwtTokenProd;

    @Before
    public void start() throws Exception {
        super.startMGW();

        //deploy the api

        //todo create a zip file
        String apiZipfile = getClass().getClassLoader()
                .getResource("apis/petstore.zip").getPath();
        String certificatesTrustStorePath = getClass().getClassLoader()
                .getResource("keystore/cacerts").getPath();
        super.deployAPI(apiZipfile,certificatesTrustStorePath);

        //generate JWT token from APIM
        API api = new API();
        api.setName("PetStoreAPI");
        api.setContext("petstore/v1");
        api.setProdEndpoint(getMockServiceURLHttp("/echo/prod"));
        api.setVersion("1.0.0");
        api.setProvider("admin");

        //Define application info
        ApplicationDTO application = new ApplicationDTO();
        application.setName("jwtApp");
        application.setTier("Unlimited");
        application.setId((int) (Math.random() * 1000));

        jwtTokenProd = getJWT(api, application, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION, 3600);
        System.out.println(jwtTokenProd );
    }


    /*@After
    public void stop() {
        super.stopMGW();
    } */


    @Test
    @DisplayName("Test to check the JWT auth working")
    public void invokeJWTHeaderSuccessTest() throws Exception{

        String certificatesTrustStorePath = getClass().getClassLoader()
                .getResource("keystore/cacerts").getPath();

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/3") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched", HttpStatus.SC_OK, response.getResponseCode());
    }

    @Test
    @DisplayName("Test to check the JWT auth validate invalida signature token")
    public void invokeJWTHeaderInvalidTokenTest() throws Exception{

        String certificatesTrustStorePath = getClass().getClassLoader()
                .getResource("keystore/cacerts").getPath();

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.INVALID_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/3") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched",TestConstant.INVALID_CREDENTIALS_CODE,
                response.getResponseCode());
    }

    @Test
    @DisplayName("Test to check the JWT auth validate expired token")
    public void invokeJWTHeaderExpiredTokenTest() throws Exception{

        String certificatesTrustStorePath = getClass().getClassLoader()
                .getResource("keystore/cacerts").getPath();

        // Set header
        Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + TestConstant.EXPIRED_JWT_TOKEN);
        HttpResponse response = HttpsClientRequest.doGet(getServiceURLHttps(
                "/v2/pet/3") , headers, certificatesTrustStorePath);

        Assert.assertNotNull(response);
        Assert.assertEquals("Response code mismatched", TestConstant.INVALID_CREDENTIALS_CODE,
                response.getResponseCode());
    }

}
