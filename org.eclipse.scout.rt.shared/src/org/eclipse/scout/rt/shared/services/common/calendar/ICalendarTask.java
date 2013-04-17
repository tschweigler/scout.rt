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
package org.eclipse.scout.rt.shared.services.common.calendar;

import java.util.Date;

public interface ICalendarTask extends ICalendarItem {

  /**
   * @return the internal responsible id (as {@link Long} if it is a number)
   * @throws UnsupportedOperationException
   *           if internal responsible id is not <code>null</code> or a number
   * @deprecated use {@link #getResponsible()}. method will be removed in 3.10
   */
  @Deprecated
  Long getResponsibleId();

  /**
   * @deprecated use {@link #setResponsible(Object)}. method will be removed in 3.10.
   */
  @Deprecated
  void setResponsibleId(Long n);

  void setResponsible(Object responsible);

  Object getResponsible();

  Date getStart();

  void setStart(Date a);

  Date getDue();

  void setDue(Date a);

  Date getComplete();

  void setComplete(Date a);

}
