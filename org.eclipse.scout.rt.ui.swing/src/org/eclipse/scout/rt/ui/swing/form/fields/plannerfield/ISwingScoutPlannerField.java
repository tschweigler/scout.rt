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
package org.eclipse.scout.rt.ui.swing.form.fields.plannerfield;

import org.eclipse.scout.rt.client.ui.form.fields.plannerfield.IPlannerField;
import org.eclipse.scout.rt.ui.swing.basic.activitymap.SwingScoutActivityMap;
import org.eclipse.scout.rt.ui.swing.basic.table.ISwingScoutTable;
import org.eclipse.scout.rt.ui.swing.form.fields.ISwingScoutFormField;

public interface ISwingScoutPlannerField extends ISwingScoutFormField<IPlannerField<?, ?, ?, ?>> {

  ISwingScoutTable getResourceTableComposite();

  SwingScoutActivityMap getActivityMapComposite();

}
