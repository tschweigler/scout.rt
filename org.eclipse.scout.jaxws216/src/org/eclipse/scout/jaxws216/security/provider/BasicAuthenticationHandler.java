/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Daniel Wiehl (BSI Business Systems Integration AG) - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.jaxws216.security.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.eclipse.scout.commons.Base64Utility;
import org.eclipse.scout.commons.BooleanUtility;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.jaxws216.annotation.ScoutTransaction;
import org.eclipse.scout.jaxws216.security.Authenticator;
import org.eclipse.scout.service.ServiceUtility;

import com.sun.xml.internal.ws.client.BindingProviderProperties;

/**
 * <p>
 * Handler to protect your webservice with Basic Access Authentication. This requires requests to provide a valid user
 * name and password to access content. User's credentials are transported in HTTP headers. Basic authentication also
 * works across firewalls and proxy servers.
 * </p>
 * <p>
 * However, the disadvantage of Basic authentication is that it transmits unencrypted base64-encoded passwords across
 * the network. Therefore, you only should use this authentication when you know that the connection between the client
 * and the server is secure. The connection should be established either over a dedicated line or by using Secure
 * Sockets Layer (SSL) encryption and Transport Layer Security (TLS).
 * </p>
 */
@ScoutTransaction
public class BasicAuthenticationHandler implements IAuthenticationHandler {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BasicAuthenticationHandler.class);

  private ICredentialValidationStrategy m_credentialValidationStrategy;

  public BasicAuthenticationHandler() {
    ServiceUtility.injectConfigProperties(this);
  }

  @Override
  public final boolean handleMessage(SOAPMessageContext context) {
    boolean outbound = TypeCastUtility.castValue(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY), boolean.class);
    if (outbound) {
      return true; // only inbound messages are of interest
    }

    if (Authenticator.isSubjectAuthenticated()) {
      return true;
    }

    if (!isAuthHeaderIncluded(context)) {
      installAuthHeader(context);
      return breakHandlerChain(context);
    }

    List<String> authHeaders = getHttpRequestHeaders(context).get("Authorization");
    for (String authHeader : authHeaders) {
      if (authHeader.startsWith("Basic ")) {
        try {
          if (authenticateRequest(authHeader)) {
            return true;
          }
          return breakHandlerChain(context);
        }
        catch (Exception e) {
          return breakHandlerChainWithException(context, e);
        }
      }
    }
    return breakHandlerChain(context);
  }

  @Override
  public void injectCredentialValidationStrategy(ICredentialValidationStrategy strategy) {
    m_credentialValidationStrategy = strategy;
  }

  @Override
  public final Set<QName> getHeaders() {
    return new HashSet<QName>();
  }

  @Override
  public final void close(MessageContext context) {
  }

  @Override
  public final boolean handleFault(SOAPMessageContext context) {
    return false;
  }

  protected boolean authenticateRequest(String authHeader) throws Exception {
    String[] credentials = new String(Base64Utility.decode(authHeader.substring("Basic ".length())), "ISO-8859-1").split(":", 2);
    String username = credentials[0];
    String passwordPlainText = credentials[1];

    return Authenticator.authenticateRequest(m_credentialValidationStrategy, username, passwordPlainText);
  }

  protected void installAuthHeader(SOAPMessageContext context) {
    Map<String, List<String>> httpResponseHeaders = getHttpResponseHeaders(context);
    if (httpResponseHeaders == null) {
      httpResponseHeaders = new HashMap<String, List<String>>();
    }

    List<String> basicAuthToken = new LinkedList<String>();
    basicAuthToken.add("Basic realm=\"" + getRealm() + "\"");
    httpResponseHeaders.put("WWW-Authenticate", basicAuthToken);

    context.put(MessageContext.HTTP_RESPONSE_HEADERS, httpResponseHeaders);
    context.put(MessageContext.HTTP_RESPONSE_CODE, 401);
  }

  @SuppressWarnings("unchecked")
  protected Map<String, List<String>> getHttpRequestHeaders(SOAPMessageContext context) {
    return (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
  }

  @SuppressWarnings("unchecked")
  protected Map<String, List<String>> getHttpResponseHeaders(SOAPMessageContext context) {
    return (Map<String, List<String>>) context.get(MessageContext.HTTP_RESPONSE_HEADERS);
  }

  protected boolean breakHandlerChain(SOAPMessageContext context) {
    context.put(MessageContext.HTTP_RESPONSE_CODE, 401);

    boolean oneway = BooleanUtility.nvl((Boolean) context.get(BindingProviderProperties.ONE_WAY_OPERATION), false);
    if (oneway) {
      // do not just return false as in one-way communication, the chain is continued regardless of the status.
      throw new WebServiceException("Unauthorized");
    }
    return false;
  }

  protected boolean breakHandlerChainWithException(SOAPMessageContext context, Exception exception) {
    context.put(MessageContext.HTTP_RESPONSE_CODE, 500);
    LOG.error("Internal server error  (Basic Access Authentication)", exception);

    if (exception instanceof WebServiceException) {
      throw (WebServiceException) exception;
    }
    throw new WebServiceException("Internal server error");
  }

  protected String getRealm() {
    return "Secure Area";
  }

  protected boolean isAuthHeaderIncluded(SOAPMessageContext context) {
    Map<String, List<String>> httpRequestHeaders = getHttpRequestHeaders(context);
    return (httpRequestHeaders != null && httpRequestHeaders.get("Authorization") != null && httpRequestHeaders.get("Authorization").size() > 0);
  }

  @Override
  public String getName() {
    return "BASIC";
  }
}