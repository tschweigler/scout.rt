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
package org.eclipse.scout.rt.client.mobile.transformation;

import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.service.IService;

/**
 * @since 3.9.0
 */
public interface IDeviceTransformationService extends IService {

  void install();

  void install(IDesktop desktop);

  void uninstall();

  IDeviceTransformer getDeviceTransformer();
}
