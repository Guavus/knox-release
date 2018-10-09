/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.deploy.impl;

import org.apache.hadoop.gateway.config.GatewayConfig;
import org.apache.hadoop.gateway.deploy.DeploymentContext;
import org.apache.hadoop.gateway.deploy.ProviderDeploymentContributor;
import org.apache.hadoop.gateway.descriptor.FilterDescriptor;
import org.apache.hadoop.gateway.descriptor.FilterParamDescriptor;
import org.apache.hadoop.gateway.descriptor.ResourceDescriptor;
import org.apache.hadoop.gateway.descriptor.impl.GatewayDescriptorImpl;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteRulesDescriptor;
import org.apache.hadoop.gateway.service.definition.CustomDispatch;
import org.apache.hadoop.gateway.service.definition.Rewrite;
import org.apache.hadoop.gateway.service.definition.Route;
import org.apache.hadoop.gateway.service.definition.ServiceDefinition;
import org.apache.hadoop.gateway.topology.Provider;
import org.apache.hadoop.gateway.topology.Service;
import org.apache.hadoop.gateway.topology.Topology;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;



public class ServiceDefinitionDeploymentContributorTest {

  @Test
  public void testServiceLoader() throws Exception {
    ServiceLoader loader = ServiceLoader.load( ProviderDeploymentContributor.class );
    Iterator iterator = loader.iterator();
    assertThat( "Service iterator empty.", iterator.hasNext() );
    while( iterator.hasNext() ) {
      Object object = iterator.next();
      if( object instanceof ServiceDefinitionDeploymentContributor ) {
        fail("The ServiceDefinition deployment contributor is not meant to be loaded using the service loader mechanism");
      }
    }
  }

  /**
   * Test that service param useTwoWaySsl in topologies overrides the corresponding custom dispatch property.
   */
  @Test
  public void testServiceAttributeUseTwoWaySSLParamOverride() throws Exception {

    final String TEST_SERVICE_ROLE     = "Test";
    final String USE_TWO_WAY_SSL_PARAM = "useTwoWaySsl";

    UrlRewriteRulesDescriptor clusterRules = EasyMock.createNiceMock(UrlRewriteRulesDescriptor.class);
    EasyMock.replay(clusterRules);

    UrlRewriteRulesDescriptor svcRules = EasyMock.createNiceMock(UrlRewriteRulesDescriptor.class);
    EasyMock.replay(svcRules);

    ServiceDefinition svcDef = EasyMock.createNiceMock(ServiceDefinition.class);
    EasyMock.expect(svcDef.getRole()).andReturn(TEST_SERVICE_ROLE).anyTimes();
    List<Route> svcRoutes = new ArrayList<>();
    Route route = EasyMock.createNiceMock(Route.class);
    List<Rewrite> filters = new ArrayList<>();
    EasyMock.expect(route.getRewrites()).andReturn(filters).anyTimes();
    svcRoutes.add(route);
    EasyMock.replay(route);
    EasyMock.expect(svcDef.getRoutes()).andReturn(svcRoutes).anyTimes();
    CustomDispatch cd = EasyMock.createNiceMock(CustomDispatch.class);
    EasyMock.expect(cd.getClassName()).andReturn("TestDispatch").anyTimes();
    EasyMock.expect(cd.getHaClassName()).andReturn("TestHADispatch").anyTimes();
    EasyMock.expect(cd.getHaContributorName()).andReturn(null).anyTimes();

    // Let useTwoWaySsl be FALSE by default
    EasyMock.expect(cd.getUseTwoWaySsl()).andReturn(false).anyTimes();

    EasyMock.replay(cd);
    EasyMock.expect(svcDef.getDispatch()).andReturn(cd).anyTimes();
    EasyMock.replay(svcDef);

    ServiceDefinitionDeploymentContributor sddc = new ServiceDefinitionDeploymentContributor(svcDef, svcRules);

    DeploymentContext context = EasyMock.createNiceMock(DeploymentContext.class);
    EasyMock.expect(context.getDescriptor("rewrite")).andReturn(clusterRules).anyTimes();
    GatewayConfig gc = EasyMock.createNiceMock(GatewayConfig.class);
    EasyMock.expect(gc.isXForwardedEnabled()).andReturn(false).anyTimes();
    EasyMock.expect(gc.isCookieScopingToPathEnabled()).andReturn(false).anyTimes();
    EasyMock.replay(gc);
    EasyMock.expect(context.getGatewayConfig()).andReturn(gc).anyTimes();

    // Configure the HaProvider
    Topology topology = EasyMock.createNiceMock(Topology.class);
    List<Provider> providers = new ArrayList<>();
    Provider haProvider = EasyMock.createNiceMock(Provider.class);
    EasyMock.expect(haProvider.getRole()).andReturn("ha").anyTimes();
    EasyMock.expect(haProvider.isEnabled()).andReturn(true).anyTimes();
    Map<String, String> providerParams = new HashMap<>();
    providerParams.put(TEST_SERVICE_ROLE, "whatever");
    EasyMock.expect(haProvider.getParams()).andReturn(providerParams).anyTimes();

    EasyMock.replay(haProvider);
    providers.add(haProvider);
    EasyMock.expect(topology.getProviders()).andReturn(providers).anyTimes();
    EasyMock.replay(topology);
    EasyMock.expect(context.getTopology()).andReturn(topology).anyTimes();

    TestGatewayDescriptor gd = new TestGatewayDescriptor();
    EasyMock.expect(context.getGatewayDescriptor()).andReturn(gd).anyTimes();
    EasyMock.replay(context);

    // Configure the service with the useTwoWaySsl param to OVERRIDE the value in the service definition
    Service service = EasyMock.createNiceMock(Service.class);
    Map<String, String> svcParams = new HashMap<>();
    svcParams.put(USE_TWO_WAY_SSL_PARAM, "true");
    EasyMock.expect(service.getParams()).andReturn(svcParams).anyTimes();
    EasyMock.replay(service);

    sddc.contributeService(context, service);

    List<ResourceDescriptor> resources = gd.resources();
    assertEquals(1, gd.resources().size());
    ResourceDescriptor res = gd.resources().get(0);
    assertNotNull(res);
    List<FilterDescriptor> filterList = res.filters();
    assertEquals(1, filterList.size());
    FilterDescriptor f = filterList.get(0);
    assertNotNull(f);
    assertEquals("dispatch", f.role());
    List<FilterParamDescriptor> fParams = f.params();
    assertNotNull(fParams);

    // Collect the values of filter params named useTwoWaySsl
    List<String> useTwoWaySslFilterParamValues = new ArrayList<>();
    for (FilterParamDescriptor param : fParams) {
      if (param.name().equals(USE_TWO_WAY_SSL_PARAM)) {
        useTwoWaySslFilterParamValues.add(param.value());
      }
    }

    assertEquals("Expected only a single filter param named " + USE_TWO_WAY_SSL_PARAM,
                 1, useTwoWaySslFilterParamValues.size());
    assertEquals("Expected the service param to override the service definition value for " + USE_TWO_WAY_SSL_PARAM,
                 "true", useTwoWaySslFilterParamValues.get(0));
  }

  private static class TestGatewayDescriptor extends GatewayDescriptorImpl {
  }

}