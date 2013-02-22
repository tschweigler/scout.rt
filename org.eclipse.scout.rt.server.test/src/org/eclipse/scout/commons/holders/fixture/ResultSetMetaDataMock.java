/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.holders.fixture;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class ResultSetMetaDataMock implements InvocationHandler/*, java.sql.ResultSetMetaData*/{
  private final ResultSetMetaData m_meta;
  private Object[][] m_resultData;

  public ResultSetMetaDataMock(Object[][] resultData) {
    m_resultData = resultData;
    m_meta = (ResultSetMetaData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSetMetaData.class}, this);
  }

  public ResultSetMetaData getResultSetMetaData() {
    return m_meta;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Method m = this.getClass().getMethod(method.getName(), method.getParameterTypes());
    return m.invoke(this, args);
  }

  public int getColumnCount() throws SQLException {
    if (m_resultData != null && m_resultData.length > 0) {
      return m_resultData[0].length;
    }
    return 0;
  }

  public int getColumnType(int column) throws SQLException {
    return Types.OTHER;
  }

  public int getPrecision(int column) throws SQLException {
    return 0;
  }

  public int getScale(int column) throws SQLException {
    return 0;
  }

}
