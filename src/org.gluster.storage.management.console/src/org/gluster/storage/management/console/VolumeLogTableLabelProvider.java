/*******************************************************************************
 * Copyright (c) 2006-2011 Gluster, Inc. <http://www.gluster.com>
 * This file is part of Gluster Management Console.
 *
 * Gluster Management Console is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Gluster Management Console is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package org.gluster.storage.management.console;


import org.gluster.storage.management.console.views.pages.VolumeLogsPage.LOG_TABLE_COLUMN_INDICES;
import org.gluster.storage.management.core.model.VolumeLogMessage;
import org.gluster.storage.management.core.utils.DateUtil;


public class VolumeLogTableLabelProvider extends TableLabelProviderAdapter {
	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof VolumeLogMessage)) {
			return null;
		}

		VolumeLogMessage logMessage = (VolumeLogMessage) element;
		return (columnIndex == LOG_TABLE_COLUMN_INDICES.DATE.ordinal() ? DateUtil.formatDate(logMessage.getTimestamp()) 
			: columnIndex == LOG_TABLE_COLUMN_INDICES.TIME.ordinal() ? DateUtil.formatTime(logMessage.getTimestamp())
			: columnIndex == LOG_TABLE_COLUMN_INDICES.BRICK.ordinal() ? logMessage.getBrick()
			: columnIndex == LOG_TABLE_COLUMN_INDICES.SEVERITY.ordinal() ? "" + logMessage.getSeverity()
			: columnIndex == LOG_TABLE_COLUMN_INDICES.MESSAGE.ordinal() ? logMessage.getMessage() : "Invalid");
	}
}
