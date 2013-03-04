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
package org.eclipse.scout.rt.client.ui.basic.tree;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;

public class TreeEvent extends EventObject {
  private static final long serialVersionUID = 1L;

  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_INSERTED = 10;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_UPDATED = 20;
  /**
   * no attributes
   */
  public static final int TYPE_NODE_FILTER_CHANGED = 400;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_NODES_DELETED = 30;
  /**
   * valid attributes are nodes, deselectedNodes parentNode is null
   */
  public static final int TYPE_BEFORE_NODES_SELECTED = 35;
  /**
   * valid attributes are nodes, deselectedNodes parentNode is null
   */
  public static final int TYPE_NODES_SELECTED = 40;
  /**
   * valid attributes are parentNode,childNodes
   */
  public static final int TYPE_CHILD_NODE_ORDER_CHANGED = 50;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_EXPANDED = 100;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_COLLAPSED = 101;
  /**
   * valid attributes are node contribute to menus using the addPopupMenu
   * methods
   */
  public static final int TYPE_NODE_POPUP = 700;
  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_ACTION = 705;

  /**
   * valid attributes are parentNode (if common parent of all nodes), nodes
   * register the drag object using the setDragObject method
   */
  public static final int TYPE_NODES_DRAG_REQUEST = 730;
  /**
   * valid attributes are node get the drop object using the getDropObject
   * method
   */
  public static final int TYPE_NODE_DROP_ACTION = 740;
  /**
   * Gui targeted event valid attributes are node
   */
  public static final int TYPE_NODE_REQUEST_FOCUS = 200;
  /**
   * Gui targeted event valid attributes are node
   */
  public static final int TYPE_NODE_ENSURE_VISIBLE = 300;

  public static final int TYPE_REQUEST_FOCUS = 800;

  /**
   * valid attributes are node
   */
  public static final int TYPE_NODE_CLICK = 820;

  /**
   * Advise to scroll to selection
   */
  public static final int TYPE_SCROLL_TO_SELECTION = 830;
  // next 840

  private final int m_type;
  private ITreeNode m_commonParentNode;
  private ITreeNode[] m_nodes;
  private ITreeNode[] m_deselectedNodes;
  private ITreeNode[] m_newSelectedNodes;
  private List<IMenu> m_popupMenus;
  private boolean m_consumed;
  private TransferObject m_dragObject;
  private TransferObject m_dropObject;

  public TreeEvent(ITree source, int type) {
    super(source);
    m_type = type;
  }

  public TreeEvent(ITree source, int type, ITreeNode node) {
    super(source);
    m_type = type;
    if (node != null) {
      m_nodes = new ITreeNode[]{node};
    }
    m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
  }

  public TreeEvent(ITree source, int type, ITreeNode[] nodes) {
    super(source);
    m_type = type;
    if (nodes != null) {
      m_nodes = nodes;
    }
    m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
  }

  public TreeEvent(ITree source, int type, ITreeNode parentNode, ITreeNode[] childNodes) {
    super(source);
    m_type = type;
    if (childNodes != null) {
      m_nodes = childNodes;
    }
    m_commonParentNode = parentNode;
    if (m_commonParentNode == null) {
      m_commonParentNode = TreeUtility.calculateCommonParentNode(m_nodes);
    }
  }

  public ITree getTree() {
    return (ITree) getSource();
  }

  public int getType() {
    return m_type;
  }

  public ITreeNode getCommonParentNode() {
    return m_commonParentNode;
  }

  public ITreeNode getDeselectedNode() {
    if (m_deselectedNodes != null && m_deselectedNodes.length > 0) {
      return m_deselectedNodes[0];
    }
    else {
      return null;
    }
  }

  public ITreeNode[] getDeselectedNodes() {
    if (m_deselectedNodes != null) {
      return m_deselectedNodes;
    }
    else {
      return new ITreeNode[0];
    }
  }

  protected void setDeselectedNodes(ITreeNode[] deselectedNodes) {
    m_deselectedNodes = deselectedNodes;
  }

  public ITreeNode getNewSelectedNode() {
    if (m_newSelectedNodes != null && m_newSelectedNodes.length > 0) {
      return m_newSelectedNodes[0];
    }
    else {
      return null;
    }
  }

  public ITreeNode[] getNewSelectedNodes() {
    if (m_newSelectedNodes != null) {
      return m_newSelectedNodes;
    }
    else {
      return new ITreeNode[0];
    }
  }

  protected void setNewSelectedNodes(ITreeNode[] newSelectedNodes) {
    m_newSelectedNodes = newSelectedNodes;
  }

  public ITreeNode getNode() {
    if (m_nodes != null && m_nodes.length > 0) {
      return m_nodes[0];
    }
    else {
      return null;
    }
  }

  public ITreeNode[] getNodes() {
    if (m_nodes != null) {
      return m_nodes;
    }
    else {
      return new ITreeNode[0];
    }
  }

  public ITreeNode getChildNode() {
    return getNode();
  }

  public ITreeNode[] getChildNodes() {
    return getNodes();
  }

  public void addPopupMenu(IMenu menu) {
    if (menu != null) {
      if (m_popupMenus == null) {
        m_popupMenus = new ArrayList<IMenu>();
      }
      m_popupMenus.add(menu);
    }
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public void addPopupMenus(IMenu[] menus) {
    if (menus != null) {
      if (m_popupMenus == null) {
        m_popupMenus = new ArrayList<IMenu>();
      }
      m_popupMenus.addAll(Arrays.asList(menus));
    }
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public IMenu[] getPopupMenus() {
    if (m_popupMenus != null) {
      return m_popupMenus.toArray(new IMenu[0]);
    }
    else {
      return new IMenu[0];
    }
  }

  /**
   * used by TYPE_ROW_POPUP to add actions
   */
  public int getPopupMenuCount() {
    if (m_popupMenus != null) {
      return m_popupMenus.size();
    }
    else {
      return 0;
    }
  }

  /**
   * used by TYPE_ROW_DRAG_REQUEST
   */
  public TransferObject getDragObject() {
    return m_dragObject;
  }

  public void setDragObject(TransferObject t) {
    m_dragObject = t;
  }

  /**
   * used by TYPE_ROW_DROP_ACTION
   */
  public TransferObject getDropObject() {
    return m_dropObject;
  }

  protected void setDropObject(TransferObject t) {
    m_dropObject = t;
  }

  /**
   * @deprecated Use {@link TreeUtility#calculateCommonParentNode(ITreeNode[])}; Will be removed in Release 3.10.
   */
  @Deprecated
  public static ITreeNode calculateCommonParentNode(ITreeNode[] nodes) {
    return TreeUtility.calculateCommonParentNode(nodes);
  }

  public boolean isConsumed() {
    return m_consumed;
  }

  public void consume() {
    m_consumed = true;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("TreeEvent[");
    // nodes
    if (m_nodes != null && m_nodes.length > 0 && getTree() != null) {
      if (m_nodes.length == 1) {
        buf.append("\"" + m_nodes[0] + "\"");
      }
      else {
        buf.append("{");
        for (int i = 0; i < m_nodes.length; i++) {
          if (i >= 0) {
            buf.append(",");
          }
          buf.append("\"" + m_nodes[i] + "\"");
        }
        buf.append("}");
      }
    }
    else {
      buf.append("{}");
    }
    buf.append(" ");
    // decode type
    try {
      Field[] f = getClass().getDeclaredFields();
      for (int i = 0; i < f.length; i++) {
        if (Modifier.isPublic(f[i].getModifiers()) && Modifier.isStatic(f[i].getModifiers()) && f[i].getName().startsWith("TYPE_")) {
          if (((Number) f[i].get(null)).intValue() == m_type) {
            buf.append(f[i].getName());
            break;
          }
        }
      }
    }
    catch (Throwable t) {
      buf.append("#" + m_type);
    }
    buf.append("]");
    return buf.toString();
  }

}
