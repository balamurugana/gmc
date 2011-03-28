/*******************************************************************************
 * Copyright (c) 2011 Gluster, Inc. <http://www.gluster.com>
 * This file is part of Gluster Management Console.
 *
 * Gluster Management Console is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *  
 * Gluster Management Console is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.gluster.storage.management.gui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import com.gluster.storage.management.core.model.Disk;
import com.gluster.storage.management.core.model.Volume;
import com.gluster.storage.management.gui.dialogs.MigrateDiskWizard;

public class MigrateDiskAction extends AbstractActionDelegate {
	private Volume volume;
	private Disk disk;

	@Override
	public void run(IAction action) {
//		MigrateDiskDialog dialog = new MigrateDiskDialog(window.getShell(), volume, disk);
// 		dialog.create();
// 		dialog.open();
		MigrateDiskWizard wizard = new MigrateDiskWizard(volume, disk);

		WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
		dialog.create();
		dialog.getShell().setSize(1024, 600);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);

		if (selectedEntity instanceof Volume) {
			volume = (Volume) selectedEntity;
		}

		action.setEnabled(false);
		if (selectedEntity instanceof Disk) {
			disk = (Disk) selectedEntity;
			action.setEnabled(((StructuredSelection) selection).size() == 1);
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}
}