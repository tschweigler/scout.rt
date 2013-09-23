/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.security.AccessController;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.osgi.BundleInspector;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.http.servletfilter.HttpServletEx;
import org.eclipse.scout.http.servletfilter.SessionHandler;
import org.eclipse.scout.http.servletfilter.helper.HttpAuthJaasFilter;
import org.eclipse.scout.rt.server.admin.html.AdminSession;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.server.services.common.session.IServerSessionRegistryService;
import org.eclipse.scout.rt.shared.servicetunnel.DefaultServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.IServiceTunnelContentHandler;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelRequest;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelResponse;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.service.SERVICES;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

/**
 * Use this servlet to dispatch scout gui service requests using {@link ServiceTunnelRequest},
 * {@link ServiceTunnelResponse} and any {@link IServiceTunnelContentHandler} implementation.
 * <p>
 * Override the methods
 * {@link DefaultTransactionDelegate#validateInput(org.eclipse.scout.rt.shared.validate.IValidationStrategy, Object, java.lang.reflect.Method, Object[])
 * DefaultTransactionDelegate#validateInput} and
 * {@link DefaultTransactionDelegate#validateOutput(org.eclipse.scout.rt.shared.validate.IValidationStrategy, Object, java.lang.reflect.Method, Object, Object[])
 * DefaultTransactionDelegate#validateOutput} to do central input/output validation.
 * <p>
 * By default there is a jaas convenience filter {@link HttpAuthJaasFilter} on /process and a {@link SoapWsseJaasFilter}
 * on /ajax with priority 1000
 * <p>
 * When using RAP (rich ajax platform) as the ui web app then there must be a {@link WebSessionIdPrincipal} in the
 * subject, in order to map those requests to virtual sessions instead of (the unique) http session.
 */
public class ServiceTunnelServlet extends HttpServletEx {
  public static final String HTTP_DEBUG_PARAM = "org.eclipse.scout.rt.server.http.debug";
  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ServiceTunnelServlet.class);

  private transient IServiceTunnelContentHandler m_contentHandler;
  private transient Bundle[] m_orderedBundleList;
  private Object m_orderedBundleListLock = new Boolean(true);
  private VirtualSessionCache m_ajaxSessionCache = new VirtualSessionCache();
  private Object m_msgEncoderLock = new Boolean(true);
  private Class<? extends IServerSession> m_serverSessionClass;
  private Version m_requestMinVersion;
  private boolean m_debug;

  public ServiceTunnelServlet() {
    String text = Activator.getDefault().getBundle().getBundleContext().getProperty(HTTP_DEBUG_PARAM);
    if (text != null && text.equalsIgnoreCase("true")) {
      m_debug = true;
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    m_requestMinVersion = initRequestMinVersion(config);
  }

  @SuppressWarnings("unchecked")
  protected void lazyInit(HttpServletRequest req, HttpServletResponse res) throws ServletException {
    if (m_serverSessionClass == null) {
      String qname = getServletConfig().getInitParameter("session");
      if (qname != null) {
        int i = qname.lastIndexOf('.');
        try {
          m_serverSessionClass = (Class<? extends IServerSession>) Platform.getBundle(qname.substring(0, i)).loadClass(qname);
        }
        catch (ClassNotFoundException e) {
          throw new ServletException("Loading class " + qname, e);
        }
      }
    }
    if (m_serverSessionClass == null) {
      // find bundle that defines this servlet
      try {
        Bundle bundle = findServletContributor(req.getServletPath());
        if (bundle != null) {
          m_serverSessionClass = (Class<? extends IServerSession>) bundle.loadClass(bundle.getSymbolicName() + ".ServerSession");
        }
      }
      catch (Throwable t) {
        // nop
      }
    }
    if (m_serverSessionClass == null) {
      throw new ServletException("Expected init-param \"session\"");
    }
  }

  /**
   * <p>
   * Reads the minimum version a request must have.
   * </p>
   * <p>
   * The version has to be defined as init parameter in the servlet configuration. <br/>
   * This can be done by adding a new init-param at the {@link DefaultHttpProxyHandlerServlet} on the extension point
   * org.eclipse.equinox.http.registry.servlets and setting its name to min-version and its value to the desired version
   * (like 1.2.3).
   * </p>
   * <p>
   * If there is no min-version defined it uses the Bundle-Version of the bundle which contains the running product.
   * </p>
   * 
   * @param config
   * @return
   */
  protected Version initRequestMinVersion(ServletConfig config) {
    Version version = null;
    String v = config.getInitParameter("min-version");
    if (v != null) {
      Version tmp = Version.parseVersion(v);
      version = new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
    }
    else if (Platform.getProduct() != null) {
      v = (String) Platform.getProduct().getDefiningBundle().getHeaders().get("Bundle-Version");
      Version tmp = Version.parseVersion(v);
      version = new Version(tmp.getMajor(), tmp.getMinor(), tmp.getMicro());
    }

    return version;
  }

  /**
   * @deprecated use {@link #createContentHandler(Class)}. Will be removed in Release 3.10.
   */
  @Deprecated
  protected IServiceTunnelContentHandler createMessageEncoder(Class<? extends IServerSession> sessionClass) {
    return createContentHandler(sessionClass);
  }

  /**
   * create the (reusable) content handler (soap, xml, binary) for marshalling scout/osgi remote service calls
   * <p>
   * This method is part of the protected api and can be overridden.
   */
  protected IServiceTunnelContentHandler createContentHandler(Class<? extends IServerSession> sessionClass) {
    DefaultServiceTunnelContentHandler e = new DefaultServiceTunnelContentHandler();
    e.initialize(getOrderedBundleList(), sessionClass.getClassLoader());
    return e;
  }

  private IServiceTunnelContentHandler getServiceTunnelContentHandler() {
    synchronized (m_msgEncoderLock) {
      if (m_contentHandler == null) {
        m_contentHandler = createMessageEncoder(m_serverSessionClass);
      }
    }
    return m_contentHandler;
  }

  private Bundle[] getOrderedBundleList() {
    synchronized (m_orderedBundleListLock) {
      if (m_orderedBundleList == null) {
        String[] bundleOrderPrefixes = SerializationUtility.getBundleOrderPrefixes();
        m_orderedBundleList = BundleInspector.getOrderedBundleList(bundleOrderPrefixes);
      }
    }
    return m_orderedBundleList;
  }

  private IServerSession lookupScoutServerSessionOnHttpSession(HttpServletRequest req, HttpServletResponse res, Subject subject, UserAgent userAgent) throws ProcessingException, ServletException {
    //external request: apply locking, this is the session initialization phase
    synchronized (req.getSession()) {
      IServerSession serverSession = (IServerSession) SessionHandler.getInstance().getAttribute(req, IServerSession.class.getName());
      if (serverSession == null) {
        serverSession = SERVICES.getService(IServerSessionRegistryService.class).newServerSession(m_serverSessionClass, subject, userAgent);
        SessionHandler.getInstance().setAttribute(req, IServerSession.class.getName(), serverSession);
      }
      return serverSession;
    }
  }

  private IServerSession lookupScoutServerSessionOnVirtualSession(HttpServletRequest req, HttpServletResponse res, String ajaxSessionId, Subject subject, UserAgent userAgent) throws ProcessingException, ServletException {
    synchronized (m_ajaxSessionCache) {
      //update session timeout
      int maxInactive = req.getSession().getMaxInactiveInterval();
      if (maxInactive < 0) {
        maxInactive = 3600;
      }
      m_ajaxSessionCache.setSessionTimeoutMillis(Math.max(1000L, 1000L * maxInactive));
      IServerSession serverSession = m_ajaxSessionCache.get(ajaxSessionId);
      if (serverSession == null) {
        serverSession = SERVICES.getService(IServerSessionRegistryService.class).newServerSession(m_serverSessionClass, subject, userAgent);
        m_ajaxSessionCache.put(ajaxSessionId, serverSession);
      }
      else {
        m_ajaxSessionCache.touch(ajaxSessionId);
      }
      return serverSession;
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
    Subject subject = Subject.getSubject(AccessController.getContext());
    if (subject == null) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    //invoke
    Map<Class, Object> backup = ThreadContext.backup();
    try {
      lazyInit(req, res);
      //
      //legacy, deprecated, do not use servlet request/response in scout code
      ThreadContext.putHttpServletRequest(req);
      ThreadContext.putHttpServletResponse(res);
      //
      UserAgent userAgent = UserAgent.createDefault();
      IServerSession serverSession = lookupScoutServerSessionOnHttpSession(req, res, subject, userAgent);
      //
      ServerJob job = new AdminServiceJob(serverSession, subject, req, res);
      job.runNow(new NullProgressMonitor());
      job.throwOnError();
    }
    catch (ProcessingException e) {
      throw new ServletException(e);
    }
    finally {
      ThreadContext.restore(backup);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    Subject subject = Subject.getSubject(AccessController.getContext());
    if (subject == null) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    try {
      lazyInit(req, res);
      Map<Class, Object> backup = ThreadContext.backup();
      Locale oldLocale = LocaleThreadLocal.get();
      try {
        ThreadContext.putHttpServletRequest(req);
        ThreadContext.putHttpServletResponse(res);
        //read request
        ServiceTunnelRequest serviceRequest = deserializeInput(req.getInputStream());
        LocaleThreadLocal.set(serviceRequest.getLocale());
        //virtual or http session?
        IServerSession serverSession;
        String virtualSessionId = serviceRequest.getVirtualSessionId();
        UserAgent userAgent = UserAgent.createByIdentifier(serviceRequest.getUserAgent());
        if (virtualSessionId != null) {
          serverSession = lookupScoutServerSessionOnVirtualSession(req, res, virtualSessionId, subject, userAgent);
        }
        else {
          serverSession = lookupScoutServerSessionOnHttpSession(req, res, subject, userAgent);
        }
        //invoke
        AtomicReference<ServiceTunnelResponse> serviceResponseHolder = new AtomicReference<ServiceTunnelResponse>();
        ServerJob job = createServiceTunnelServerJob(serverSession, serviceRequest, serviceResponseHolder, subject);
        job.setTransactionSequence(serviceRequest.getRequestSequence());
        job.runNow(new NullProgressMonitor());
        job.throwOnError();
        serializeOutput(res, serviceResponseHolder.get());
      }
      finally {
        ThreadContext.restore(backup);
        LocaleThreadLocal.set(oldLocale);
      }
    }
    catch (Throwable t) {
      //ignore disconnect errors
      // we don't want to throw an exception, if the client closed the connection
      Throwable cause = t;
      while (cause != null) {
        if (cause instanceof SocketException) {
          return;
        }
        else if (cause.getClass().getSimpleName().equalsIgnoreCase("EofException")) {
          return;
        }
        else if (cause instanceof InterruptedIOException) {
          return;
        }
        // next
        cause = cause.getCause();
      }
      LOG.error("Session=" + req.getSession().getId() + ", Client=" + req.getRemoteUser() + "@" + req.getRemoteAddr() + "/" + req.getRemoteHost(), t);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  protected ServiceTunnelRequest deserializeInput(InputStream in) throws Exception {
    ServiceTunnelRequest req = getServiceTunnelContentHandler().readRequest(in);
    return req;
  }

  protected void serializeOutput(HttpServletResponse httpResponse, ServiceTunnelResponse res) throws Exception {
    // security: do not send back error stack trace
    if (res.getException() != null) {
      res.getException().setStackTrace(new StackTraceElement[0]);
    }
    //
    httpResponse.setDateHeader("Expires", -1);
    httpResponse.setHeader("Cache-Control", "no-cache");
    httpResponse.setHeader("pragma", "no-cache");
    httpResponse.setContentType("text/xml");
    getServiceTunnelContentHandler().writeResponse(httpResponse.getOutputStream(), res);
  }

  private Bundle findServletContributor(String alias) throws CoreException {
    BundleContext context = Activator.getDefault().getBundle().getBundleContext();
    ServiceReference ref = context.getServiceReference(IExtensionRegistry.class.getName());
    Bundle bundle = null;
    if (ref != null) {
      @SuppressWarnings("unchecked")
      IExtensionRegistry reg = (IExtensionRegistry) context.getService(ref);
      if (reg != null) {
        IExtensionPoint xpServlet = reg.getExtensionPoint("org.eclipse.equinox.http.registry.servlets");
        if (xpServlet != null) {
          for (IExtension xServlet : xpServlet.getExtensions()) {
            for (IConfigurationElement cServlet : xServlet.getConfigurationElements()) {
              if (cServlet.getName().equals("servlet")) {
                if (this.getClass().getName().equals(cServlet.getAttribute("class"))) {
                  // half match, go on looping
                  bundle = Platform.getBundle(xServlet.getContributor().getName());
                  if (alias.equals(cServlet.getAttribute("alias"))) {
                    // full match, return
                    return bundle;
                  }
                }
              }
            }
          }
        }
      }
    }
    return bundle;
  }

  /**
   * Create the {@link ServerJob} that runs the request as a single atomic transaction
   */
  protected ServerJob createServiceTunnelServerJob(IServerSession serverSession, ServiceTunnelRequest serviceRequest, AtomicReference<ServiceTunnelResponse> serviceResponseHolder, Subject subject) {
    return new RemoteServiceJob(serverSession, serviceRequest, serviceResponseHolder, subject);
  }

  /**
   * runnable content of the {@link ServerJob}, thzis is the atomic transaction
   * <p>
   * This method is part of the protected api and can be overridden.
   */
  protected ServiceTunnelResponse runServerJobTransaction(ServiceTunnelRequest req) throws Exception {
    return runServerJobTransactionWithDelegate(req, getOrderedBundleList(), m_requestMinVersion, m_debug);
  }

  protected ServiceTunnelResponse runServerJobTransactionWithDelegate(ServiceTunnelRequest req, Bundle[] loaderBundles, Version requestMinVersion, boolean debug) throws Exception {
    return new DefaultTransactionDelegate(loaderBundles, requestMinVersion, debug).invoke(req);
  }

  private class RemoteServiceJob extends ServerJob {

    private final ServiceTunnelRequest m_serviceRequest;
    private final AtomicReference<ServiceTunnelResponse> m_serviceResponseHolder;

    public RemoteServiceJob(IServerSession serverSession, ServiceTunnelRequest serviceRequest, AtomicReference<ServiceTunnelResponse> serviceResponseHolder, Subject subject) {
      super("RemoteServiceCall", serverSession, subject);
      m_serviceRequest = serviceRequest;
      m_serviceResponseHolder = serviceResponseHolder;
    }

    public ServiceTunnelRequest getServiceRequest() {
      return m_serviceRequest;
    }

    public AtomicReference<ServiceTunnelResponse> getServiceResponseHolder() {
      return m_serviceResponseHolder;
    }

    @Override
    protected IStatus runTransaction(IProgressMonitor monitor) throws Exception {
      ServiceTunnelResponse serviceRes = runServerJobTransaction(getServiceRequest());
      getServiceResponseHolder().set(serviceRes);
      return Status.OK_STATUS;
    }
  }

  private class AdminServiceJob extends ServerJob {

    protected HttpServletRequest m_request;
    protected HttpServletResponse m_response;

    public AdminServiceJob(IServerSession serverSession, Subject subject, HttpServletRequest request, HttpServletResponse response) {
      super("AdminServiceCall", serverSession, subject);
      m_request = request;
      m_response = response;
    }

    @Override
    protected IStatus runTransaction(IProgressMonitor monitor) throws Exception {
      String key = AdminSession.class.getName();
      AdminSession as = (AdminSession) SessionHandler.getInstance().getAttribute(m_request, key);
      if (as == null) {
        as = new AdminSession();
        SessionHandler.getInstance().setAttribute(m_request, key, as);
      }
      as.serviceRequest(m_request, m_response);
      return Status.OK_STATUS;
    }
  }

}
