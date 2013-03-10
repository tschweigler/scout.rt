/*******************************************************************************
 * Copyright (c) 2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.extension.client.ui.desktop.outline.pages.fixture.modification;

import java.util.Collection;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.extension.client.ui.desktop.outline.pages.AbstractExtensiblePageWithNodes;

/**
 * @since 3.9.0
 */
public class PageModificationNodePage extends AbstractExtensiblePageWithNodes {

  @Override
  protected void execCreateChildPages(Collection<IPage> pageList) throws ProcessingException {
    pageList.add(new AModificationPageWithNodes());
    pageList.add(new BModificationPageWithNodes());
    pageList.add(new C1ModificationPageWithNodes());
    pageList.add(new C2ModificationPageWithNodes());
  }
}
