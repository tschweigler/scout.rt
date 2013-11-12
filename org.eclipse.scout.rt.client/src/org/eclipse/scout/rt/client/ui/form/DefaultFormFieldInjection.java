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
package org.eclipse.scout.rt.client.ui.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.BeanUtility;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.annotations.InjectFieldTo;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.ICompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;

/**
 * Default implementation that inserts new fields at the right place based on their {@link Order} annotation and that
 * replaces existing fields using the {@link Replace} annotation.
 * 
 * @since 3.8.2
 */
public class DefaultFormFieldInjection implements IFormFieldInjection {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultFormFieldInjection.class);

  private final Map<IFormField, Set<Class<? extends IFormField>>> m_replacingFormFieldsByContainer;
  private final Map<Class<?>, Class<? extends IFormField>> m_replacementMapping;
  private final ArrayList<Class<? extends IFormField>> m_injectedFieldList;
  private final Object m_enclosingContext;

  private Set<Class<? extends IFormField>> m_injectingFields;
  private Set<Class<? extends IFormField>> m_replacingFields;

  /**
   * Creates a new instance for the given enclosing context (i.e. an {@link IForm} or an {@link ICompositeField}).
   * 
   * @param enclosingContext
   */
  public DefaultFormFieldInjection(Object enclosingContext) {
    m_replacingFormFieldsByContainer = new HashMap<IFormField, Set<Class<? extends IFormField>>>();
    m_replacementMapping = new HashMap<Class<?>, Class<? extends IFormField>>();
    m_injectedFieldList = new ArrayList<Class<? extends IFormField>>();
    m_enclosingContext = enclosingContext;
  }

  public void addField(Class<? extends IFormField> fieldClass) {
    if (fieldClass == null) {
      throw new IllegalArgumentException("fieldClass must not be null");
    }
    Replace replace = fieldClass.getAnnotation(Replace.class);
    InjectFieldTo injectFieldTo = fieldClass.getAnnotation(InjectFieldTo.class);
    if (replace == null && injectFieldTo == null) {
      LOG.warn("Ignoring field [" + fieldClass + "] since neither @" + InjectFieldTo.class.getSimpleName() + " nor @" + Replace.class.getSimpleName() + " is declared.");
      return;
    }
    if (replace != null && injectFieldTo != null) {
      LOG.warn("@" + InjectFieldTo.class.getSimpleName() + " annotation is ignored since @" + Replace.class.getSimpleName() + " is available as well on class [" + fieldClass + "]. You should remove one of both annotations.");
    }
    else if (injectFieldTo != null && !ICompositeField.class.isAssignableFrom(injectFieldTo.value())) {
      LOG.warn("Ignoring field [" + fieldClass + "] since it is not injected into an " + ICompositeField.class.getSimpleName() + ", but @" + InjectFieldTo.class.getSimpleName() + "(" + injectFieldTo.value() + ")");
      return;
    }
    m_injectedFieldList.add(fieldClass);
    m_injectingFields = null;
    m_replacingFields = null;
  }

  public void addFields(Class<? extends IFormField>[] fieldClasses) {
    for (Class<? extends IFormField> c : fieldClasses) {
      addField(c);
    }
  }

  /**
   * @return Returns the mapping of all replaced fields or <code>null</code>, if no field replacements were performed.
   */
  public Map<Class<?>, Class<? extends IFormField>> getReplacementMapping() {
    if (m_replacementMapping.isEmpty()) {
      return null;
    }
    return m_replacementMapping;
  }

  private void ensurePrepared() {
    if (m_injectingFields == null || m_replacingFields == null) {
      prepare();
    }
  }

  private void prepare() {
    m_injectingFields = new HashSet<Class<? extends IFormField>>();
    m_replacingFields = new HashSet<Class<? extends IFormField>>();
    // 1. separate injected fields by annotation
    for (Class<? extends IFormField> f : m_injectedFieldList) {
      if (f.isAnnotationPresent(Replace.class)) {
        m_replacingFields.add(f);
      }
      else if (f.isAnnotationPresent(InjectFieldTo.class)) {
        m_injectingFields.add(f);
      }
    }

    // 2. remove transitive replacements (i.e. compute replacing leaf classes)
    if (!m_replacingFields.isEmpty()) {
      @SuppressWarnings("unchecked")
      Class<? extends IFormField>[] classes = m_replacingFields.toArray(new Class[m_replacingFields.size()]);
      m_replacingFields = ConfigurationUtility.getReplacingLeafClasses(classes);
    }

    // 3. remove injected fields that are replaced and treat those replacing fields as injected fields
    if (!m_injectingFields.isEmpty() && !m_replacingFields.isEmpty()) {
      Set<Class<? extends IFormField>> replacingInjectedFields = new HashSet<Class<? extends IFormField>>();
      for (Class<? extends IFormField> replacingField : m_replacingFields) {
        for (Iterator<Class<? extends IFormField>> it = m_injectingFields.iterator(); it.hasNext();) {
          Class<? extends IFormField> injectedField = it.next();
          if (injectedField.isAssignableFrom(replacingField)) {
            it.remove();
            replacingInjectedFields.add(replacingField);
            // do not break since injectedField could extend another injected field
          }
        }
      }
      m_replacingFields.removeAll(replacingInjectedFields);
      m_injectingFields.addAll(replacingInjectedFields);
    }
  }

  @Override
  public void filterFields(IFormField container, List<Class<? extends IFormField>> fieldList) {
    if (container == null || fieldList == null || fieldList.isEmpty()) {
      return;
    }

    ensurePrepared();
    if (m_replacingFields.isEmpty()) {
      return;
    }

    Set<Class<? extends IFormField>> replacingFields = new HashSet<Class<? extends IFormField>>();
    // remove all replaced classes
    for (Iterator<Class<? extends IFormField>> it = fieldList.iterator(); it.hasNext();) {
      Class<? extends IFormField> field = it.next();
      for (Class<? extends IFormField> replacingField : m_replacingFields) {
        if (field.isAssignableFrom(replacingField)) {
          replacingFields.add(replacingField);
          it.remove();
          break;
        }
      }
    }
    if (!replacingFields.isEmpty()) {
      m_replacingFormFieldsByContainer.put(container, replacingFields);
    }
  }

  @Override
  public void injectFields(IFormField container, List<IFormField> fieldList) {
    if (container == null || fieldList == null || m_injectedFieldList.isEmpty()) {
      return;
    }

    ensurePrepared();

    // 1. replace fields if required
    Set<Class<? extends IFormField>> replacingFields = m_replacingFormFieldsByContainer.remove(container);
    if (replacingFields != null) {
      for (Class<? extends IFormField> replacingField : replacingFields) {
        // compute order and create field
        Order order = getOrder(replacingField);
        createAndInsertField(container, fieldList, replacingField, order);
        addReplacementMappings(replacingField);
      }
    }

    // 2. insert injected fields
    for (Class<? extends IFormField> injectedField : m_injectingFields) {
      Class<?> tmpClass = injectedField;
      while (tmpClass.isAnnotationPresent(Replace.class)) {
        tmpClass = tmpClass.getSuperclass();
      }
      if (tmpClass.getAnnotation(InjectFieldTo.class).value() == container.getClass()) {
        Order order = getOrder(injectedField);
        createAndInsertField(container, fieldList, injectedField, order);
        addReplacementMappings(injectedField);
      }
    }
  }

  /**
   * Adds class mappings from replaced classes to the given one. This method does nothing if the given
   * <code>field</code> is not annotated with {@link Replace}.
   * 
   * @param field
   */
  private void addReplacementMappings(Class<? extends IFormField> field) {
    Class<?> tmpClass = field;
    while (tmpClass.isAnnotationPresent(Replace.class)) {
      tmpClass = tmpClass.getSuperclass();
      m_replacementMapping.put(tmpClass, field);
    }
  }

  /**
   * Adds the field f to the list at the right place regarding the {@link Order} annotation.
   */
  private void createAndInsertField(IFormField container, List<IFormField> list, Class<? extends IFormField> fieldClass, Order order) {
    //check if list already contains the field
    for (IFormField f : list) {
      if (f.getClass() == fieldClass) {
        return;
      }
    }

    try {
      IFormField f = newInnerInstance(container, fieldClass);

      // add ordered
      if (order != null) {
        double orderValue = order.value();
        for (int i = 0, n = list.size(); i < n; i++) {
          Class<?> existingClazz = list.get(i).getClass();
          Order existingClazzOrder = getOrder(existingClazz);
          if (existingClazzOrder != null && orderValue < existingClazzOrder.value()) {
            list.add(i, f);
            return;
          }
        }
      }

      // default at end
      list.add(f);
    }
    catch (Exception e) {
      LOG.error("exception while injecting a field", e);
    }
  }

  /**
   * Returns the first {@link Order} annotation found up the replacement hierarchy.
   */
  private Order getOrder(Class<?> c) {
    Order order = c.getAnnotation(Order.class);
    while (order == null && c.isAnnotationPresent(Replace.class)) {
      c = c.getSuperclass();
      order = c.getAnnotation(Order.class);
    }
    return order;
  }

  /**
   * Creates a new form field instance within the given container.
   * 
   * @param container
   * @param fieldClass
   */
  private <T> T newInnerInstance(IFormField container, Class<T> fieldClass) throws Exception {
    try {
      // try to create the field using its container
      T t = BeanUtility.createInstance(fieldClass, m_enclosingContext, container);
      if (t != null) {
        return t;
      }
    }
    catch (Throwable t) {
    }
    return ConfigurationUtility.newInnerInstance(m_enclosingContext, fieldClass);
  }
}
