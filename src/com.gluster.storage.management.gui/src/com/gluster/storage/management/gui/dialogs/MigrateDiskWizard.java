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
package com.gluster.storage.management.gui.dialogs;

import org.eclipse.jface.wizard.Wizard;

import com.gluster.storage.management.client.GlusterDataModelManager;
import com.gluster.storage.management.client.VolumesClient;
import com.gluster.storage.management.core.model.Disk;
import com.gluster.storage.management.core.model.Volume;

public class MigrateDiskWizard extends Wizard {
	private Volume volume;
	private Disk disk;
	private MigrateDiskPage1 page;

	public MigrateDiskWizard(Volume volume, Disk disk) {
		setWindowTitle("Gluster Management Console - Migrate Disk [" + volume.getName() + "]");
		this.volume = volume;
		this.disk = disk;
		setHelpAvailable(false); // TODO: Introduce wizard help
	}

	@Override
	public void addPages() {
		page = new MigrateDiskPage1(volume, disk);
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		Disk sourceDisk = page.getSourceDisk();
		Disk targetDisk = page.getTargetDisk();
		// TODO add custom confirm dialog
		
		VolumesClient volumesClient = new VolumesClient(GlusterDataModelManager.getInstance().getSecurityToken());
		volumesClient.startMigration(volume.getName(), sourceDisk.getQualifiedName(), targetDisk.getQualifiedName());
		
		return true;
	}
}
