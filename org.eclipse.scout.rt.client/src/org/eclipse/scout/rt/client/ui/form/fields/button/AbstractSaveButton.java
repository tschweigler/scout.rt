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
package org.eclipse.scout.rt.client.ui.form.fields.button;

import org.eclipse.scout.rt.shared.ScoutTexts;

public abstract class AbstractSaveButton extends AbstractButton implements IButton {

  public AbstractSaveButton() {
    this(true);
  }

  public AbstractSaveButton(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */
  @Override
  protected int getConfiguredSystemType() {
    return SYSTEM_TYPE_SAVE;
  }

  @Override
  protected String getConfiguredLabel() {
    return ScoutTexts.get("SaveButton");
  }

  @Override
  protected String getConfiguredTooltipText() {
    return ScoutTexts.get("SaveButtonTooltip");
  }

  @Override
  protected int getConfiguredHorizontalAlignment() {
    return -1;
  }
}
