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
package com.gluster.storage.management.console.dialogs;

import java.net.URI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

import com.gluster.storage.management.client.TasksClient;
import com.gluster.storage.management.client.VolumesClient;
import com.gluster.storage.management.console.GlusterDataModelManager;
import com.gluster.storage.management.core.model.Brick;
import com.gluster.storage.management.core.model.Cluster;
import com.gluster.storage.management.core.model.TaskInfo;
import com.gluster.storage.management.core.model.TaskStatus;
import com.gluster.storage.management.core.model.Volume;

public class MigrateBrickWizard extends Wizard {
	private Volume volume;
	private Brick brick;
	private MigrateBrickPage1 page;
	private GlusterDataModelManager modelManager = GlusterDataModelManager.getInstance();
	private Cluster cluster = modelManager.getModel().getCluster();

	public MigrateBrickWizard(Volume volume, Brick brick) {
		setWindowTitle("Gluster Management Console - Migrate Brick [" + volume.getName() + "]");
		this.volume = volume;
		this.brick = brick;
		setHelpAvailable(false); // TODO: Introduce wizard help
	}

	@Override
	public void addPages() {
		page = new MigrateBrickPage1(volume, brick);
		addPage(page);
	}

	@Override
	public boolean performFinish() {

		String sourceDir = page.getSourceBrickDir();
		String targetDir = page.getTargetBrickDir();
		Boolean autoCommit = page.getAutoCommitSelection();
		VolumesClient volumesClient = new VolumesClient();
		String dialogTitle = "Brick migration";

		try {
			URI uri = volumesClient.startMigration(volume.getName(), sourceDir, targetDir, autoCommit);

			// To get the object
			TasksClient taskClient = new TasksClient();
			TaskInfo taskInfo = taskClient.getTaskInfo(uri);
			if (taskInfo != null && taskInfo instanceof TaskInfo) {
				cluster.addTaskInfo(taskInfo);
				modelManager.refreshVolumeData(cluster.getVolume(taskInfo.getReference()));
				
				// If auto commit selected and migration operation complete immediately, 
				if (taskInfo.getStatus().getCode() == TaskStatus.STATUS_CODE_SUCCESS) {
					
					String volumeName = taskInfo.getReference();
					Volume oldVolume = cluster.getVolume(volumeName);
					Volume newVolume = (new VolumesClient()).getVolume(volumeName);
					
					modelManager.volumeChanged(oldVolume, newVolume);
					
					MessageDialog.openInformation(getShell(), dialogTitle, "Brick migration completed successfully");
					return true;
				}
			} 
			MessageDialog.openInformation(getShell(), dialogTitle, "Brick migration started successfully");
			
		} catch (Exception e) {
			MessageDialog.openError(getShell(), dialogTitle, "Brick Migration failed! [" + e.getMessage() + "]");
		}
		return true;
	}
}