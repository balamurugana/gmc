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
package com.gluster.storage.management.gui.views.details;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchSite;

import com.gluster.storage.management.core.model.Disk;
import com.gluster.storage.management.gui.ServerDiskTableLabelProvider;
import com.gluster.storage.management.gui.TableLabelProviderAdapter;

public class ServerDisksPage extends AbstractDisksPage {
	public ServerDisksPage(Composite parent, int style, IWorkbenchSite site, List<Disk> disks) {
		super(parent, style, site, disks);
	}

	public enum SERVER_DISK_TABLE_COLUMN_INDICES {
		DISK, SPACE, SPACE_IN_USE, STATUS
	};

	private static final String[] SERVER_DISK_TABLE_COLUMN_NAMES = new String[] { "Disk", "Space (GB)",
			"Space in Use (GB)", "Status" };

	@Override
	protected void setupDiskTable(Composite parent, Table table) {
		table.setHeaderVisible(true);
		table.setLinesVisible(false);

		TableColumnLayout tableColumnLayout = guiHelper.createTableColumnLayout(table, SERVER_DISK_TABLE_COLUMN_NAMES);
		parent.setLayout(tableColumnLayout);

		guiHelper.setColumnProperties(table, SERVER_DISK_TABLE_COLUMN_INDICES.DISK.ordinal(), SWT.CENTER, 100);
		guiHelper.setColumnProperties(table, SERVER_DISK_TABLE_COLUMN_INDICES.SPACE.ordinal(), SWT.CENTER, 90);
		guiHelper.setColumnProperties(table, SERVER_DISK_TABLE_COLUMN_INDICES.SPACE_IN_USE.ordinal(), SWT.CENTER, 90);
		guiHelper.setColumnProperties(table, SERVER_DISK_TABLE_COLUMN_INDICES.STATUS.ordinal(), SWT.LEFT, 90);
	}

	@Override
	protected int getStatusColumnIndex() {
		return SERVER_DISK_TABLE_COLUMN_INDICES.STATUS.ordinal();
	}

	@Override
	protected TableLabelProviderAdapter getTableLabelProvider() {
		return new ServerDiskTableLabelProvider();
	}
}