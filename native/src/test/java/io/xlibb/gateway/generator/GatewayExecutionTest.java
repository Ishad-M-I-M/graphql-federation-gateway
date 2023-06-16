/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.xlibb.gateway.generator;

import io.xlibb.gateway.GatewayProject;
import io.xlibb.gateway.exception.GatewayGenerationException;
import io.xlibb.gateway.exception.ValidationException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to test gateway generated by sending requests to the gateway.
 */
public class GatewayExecutionTest {
    private static final int PORT = 9001;
    private static final String GATEWAY_URL = "http://localhost:" + PORT;
    private static final String ASTRONAUT_SUBGRAPH_URL = "http://localhost:5001";
    private static final String MISSION_SUBGRAPH_URL = "http://localhost:5002";

    private static final Path supergraphSdl =
            Paths.get("src/test/resources/supergraph_schemas/two_entities.graphql");
    private static final Path services = Paths.get("src/test/resources/sample_subgraph_services");

    private Path tmpDir;
    Process astronautServiceProcess;
    Process missionsServiceProcess;
    Process gatewayProcess;

    @BeforeClass
    public void setup() throws IOException, GatewayGenerationException, ValidationException {
        this.tmpDir = Files.createTempDirectory("graphql-gateway-" + System.nanoTime());
        File gatewayExec = GatewayCodeGenerator
                .generateGatewayJar(new GatewayProject("test", supergraphSdl.toAbsolutePath().toString(),
                        tmpDir.toAbsolutePath().toString(), 9001));
        gatewayProcess = new ProcessBuilder("java", "-jar", gatewayExec.getAbsolutePath()).start();
        astronautServiceProcess = new ProcessBuilder("java", "-jar",
                GatewayTestUtils.getBallerinaExecutableJar(
                                services.resolve("astronaut_service").toAbsolutePath(), tmpDir)
                        .getAbsolutePath()).start();
        missionsServiceProcess = new ProcessBuilder("java", "-jar",
                GatewayTestUtils.getBallerinaExecutableJar(
                                services.resolve("missions_service").toAbsolutePath(), tmpDir)
                        .getAbsolutePath()).start();

        GatewayTestUtils.waitTillUrlIsAvailable(gatewayProcess, ASTRONAUT_SUBGRAPH_URL);
        GatewayTestUtils.waitTillUrlIsAvailable(astronautServiceProcess, MISSION_SUBGRAPH_URL);
        GatewayTestUtils.waitTillUrlIsAvailable(missionsServiceProcess, GATEWAY_URL);
    }

    @AfterClass
    public void cleanup() throws IOException {
        astronautServiceProcess.destroy();
        missionsServiceProcess.destroy();
        gatewayProcess.destroy();
        GatewayTestUtils.deleteDirectory(tmpDir);
    }

    @Test(description = "Test gateway with query requests",
            dataProvider = "QueryTestDataProvider")
    public void testGatewayQueryExecution(String testName) throws IOException, ValidationException {
        String query = GatewayTestUtils.getRequestContent(testName);
        String expectedResponse = GatewayTestUtils.getResponseContent(testName);
        String response = GatewayTestUtils.getGraphqlQueryResponse(GATEWAY_URL, query);
        Assert.assertEquals(response, expectedResponse);
    }

    @DataProvider(name = "QueryTestDataProvider")
    public Object[] getQueryFileNames() {
        return new Object[]{
                "query_one_subgraph_service",
                "query_two_subgraph_services",
                "query_simple_scalar_with_parameter",
        };
    }

    @Test(description = "Test gateway with mutation requests",
            dataProvider = "MutationTestDataProvider")
    public void testGatewayMutationExecution(String testName) throws IOException, ValidationException {
        String query = GatewayTestUtils.getRequestContent(testName);
        String expectedResponse = GatewayTestUtils.getResponseContent(testName);
        String response = GatewayTestUtils.getGraphqlMutationResponse(GATEWAY_URL, query);
        Assert.assertEquals(response, expectedResponse);
    }

    @DataProvider(name = "MutationTestDataProvider")
    public Object[] getMutationFileNames() {
        return new Object[]{
                "mutation_with_query_two_subgraph_services_to_three_levels",
                "mutation_simple_string"
        };
    }

}
