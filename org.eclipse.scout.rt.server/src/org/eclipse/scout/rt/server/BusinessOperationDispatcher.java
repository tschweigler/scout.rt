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
package org.eclipse.scout.rt.server;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * @deprecated use {@link DefaultTransactionDelegate}. Will be removed in Release 3.10.
 */
@Deprecated
public class BusinessOperationDispatcher extends DefaultTransactionDelegate {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BusinessOperationDispatcher.class);

  public BusinessOperationDispatcher(Bundle[] loaderBundles, Version requestMinVersion, boolean debug) {
    super(loaderBundles, requestMinVersion, debug);
  }
}
