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
package org.gluster.storage.management.console.views;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.gluster.storage.management.console.Application;
import org.gluster.storage.management.console.IImageKeys;
import org.gluster.storage.management.core.model.Cluster;
import org.gluster.storage.management.core.model.Entity;
import org.gluster.storage.management.core.model.EntityGroup;
import org.gluster.storage.management.core.model.GlusterDataModel;
import org.gluster.storage.management.core.model.GlusterServer;
import org.gluster.storage.management.core.model.Server;
import org.gluster.storage.management.core.model.Volume;


public class ClusterAdapterFactory implements IAdapterFactory {
	private IWorkbenchAdapter entityAdapter = new IWorkbenchAdapter() {

		@Override
		public Object getParent(Object o) {
			return ((Entity) o).getParent();
		}

		@Override
		public String getLabel(Object o) {
			return ((Entity)o).getName();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			String iconPath = null;
			
			if(object instanceof GlusterDataModel || object instanceof Cluster) {
				iconPath = IImageKeys.CLUSTER_16x16;
			}
			
			if(object instanceof EntityGroup) {
				Class<? extends Entity> entityType = ((EntityGroup) object).getEntityType(); 
				if(entityType == Volume.class) {
					iconPath = IImageKeys.VOLUMES_16x16;
				} else {
					iconPath = IImageKeys.SERVERS_16x16;
				}
			}
			
			if(object instanceof Volume) {
				iconPath = IImageKeys.VOLUME_16x16;
			}

			if(object instanceof Server || object instanceof GlusterServer) {
				iconPath = IImageKeys.SERVER_16x16;
			}
			
			return AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, iconPath);
		}

		@Override
		public Object[] getChildren(Object o) {
			return ((Entity)o).getChildren().toArray();
		}
	};

	@Override
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IWorkbenchAdapter.class) {
			if (adaptableObject instanceof Entity) {
				return entityAdapter;
			}
		}
		return null;
	}

	@Override
	public Class[] getAdapterList() {
		return new Class[] { IWorkbenchAdapter.class };
	}
}
