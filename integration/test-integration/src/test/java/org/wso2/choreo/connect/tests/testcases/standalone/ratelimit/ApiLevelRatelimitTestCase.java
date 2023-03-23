/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org).
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

package org.wso2.choreo.connect.tests.testcases.standalone.ratelimit;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.choreo.connect.tests.common.model.API;
import org.wso2.choreo.connect.tests.common.model.ApplicationDTO;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ApiLevelRatelimitTestCase {
    private String testKey;

    @BeforeClass
    public void createApiProject() throws Exception {
        API api = new API();
        api.setName("ratelimit");
        api.setContext("v2/ratelimitService");
        api.setVersion("1.0.0");
        api.setProvider("admin");

        ApplicationDTO applicationDto = new ApplicationDTO();
        applicationDto.setName("jwtApp");
        applicationDto.setTier("Unlimited");
        applicationDto.setId((int) (Math.random() * 1000));

        testKey = TokenUtil.getJWT(api, applicationDto, "Unlimited", TestConstant.KEY_TYPE_PRODUCTION,
                3600, null, true);
    }

    @Test(description = "Test rate-limiting with envoy rate-limit service")
    public void testRateLimitsWithEnvoyRateLimitService() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Internal-Key", testKey);
        Utils.delay(10000, "Could not wait till initial setup completion.");
        String endpointURL = Utils.getServiceURLHttps("/v2/ratelimitService/pet/findByStatus");
        boolean isThrottled;
        int testExecutionCount = 1;
        HttpResponse response;
        do {
             response = RateLimitUtils.sendMultipleRequests(endpointURL, headers, 3);
             isThrottled = RateLimitUtils.isThrottled(response);
             testExecutionCount++;
        } while (!isThrottled && testExecutionCount <= 3);

        Assert.assertTrue(isThrottled, "API level rate-limit testcase failed.");
        Assert.assertFalse(response.getHeaders().containsKey("x-envoy-ratelimited"),
                "x-envoy-ratelimited header should not be present in the response.");
    }
}
