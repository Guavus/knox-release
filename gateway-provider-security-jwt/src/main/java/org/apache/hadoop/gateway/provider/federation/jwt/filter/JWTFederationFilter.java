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
package org.apache.hadoop.gateway.provider.federation.jwt.filter;

import org.apache.hadoop.gateway.services.security.token.impl.JWTToken;

import javax.security.auth.Subject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class JWTFederationFilter extends AbstractJWTFilter {

  public static final String KNOX_TOKEN_AUDIENCES = "knox.token.audiences";
  private static final String BEARER = "Bearer ";
  private static JWTMessages log = MessagesFactory.get( JWTMessages.class );
  private JWTokenAuthority authority = null;
  private String paramName = "knoxtoken";

  @Override
  public void init( FilterConfig filterConfig ) throws ServletException {
      super.init(filterConfig);

    // expected audiences or null
    String expectedAudiences = filterConfig.getInitParameter(KNOX_TOKEN_AUDIENCES);
    if (expectedAudiences != null) {
      audiences = parseExpectedAudiences(expectedAudiences);
    }
  }

  public void destroy() {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
      throws IOException, ServletException {
    String header = ((HttpServletRequest) request).getHeader("Authorization");
    String wireToken = null;
    if (header != null && header.startsWith(BEARER)) {
      // what follows the bearer designator should be the JWT token being used to request or as an access token
      wireToken = header.substring(BEARER.length());
    }
    else {
      // check for query param
      wireToken = ((HttpServletRequest) request).getParameter(paramName);
    }
    
    if (wireToken != null) {
      JWTToken token = new JWTToken(wireToken);
      if (validateToken((HttpServletRequest)request, (HttpServletResponse)response, chain, token)) {
        Subject subject = createSubjectFromToken(token);
        continueWithEstablishedSecurityContext(subject, (HttpServletRequest)request, (HttpServletResponse)response, chain);
      }
    }
    else {
      // no token provided in header
      ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return; //break filter chain
    }
  }

  protected void handleValidationError(HttpServletRequest request, HttpServletResponse response, int status,
                                       String error) throws IOException {
    if (error != null) {
      response.sendError(status, error);   
    }
    else {
      response.sendError(status);
    }
  }
}
