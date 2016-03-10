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
package org.apache.hadoop.gateway;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.hadoop.gateway.security.ldap.SimpleLdapDirectoryServer;
import org.apache.hadoop.gateway.services.DefaultGatewayServices;
import org.apache.hadoop.gateway.services.GatewayServices;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.services.topology.TopologyService;
import org.apache.hadoop.test.TestUtils;
import org.apache.hadoop.test.category.ReleaseTest;
import org.apache.hadoop.test.mock.MockServer;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Appender;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.hadoop.test.TestUtils.LOG_ENTER;
import static org.apache.hadoop.test.TestUtils.LOG_EXIT;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.xmlmatchers.XmlMatchers.hasXPath;
import static org.xmlmatchers.transform.XmlConverters.the;

@Category(ReleaseTest.class)
public class GatewayMultiFuncTest {

  private static Logger LOG = LoggerFactory.getLogger( GatewayMultiFuncTest.class );
  private static Class DAT = GatewayMultiFuncTest.class;

  private static Enumeration<Appender> appenders;
  private static GatewayTestConfig config;
  private static DefaultGatewayServices services;
  private static GatewayServer gateway;
  private static int gatewayPort;
  private static String gatewayUrl;
  private static SimpleLdapDirectoryServer ldap;
  private static TcpTransport ldapTransport;
  private static Properties params;
  private static TopologyService topos;

  @BeforeClass
  public static void setupSuite() throws Exception {
    LOG_ENTER();
    //appenders = NoOpAppender.setUp();
    setupLdap();
    setupGateway();
    LOG_EXIT();
  }

  @AfterClass
  public static void cleanupSuite() throws Exception {
    LOG_ENTER();
    gateway.stop();
    ldap.stop( true );
    FileUtils.deleteQuietly( new File( config.getGatewayHomeDir() ) );
    //NoOpAppender.tearDown( appenders );
    LOG_EXIT();
  }

  public static void setupLdap() throws Exception {
    URL usersUrl = TestUtils.getResourceUrl( DAT, "users.ldif" );
    ldapTransport = new TcpTransport( 0 );
    ldap = new SimpleLdapDirectoryServer( "dc=hadoop,dc=apache,dc=org", new File( usersUrl.toURI() ), ldapTransport );
    ldap.start();
    LOG.info( "LDAP port = " + ldapTransport.getAcceptor().getLocalAddress().getPort() );
  }

  public static void setupGateway() throws Exception {

    File targetDir = new File( System.getProperty( "user.dir" ), "target" );
    File gatewayDir = new File( targetDir, "gateway-home-" + UUID.randomUUID() );
    gatewayDir.mkdirs();

    config = new GatewayTestConfig();
    config.setGatewayHomeDir( gatewayDir.getAbsolutePath() );

    URL svcsFileUrl = TestUtils.getResourceUrl( DAT, "services/readme.txt" );
    File svcsFile = new File( svcsFileUrl.getFile() );
    File svcsDir = svcsFile.getParentFile();
    config.setGatewayServicesDir( svcsDir.getAbsolutePath() );

    URL appsFileUrl = TestUtils.getResourceUrl( DAT, "applications/readme.txt" );
    File appsFile = new File( appsFileUrl.getFile() );
    File appsDir = appsFile.getParentFile();
    config.setGatewayApplicationsDir( appsDir.getAbsolutePath() );

    File topoDir = new File( config.getGatewayTopologyDir() );
    topoDir.mkdirs();

    File deployDir = new File( config.getGatewayDeploymentDir() );
    deployDir.mkdirs();

    startGatewayServer();
  }

  public static void startGatewayServer() throws Exception {
    services = new DefaultGatewayServices();
    Map<String,String> options = new HashMap<String,String>();
    options.put( "persist-master", "false" );
    options.put( "master", "password" );
    try {
      services.init( config, options );
    } catch ( ServiceLifecycleException e ) {
      e.printStackTrace(); // I18N not required.
    }
    topos = services.getService(GatewayServices.TOPOLOGY_SERVICE);

    gateway = GatewayServer.startGateway( config, services );
    MatcherAssert.assertThat( "Failed to start gateway.", gateway, notNullValue() );

    gatewayPort = gateway.getAddresses()[0].getPort();
    gatewayUrl = "http://localhost:" + gatewayPort + "/" + config.getGatewayPath();

    LOG.info( "Gateway port = " + gateway.getAddresses()[ 0 ].getPort() );

    params = new Properties();
    params.put( "LDAP_URL", "ldap://localhost:" + ldapTransport.getAcceptor().getLocalAddress().getPort() );
  }

  @Test( timeout = TestUtils.MEDIUM_TIMEOUT )
  public void testDefaultJsonMimeTypeHandlingKnox678() throws Exception {
    LOG_ENTER();

    MockServer mock = new MockServer( "REPEAT", true );

    params = new Properties();
    params.put( "LDAP_URL", "ldap://localhost:" + ldapTransport.getAcceptor().getLocalAddress().getPort() );
    params.put( "MOCK_SERVER_PORT", mock.getPort() );

    String topoStr = TestUtils.merge( DAT, "topologies/test-knox678-utf8-chars-topology.xml", params );
    File topoFile = new File( config.getGatewayTopologyDir(), "knox678.xml" );
    FileUtils.writeStringToFile( topoFile, topoStr );

    topos.reloadTopologies();

    String uname = "guest";
    String pword = uname + "-password";

    mock.expect().method( "GET" )
        .respond().contentType( "application/json" ).contentLength( -1 ).content( "{\"msg\":\"H\u00eallo\"}", Charset.forName( "UTF-8" ) );
    String json = given()
        //.log().all()
        .auth().preemptive().basic( uname, pword )
        .expect()
        //.log().all()
        .statusCode( HttpStatus.SC_OK )
        .contentType( "application/json; charset=UTF-8" )
        .when().log().ifError().get( gatewayUrl + "/knox678/repeat" ).andReturn().asString();
    assertThat( json, is("{\"msg\":\"H\u00eallo\"}") );
    assertThat( mock.isEmpty(), is(true) );

    mock.expect().method( "GET" )
        .respond().contentType( "application/octet-stream" ).contentLength( -1 ).content( "H\u00eallo".getBytes() );
    byte[] bytes = given()
        //.log().all()
        .auth().preemptive().basic( uname, pword )
        .expect()
        //.log().all()
        .statusCode( HttpStatus.SC_OK )
        .contentType( "application/octet-stream" )
        .when().log().ifError().get( gatewayUrl + "/knox678/repeat" ).andReturn().asByteArray();
    assertThat( bytes, is(equalTo("H\u00eallo".getBytes())) );
    assertThat( mock.isEmpty(), is(true) );

    mock.stop();

    LOG_EXIT();
  }

  @Test( timeout = TestUtils.MEDIUM_TIMEOUT )
  public void testPostWithContentTypeKnox681() throws Exception {
    LOG_ENTER();

    MockServer mock = new MockServer( "REPEAT", true );

    params = new Properties();
    params.put( "MOCK_SERVER_PORT", mock.getPort() );
    params.put( "LDAP_URL", "ldap://localhost:" + ldapTransport.getAcceptor().getLocalAddress().getPort() );

    String topoStr = TestUtils.merge( DAT, "topologies/test-knox678-utf8-chars-topology.xml", params );
    File topoFile = new File( config.getGatewayTopologyDir(), "knox681.xml" );
    FileUtils.writeStringToFile( topoFile, topoStr );

    topos.reloadTopologies();

    mock
        .expect()
        .method( "PUT" )
        .pathInfo( "/repeat-context/" )
        .respond()
        .status( HttpStatus.SC_CREATED )
        .content( "{\"name\":\"value\"}".getBytes() )
        .contentLength( -1 )
        .contentType( "application/json; charset=UTF-8" )
        .header( "Location", gatewayUrl + "/knox681/repeat" );

    String uname = "guest";
    String pword = uname + "-password";

    HttpHost targetHost = new HttpHost( "localhost", gatewayPort, "http" );
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope( targetHost.getHostName(), targetHost.getPort() ),
        new UsernamePasswordCredentials( uname, pword ) );

    AuthCache authCache = new BasicAuthCache();
    BasicScheme basicAuth = new BasicScheme();
    authCache.put( targetHost, basicAuth );

    HttpClientContext context = HttpClientContext.create();
    context.setCredentialsProvider( credsProvider );
    context.setAuthCache( authCache );

    CloseableHttpClient client = HttpClients.createDefault();
    HttpPut request = new HttpPut( gatewayUrl + "/knox681/repeat" );
    request.addHeader( "X-XSRF-Header", "jksdhfkhdsf" );
    request.addHeader( "Content-Type", "application/json" );
    CloseableHttpResponse response = client.execute( request, context );
    assertThat( response.getStatusLine().getStatusCode(), is( HttpStatus.SC_CREATED ) );
    assertThat( response.getFirstHeader( "Location" ).getValue(), endsWith("/gateway/knox681/repeat" ) );
    assertThat( response.getFirstHeader( "Content-Type" ).getValue(), is("application/json; charset=UTF-8") );
    String body = new String( IOUtils.toByteArray( response.getEntity().getContent() ), Charset.forName( "UTF-8" ) );
    assertThat( body, is( "{\"name\":\"value\"}" ) );
    response.close();
    client.close();

    mock
        .expect()
        .method( "PUT" )
        .pathInfo( "/repeat-context/" )
        .respond()
        .status( HttpStatus.SC_CREATED )
        .content( "<test-xml/>".getBytes() )
        .contentType( "application/xml; charset=UTF-8" )
        .header( "Location", gatewayUrl + "/knox681/repeat" );

    client = HttpClients.createDefault();
    request = new HttpPut( gatewayUrl + "/knox681/repeat" );
    request.addHeader( "X-XSRF-Header", "jksdhfkhdsf" );
    request.addHeader( "Content-Type", "application/xml" );
    response = client.execute( request, context );
    assertThat( response.getStatusLine().getStatusCode(), is( HttpStatus.SC_CREATED ) );
    assertThat( response.getFirstHeader( "Location" ).getValue(), endsWith("/gateway/knox681/repeat" ) );
    assertThat( response.getFirstHeader( "Content-Type" ).getValue(), is("application/xml; charset=UTF-8") );
    body = new String( IOUtils.toByteArray( response.getEntity().getContent() ), Charset.forName( "UTF-8" ) );
    assertThat( the(body), hasXPath( "/test-xml" ) );
    response.close();
    client.close();

    mock.stop();

    LOG_EXIT();
  }

}


