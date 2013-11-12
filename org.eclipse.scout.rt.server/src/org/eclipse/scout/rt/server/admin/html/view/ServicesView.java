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
package org.eclipse.scout.rt.server.admin.html.view;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.scout.commons.CompositeObject;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.admin.html.AbstractHtmlAction;
import org.eclipse.scout.rt.server.admin.html.AdminSession;
import org.eclipse.scout.rt.server.admin.html.widget.table.HtmlComponent;
import org.eclipse.scout.rt.server.admin.inspector.ReflectServiceInventory;
import org.eclipse.scout.rt.server.admin.inspector.ServiceInspector;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.shared.security.UpdateServiceConfigurationPermission;
import org.eclipse.scout.rt.shared.services.common.security.ACCESS;
import org.eclipse.scout.service.SERVICES;
import org.eclipse.scout.service.ServiceUtility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ServicesView extends DefaultView {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ServicesView.class);

  private ServiceInspector[] m_serviceInspectors;
  private ServiceInspector m_selectedService;

  public ServicesView(AdminSession as) {
    super(as);
  }

  @Override
  public boolean isVisible() {
    return ACCESS.check(new UpdateServiceConfigurationPermission());
  }

  @Override
  public void activated() {
    // read all services
    m_serviceInspectors = null;
    Activator a = Activator.getDefault();
    if (a != null) {
      SERVICES.getService(String.class);
      BundleContext context = a.getBundle().getBundleContext();
      try {
        ServiceReference[] refs = context.getAllServiceReferences(null, null);
        if (refs != null) {
          try {
            ArrayList<ServiceInspector> list = new ArrayList<ServiceInspector>(refs.length);
            for (ServiceReference ref : refs) {
              Object s = context.getService(ref);
              if (s != null) {
                list.add(new ServiceInspector(s));
              }
            }
            m_serviceInspectors = list.toArray(new ServiceInspector[list.size()]);
          }
          finally {
            for (ServiceReference ref : refs) {
              context.ungetService(ref);
            }
          }
        }
      }
      catch (Exception e) {
        // nop
      }
    }
  }

  @Override
  public void produceTitle(HtmlComponent p) {
    p.print("Services");
  }

  @Override
  public void produceBody(HtmlComponent p) {
    p.startTable(0, 5, 5);
    p.startTableRow();
    p.startTableCell();
    renderServiceTables(p);
    p.startTableCell();
    // selected call
    if (m_selectedService != null) {
      renderServiceDetail(p, m_selectedService);
    }
    p.endTableCell();
    p.endTableRow();
    p.endTable();
  }

  private void renderServiceTables(HtmlComponent p) {
    // categorize services
    TreeMap<CompositeObject, Collection<ServiceInspector>> servicesMap = new TreeMap<CompositeObject, Collection<ServiceInspector>>();
    if (m_serviceInspectors != null) {
      for (ServiceInspector inspector : m_serviceInspectors) {
        String serviceName = inspector.getService().getClass().getSimpleName();
        String sectionName = null;
        int sectionOrder;
        try {
          if (serviceName.matches(".*ProcessService")) {
            sectionOrder = 1;
            sectionName = "Process Services";
          }
          else if (serviceName.matches(".*OutlineService")) {
            sectionOrder = 2;
            sectionName = "Outline Services";
          }
          else if (serviceName.matches(".*LookupService")) {
            sectionOrder = 3;
            sectionName = "Lookup Services";
          }
          else {
            Class[] serviceInterfaces = ServiceUtility.getInterfacesHierarchy(inspector.getService().getClass(), Object.class);
            Class topInterface = (serviceInterfaces.length > 0 ? serviceInterfaces[serviceInterfaces.length - 1] : null);
            if (topInterface != null && topInterface.getPackage() != null && topInterface.getPackage().getName().indexOf(".common.") >= 0) {
              sectionOrder = 4;
              sectionName = "Common Services";
            }
            else {
              sectionOrder = 5;
              sectionName = "Other Services";
            }
          }
          CompositeObject key = new CompositeObject(sectionOrder, sectionName);
          Collection<ServiceInspector> list = servicesMap.get(key);
          if (list == null) {
            list = new ArrayList<ServiceInspector>();
            servicesMap.put(key, list);
          }
          list.add(inspector);
        }
        catch (Throwable t) {
          LOG.warn("Failed inspecting service " + inspector.getService().getClass());
        }
      }
    }
    // tables per section
    for (Map.Entry<CompositeObject, Collection<ServiceInspector>> e : servicesMap.entrySet()) {
      String sectionName = (String) e.getKey().getComponent(1);
      Collection<ServiceInspector> list = e.getValue();
      renderServiceTable(p, sectionName, list);
      p.p();
    }
  }

  private void renderServiceTable(HtmlComponent p, String sectionName, Collection<ServiceInspector> serviceInspectors) {
    // sort
    TreeMap<String, ServiceInspector> sortMap = new TreeMap<String, ServiceInspector>();
    for (ServiceInspector inspector : serviceInspectors) {
      String s = inspector.getService().getClass().getName();
      sortMap.put(s, inspector);
    }

    p.p(sectionName);
    p.startListBox("listBox", 1, true);
    p.listBoxOption(" ", new AbstractHtmlAction("selectService.choose") {
      @Override
      public void run() {
      }
    }, false);
    for (ServiceInspector serviceInspector : sortMap.values()) {
      boolean selected = m_selectedService != null && (m_selectedService.getService() == serviceInspector.getService());
      final ServiceInspector finalServiceInspector = serviceInspector;
      p.listBoxOption(serviceInspector.getService().getClass().getName(), new AbstractHtmlAction("selectService2." + serviceInspector.getService().getClass().getName()) {
        @Override
        public void run() {
          m_selectedService = finalServiceInspector;
        }
      }, selected);
    }
    p.endListBox();

    /*
     * p.startTable(0,0,3,"100%"); p.startTableRow();
     * p.tableHeaderCell(sectionName); p.endTableRow(); for(ServiceInspector
     * serviceInspector: sortMap.values()){
     * renderServiceRow(p,serviceInspector); } p.endTable();
     */
  }

  private void renderServiceRow(HtmlComponent p, final ServiceInspector service) {
    boolean selected = m_selectedService != null && (m_selectedService.getService() == service.getService());
    p.startTableRow();
    p.startTableCell();
    String serviceId = service.getService().getClass().getSimpleName();
    if (selected) {
      p.bold(serviceId);
    }
    else {
      p.linkAction(serviceId, new AbstractHtmlAction("selectService." + service.getService().getClass().getName()) {
        @Override
        public void run() {
          m_selectedService = service;
        }
      });
    }
    p.endTableCell();
    p.endTableRow();
  }

  private void renderServiceDetail(HtmlComponent p, ServiceInspector service) {
    p.bold(service.getService().getClass().getSimpleName());
    p.p();
    ReflectServiceInventory inv;
    try {
      inv = service.buildInventory();
    }
    catch (Throwable t) {
      p.raw("<font color=red>Inventory failed: " + t + "</font>");
      return;
    }
    renderHierarchy(p, service, inv);
    renderProperties(p, service, inv);
    renderOperations(p, service, inv);
    renderStates(p, service, inv);
  }

  private void renderHierarchy(HtmlComponent p, ServiceInspector service, ReflectServiceInventory inv) {
    // hierarchy
    Class serviceClass = service.getService().getClass();
    ArrayList<Class> interfaceHierarchy = new ArrayList<Class>();
    for (Class c : serviceClass.getInterfaces()) {
      interfaceHierarchy.addAll(Arrays.asList(ServiceUtility.getInterfacesHierarchy(c, Object.class)));
    }
    if (interfaceHierarchy.size() == 0) {
      interfaceHierarchy.addAll(Arrays.asList(ServiceUtility.getInterfacesHierarchy(serviceClass, Object.class)));
    }
    interfaceHierarchy.add(serviceClass);
    ArrayList<Class> classHierarchy = new ArrayList<Class>();
    Class test = service.getService().getClass();
    while (test != null) {
      if (Object.class.isAssignableFrom(test)) {
        classHierarchy.add(0, test);
      }
      test = test.getSuperclass();
    }
    //
    p.pBold("Hierarchy");
    p.startTable(1, 0, 4);
    p.startTableRow();
    p.tableHeaderCell("Interfaces");
    p.tableHeaderCell("Classes");
    p.endTableRow();
    p.startTableRow();
    p.startTableCell();
    String prefix = "";
    for (Iterator it = interfaceHierarchy.iterator(); it.hasNext();) {
      Class c = (Class) it.next();
      p.print(prefix + c.getName());
      if (it.hasNext()) {
        p.br();
        prefix = prefix + "&nbsp;&nbsp;";
      }
    }
    p.endTableCell();
    p.startTableCell();
    prefix = "";
    for (Iterator it = classHierarchy.iterator(); it.hasNext();) {
      Class c = (Class) it.next();
      p.print(prefix + c.getName());
      if (it.hasNext()) {
        p.br();
        prefix = prefix + "&nbsp;&nbsp;";
      }
    }
    p.endTableCell();
    p.endTableRow();
    p.endTable();
  }

  private void renderProperties(HtmlComponent p, final ServiceInspector service, ReflectServiceInventory inv) {
    PropertyDescriptor[] properties = inv.getProperties();
    if (properties.length > 0) {
      p.pBold("Properties (" + properties.length + ")");
      p.startTable(1, 0, 4);
      p.startTableRow();
      p.tableHeaderCell("Property name");
      p.tableHeaderCell("Value");
      p.endTableRow();
      for (PropertyDescriptor desc : properties) {
        String propName = desc.getName();
        String propValue = "[value not available]";
        if (desc.getReadMethod() != null) {
          try {
            propValue = formatPropertyValue(desc, desc.getReadMethod().invoke(service.getService(), (Object[]) null));
          }
          catch (Exception e) {
            // nop
          }
        }
        boolean editable = desc.getWriteMethod() != null;
        //
        if (editable) {
          final PropertyDescriptor finalDesc = desc;
          p.startForm(
              new AbstractHtmlAction("changeProp." + service.getService().getClass().getName() + "." + desc.getName()) {
                @Override
                public void run() {
                  String propText = getFormParameter("value", "");
                  if (propText.length() == 0) {
                    propText = null;
                  }
                  try {
                    service.changeProperty(finalDesc, propText);
                  }
                  catch (Exception e) {
                    LOG.error("setting " + finalDesc.getName() + "=" + propText, e);
                  }
                }
              }
              );
        }
        p.startTableRow();
        p.tableCell(propName);
        p.startTableCell();
        if (editable) {
          p.formTextArea("value", getPropertyDisplayValue(propName, propValue));
          p.formSubmit("Change");
        }
        else {
          p.print(getPropertyDisplayValue(propName, propValue));
        }
        p.endTableCell();
        p.endTableRow();
        if (editable) {
          p.endForm();
        }
      }
      p.endTable();
    }
  }

  private void renderOperations(HtmlComponent p, ServiceInspector service, ReflectServiceInventory inv) {
    Method[] operations = inv.getOperations();
    if (operations.length > 0) {
      p.pBold("Operations (" + operations.length + ")");
      p.startTable(1, 0, 4);
      p.startTableRow();
      p.tableHeaderCell("Operation");
      p.tableHeaderCell("Detail");
      p.endTableRow();
      for (Method m : operations) {
        p.startTableRow();
        p.tableCell(m.getName());
        p.tableCell(createSignature(m.getReturnType()) + " " + m.getName() + "(" + createSignature(m.getParameterTypes()) + ")");
        p.endTableRow();
      }
      p.endTable();
    }
  }

  private void renderStates(HtmlComponent p, ServiceInspector service, ReflectServiceInventory inv) {
    String[] states = inv.getStates();
    if (states.length > 0) {
      p.pBold("State");
      p.raw("<pre>");
      for (String s : states) {
        p.print(s);
        p.br();
      }
      p.raw("</pre>");
    }
  }

  /**
   * @return Value to be displayed (sensitive information is not displayed).
   */
  private String getPropertyDisplayValue(String propertyName, String propertyValue) {
    if (isPropertySuppressed(propertyName)) {
      return "***";
    }
    else {
      return propertyValue;
    }
  }

  /**
   * @return true if property contains sensitive information and is therefore not shown
   */
  private boolean isPropertySuppressed(String propertyName) {
    return propertyName != null && propertyName.toLowerCase().contains("password");
  }

  private String formatPropertyValue(PropertyDescriptor p, Object value) {
    Object formattedValue = value;
    return StringUtility.valueOf(formattedValue);
  }

  private String createSignature(Class c) {
    if (c == null) {
      return "void";
    }
    else {
      return createSignature(new Class[]{c});
    }
  }

  private String createSignature(Class[] a) {
    StringBuffer sig = new StringBuffer();
    for (Class c : a) {
      if (sig.length() > 0) {
        sig.append(", ");
      }
      sig.append(c.getSimpleName());
    }
    return sig.toString();
  }

}
