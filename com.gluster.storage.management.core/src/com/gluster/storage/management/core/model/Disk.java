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
package com.gluster.storage.management.core.model;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.gluster.storage.management.core.utils.StringUtil;

@XmlRootElement(name="Disk")
public class Disk extends Entity {
	public enum DISK_STATUS {
		READY, UNINITIALIZED, INITIALIZING, OFFLINE
	};

	private String[] DISK_STATUS_STR = { "Ready", "Uninitialized", "Initializing", "Offline" };

	private Server server;
	private Double space;
	private Double spaceInUse;
	private DISK_STATUS status;

	public Disk() {
		
	}
	
	public Double getSpace() {
		return space;
	}

	public void setSpace(Double space) {
		this.space = space;
	}
	
	public boolean isUninitialized() {
		return getStatus() == DISK_STATUS.UNINITIALIZED;
	}
	
	public boolean isOffline() {
		return getStatus() == DISK_STATUS.OFFLINE;
	}
	
	public boolean isReady() {
		return getStatus() == DISK_STATUS.READY;
	}

	public DISK_STATUS getStatus() {
		return status;
	}

	public String getStatusStr() {
		return DISK_STATUS_STR[getStatus().ordinal()];
	}

	public void setStatus(DISK_STATUS status) {
		this.status = status;
	}

	public Double getSpaceInUse() {
		return spaceInUse;
	}

	public void setSpaceInUse(Double spaceInUse) {
		this.spaceInUse = spaceInUse;
	}

	public Server getServer() {
		return server;
	}

	@XmlTransient
	public void setServer(Server server) {
		this.server = server;
	}

	public Disk(Server server, String name, Double space, Double spaceInUse, DISK_STATUS status) {
		super(name, server);
		setServer(server);
		setSpace(space);
		setSpaceInUse(spaceInUse);
		setStatus(status);
	}

	@Override
	public boolean filter(String filterString, boolean caseSensitive) {
		return StringUtil.filterString(getServer().getName() + getName() + getStatusStr(), filterString, caseSensitive);
	}
	
	public String getQualifiedName() {
		return getServer().getName() + ":" + getName();
	}
}
