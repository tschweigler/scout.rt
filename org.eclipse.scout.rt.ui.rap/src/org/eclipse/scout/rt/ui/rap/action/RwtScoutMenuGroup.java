/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.action;

import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.ui.rap.IRwtEnvironment;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * <h3>RwtScoutMenuGroup</h3> ...
 * 
 * @since 3.7.0 June 2011
 */
public class RwtScoutMenuGroup extends AbstractRwtMenuAction {

  public RwtScoutMenuGroup(Menu uiMenu, IAction scoutMenu, IRwtEnvironment uiEnvironment) {
    super(uiMenu, scoutMenu, uiEnvironment, true);
  }

  @Override
  protected void initializeUi(Menu uiMenu) {
    MenuItem item = new MenuItem(uiMenu, SWT.CASCADE);
    setUiMenuItem(item);
  }
}
