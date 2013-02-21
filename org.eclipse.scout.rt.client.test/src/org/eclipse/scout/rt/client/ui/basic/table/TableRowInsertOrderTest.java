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
package org.eclipse.scout.rt.client.ui.basic.table;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractIntegerColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=395185">Bug 395185 -Table: Rows contained at TableEvent are in
 * random order when fired as batch</a>
 * <p>
 * If table rows are inserted while tableChanging is active, a batch event will be fired at the end. This batch event
 * should contain the table rows in the same order as they were inserted.
 */
@RunWith(ScoutClientTestRunner.class)
public class TableRowInsertOrderTest {

  @Test
  public void testEventRowOrderAfterInsert() throws Exception {
    P_Table table = new P_Table();
    table.initTable();

    P_TableListener tableListener = new P_TableListener();
    table.addTableListener(tableListener);

    table.setTableChanging(true);
    try {
      for (int i = 0; i < 10; i++) {
        table.addRowByArray(new Object[]{i, "Item" + i});
      }
    }
    finally {
      table.setTableChanging(false);
    }

    Assert.assertArrayEquals(table.getRows(), tableListener.getInsertedRows());
    //No order_change_event expected
    Assert.assertTrue(tableListener.getOrderedRows() == null);
  }

  @Test
  public void testEventRowOrderAfterInsertWithSort() throws Exception {
    P_Table table = new P_Table();
    table.getFirstColumn().setInitialSortIndex(0);
    table.getFirstColumn().setInitialSortIndex(1);
    table.getFirstColumn().setInitialSortAscending(false);
    table.initTable();

    P_TableListener tableListener = new P_TableListener();
    table.addTableListener(tableListener);

    table.setTableChanging(true);
    try {
      for (int i = 0; i < 10; i++) {
        table.addRowByArray(new Object[]{i, "Item" + i});
      }
    }
    finally {
      table.setTableChanging(false);
    }

    Assert.assertTrue(table.getRows()[0] != tableListener.getInsertedRows()[0]);
    Assert.assertArrayEquals(table.getRows(), tableListener.getOrderedRows());
  }

  private static class P_TableListener extends TableAdapter {
    private ITableRow[] m_insertedRows;
    private ITableRow[] m_orderedRows;

    @Override
    public void tableChanged(TableEvent e) {
      if (e.getType() == TableEvent.TYPE_ROWS_INSERTED) {
        m_insertedRows = e.getRows();
      }
      else if (e.getType() == TableEvent.TYPE_ROW_ORDER_CHANGED) {
        m_orderedRows = e.getRows();
      }
    }

    public ITableRow[] getInsertedRows() {
      return m_insertedRows;
    }

    public ITableRow[] getOrderedRows() {
      return m_orderedRows;
    }

  }

  private static class P_Table extends AbstractTable {

    public FirstColumn getFirstColumn() {
      return getColumnSet().getColumnByClass(FirstColumn.class);
    }

    public SecondColumn getSecondColumn() {
      return getColumnSet().getColumnByClass(SecondColumn.class);
    }

    @Order(10)
    public class FirstColumn extends AbstractIntegerColumn {
    }

    @Order(20)
    public class SecondColumn extends AbstractStringColumn {
    }
  }

}
