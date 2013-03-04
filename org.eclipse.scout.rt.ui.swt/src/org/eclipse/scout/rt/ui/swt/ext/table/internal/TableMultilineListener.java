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
package org.eclipse.scout.rt.ui.swt.ext.table.internal;

import java.text.BreakIterator;
import java.util.Set;

import org.eclipse.scout.commons.ListUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.ui.swt.Activator;
import org.eclipse.scout.rt.ui.swt.SwtIcons;
import org.eclipse.scout.rt.ui.swt.basic.table.ISwtScoutTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * A table listener for multiline and wrapped tables.
 */
public class TableMultilineListener implements Listener {
  private final int m_text_margin_y;
  private final int m_text_margin_x;
  private static final int IMAGE_TEXT_PADDING = 1;
  private static final String LINE_SEPARATOR = "\n";

  private final boolean m_multiline;
  private final int m_rowHeight;
  private final Image m_editableMarkerImage;
  private final int m_editableMarkerWidth;
  private final Set<Integer> m_wrapTextColumns;

  /**
   * @param multiline
   *          true, if the table is multiline
   * @param rowHeight
   *          row height of the table (in pixel)
   * @param wrapTextColumns
   *          the wrapped column indexes
   * @param textMarginX
   *          horizontal text margin
   * @param textMarginY
   *          vertical text margin
   */
  public TableMultilineListener(boolean multiline, int rowHeight, Set<Integer> wrapTextColumns, int textMarginX, int textMarginY) {
    m_multiline = multiline;
    m_rowHeight = rowHeight;
    m_wrapTextColumns = wrapTextColumns;
    m_text_margin_y = textMarginY;
    m_text_margin_x = textMarginX;
    m_editableMarkerImage = Activator.getIcon(SwtIcons.CellEditable);
    m_editableMarkerWidth = (m_editableMarkerImage != null) ? m_editableMarkerImage.getBounds().width : 0;
  }

  /**
   * @return row height of the table (in pixel)
   */
  public int getRowHeight() {
    return m_rowHeight;
  }

  /**
   * Wraps the text to fit the bounds. Line breaks are inserted at word breaks.
   * 
   * @param gc
   *          the graphics context
   * @param text
   *          the text to wrap, may be <code>null</code>
   * @param bounds
   *          the text bounds
   * @return the wrapped text
   */
  protected String softWrapText(GC gc, String text, Rectangle bounds) {
    if (StringUtility.isNullOrEmpty(text)) {
      return text;
    }
    BreakIterator wb = BreakIterator.getWordInstance();
    wb.setText(text);
    int saved = 0;
    int last = 0;
    int width = bounds.width;

    String wrappedText = "";

    for (int loc = wb.first(); loc != BreakIterator.DONE; loc = wb.next()) {
      String line = text.substring(saved, loc);
      int textWidth = getTextWidth(gc, line);

      if (textWidth > width) {
        // overflow
        String prevLine = text.substring(saved, last);
        wrappedText += prevLine.trim();
        wrappedText += LINE_SEPARATOR;
        saved = last;
      }
      last = loc;
    }
    // paint the last line
    String lastLine = text.substring(saved, last);
    wrappedText += lastLine.trim();
    return wrappedText;
  }

  /**
   * @param gc
   *          the graphics context
   * @param line
   *          the line to be displayed
   * @return width of the area the line covers
   */
  protected int getTextWidth(GC gc, String line) {
    return gc.textExtent(line).x;
  }

  /**
   * Handles the table event. Calculates width and height and draws the wrapped and trimmed text accordingly.
   */
  @Override
  public void handleEvent(Event event) {
    switch (event.type) {
      case SWT.MeasureItem:
        Point mSize = new Point(0, 0);
        TableItem mitem = (TableItem) event.item;
        Image img = mitem.getImage(event.index);
        int editableIconOffset = 0;
        if (img != null) {
          editableIconOffset = isEditableIconNeeded(event, mitem) ? m_editableMarkerWidth : 1;
          Rectangle imgBounds = img.getBounds();
          img.getBounds().width += editableIconOffset;
          mSize.x += imgBounds.width + 2;
          mSize.y = Math.max(mSize.y, imgBounds.height + 2 * m_text_margin_y);
        }
        String mtext = getCelldisplayText(event, mitem);
        Point textSize = m_multiline ? event.gc.textExtent(mtext) : event.gc.stringExtent(mtext);
        mSize.x += textSize.x + 2 * m_text_margin_x;
        mSize.y = Math.max(mSize.y, textSize.y + 2 * m_text_margin_y);
        event.width = mSize.x + editableIconOffset;
        event.height = Math.max(event.height, mSize.y);
        break;
      case SWT.PaintItem:
        TableItem pitem = (TableItem) event.item;
        String ptext = getCelldisplayText(event, pitem);

        //alignment
        int align = SWT.LEFT;
        TableColumn tc = pitem.getParent().getColumn(event.index);
        if (tc != null) {
          align = (tc.getStyle() & (SWT.CENTER | SWT.LEFT | SWT.RIGHT));
        }

        /* center column 1 vertically */
        Rectangle itemBounds = pitem.getBounds(event.index);
        editableIconOffset = isEditableIconNeeded(event, pitem) ? m_editableMarkerWidth : 1;
        int xImageOffset = itemBounds.x;
        int xTextOffset = xImageOffset + m_text_margin_x;
        int yOffset = itemBounds.y + m_text_margin_y;
        Point textExtent = event.gc.stringExtent(ptext);

        Image pImg = pitem.getImage(event.index);
        Rectangle contentBounds = null;
        if (pImg != null) {
          contentBounds = pImg.getBounds();
          if (textExtent.x > 0) {
            //Add padding between image and text
            contentBounds.width = contentBounds.width + IMAGE_TEXT_PADDING;
            xTextOffset += contentBounds.width;
          }
        }
        else {
          contentBounds = new Rectangle(0, 0, 0, 0);
        }

        contentBounds.width += textExtent.x;
        contentBounds.height += textExtent.y;
        if (align == SWT.RIGHT) {
          int dx = Math.max(m_text_margin_x, (itemBounds.x + itemBounds.width - xImageOffset - contentBounds.width - m_text_margin_x));
          xTextOffset += dx;

          //Aligning the image leads to an ugly space when row gets selected...
//            xImageOffset += dx;
        }
        else if (align == SWT.CENTER) {
          int dx = Math.max(m_text_margin_x, (itemBounds.x + itemBounds.width - xImageOffset - contentBounds.width - m_text_margin_x) / 2);
          xTextOffset += dx;

          //Aligning the image leads to an ugly space when row gets selected...
//            xImageOffset += dx;
        }

        if (pImg != null) {
          event.gc.drawImage(pImg, xImageOffset, yOffset);
        }
        event.gc.drawText(ptext, xTextOffset, yOffset, true);
        event.gc.setForeground(event.gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
        TableItem item = (TableItem) event.item;
        if (isEditableIconNeeded(event, item)) {
          IColumn<?> col = ((IColumn<?>) item.getParent().getColumn(event.index).getData(ISwtScoutTable.KEY_SCOUT_COLUMN));
          ICell cell = ((ITableRow) item.getData()).getCell(col);
          Image markerIcon = m_editableMarkerImage;
          if (markerIcon != null && cell.isEditable()) {
            event.gc.drawImage(markerIcon, event.x, event.y);
          }
        }

        break;
      case SWT.EraseItem:
        //Focus rectangle looks ugly in Windows XP if there is a column with an image so we just remove the rectangle
        //In Windows Vista it would look well.
        event.detail &= ~SWT.FOCUSED;

        //Foreground should not be painted because this is done by ourself with the SWT.PaintItem-Event.
        event.detail &= ~SWT.FOREGROUND;
        break;
    }
  }

  /**
   * @param event
   *          the table event
   * @param item
   *          the cell item
   * @return the wrapped and trimmed cell display text, if necessary
   */
  protected String getCelldisplayText(Event event, TableItem item) {
    Rectangle itemBounds = item.getBounds(event.index);
    String text = item.getText(event.index);
    if (StringUtility.hasText(text)) {
      if (isWrapped(event.index, item.getParent())) {
        text = softWrapText(event.gc, text, new Rectangle(itemBounds.x, itemBounds.y, itemBounds.width - m_text_margin_x * 2, itemBounds.height - m_text_margin_y * 2));
      }
      FontMetrics fm = event.gc.getFontMetrics();
      if (fm != null) {
        int fontHeight = fm.getHeight();
        text = trimToRowHeight(text, fontHeight);
      }
    }
    return text;
  }

  private boolean isEditableIconNeeded(Event event, TableItem item) {
    IColumn<?> col = ((IColumn<?>) item.getParent().getColumn(event.index).getData(ISwtScoutTable.KEY_SCOUT_COLUMN));
    if (col != null && col.isEditable() && !col.getDataType().isAssignableFrom(Boolean.class)) {
      return true;
    }
    return false;
  }

  /**
   * @param uiColumnIndex
   *          the column index in the table (UI not scout model)
   * @param table
   *          the table
   * @return true, if the column is wrapped, false otherwise
   */
  private boolean isWrapped(int uiColumnIndex, Table table) {
    int columnIndex = ((IColumn<?>) table.getColumn(uiColumnIndex).getData(ISwtScoutTable.KEY_SCOUT_COLUMN)).getColumnIndex();
    return ListUtility.containsAny(m_wrapTextColumns, columnIndex);
  }

  /**
   * Trims a given text to the maximum row height of the table. If the row height is <=0 or the line height is <=0, the
   * complete text is returned.
   * 
   * @param text
   *          the text to trim
   * @param lineHeight
   *          the height of a line in pixel
   * @return the trimmed String
   */
  protected String trimToRowHeight(String text, int lineHeight) {
    if (getRowHeight() > 0 && lineHeight > 0 && text != null) {
      int maxLineCount = getRowHeight() / lineHeight;
      int lineCount = Math.min(StringUtility.getLineCount(text), maxLineCount);

      String[] lines = StringUtility.getLines(text);

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lineCount - 1; i++) {
        sb.append(lines[i]).append(LINE_SEPARATOR);
      }
      if (lineCount > 0) {
        sb.append(lines[lineCount - 1]);
      }
      text = sb.toString();
    }
    return text;
  }
}
