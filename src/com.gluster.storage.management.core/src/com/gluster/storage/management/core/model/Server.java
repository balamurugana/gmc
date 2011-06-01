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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.gluster.storage.management.core.utils.StringUtil;

@XmlRootElement(name = "server")
public class Server extends Entity {
	private List<NetworkInterface> networkInterfaces = new ArrayList<NetworkInterface>();
	private int numOfCPUs;
	private double cpuUsage;
	private double totalMemory;
	private double memoryInUse;
	private double totalDiskSpace = 0;
	private double diskSpaceInUse = 0;
	private List<Disk> disks = new ArrayList<Disk>();

	public Server() {

	}

	public Server(String name) {
		super(name, null);
	}

	public Server(String name, Entity parent, int numOfCPUs, double cpuUsage, double totalMemory, double memoryInUse) {
		super(name, parent);
		setNumOfCPUs(numOfCPUs);
		setCpuUsage(cpuUsage);
		setTotalMemory(totalMemory);
		setMemoryInUse(memoryInUse);
	}

	public int getNumOfCPUs() {
		return numOfCPUs;
	}

	public void setNumOfCPUs(int numOfCPUs) {
		this.numOfCPUs = numOfCPUs;
	}

	public double getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public double getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(double totalMemory) {
		this.totalMemory = totalMemory;
	}

	public double getMemoryInUse() {
		return memoryInUse;
	}

	public void setMemoryInUse(double memoryInUse) {
		this.memoryInUse = memoryInUse;
	}

	public double getTotalDiskSpace() {
		return totalDiskSpace;
	}

	/**
	 * Total disk space is automatically calculated, and hence this method should never be called. It is required only
	 * to make sure that the element "totalDiskSpace" gets added to the XML tag when jersey converts the server object
	 * to XML for sending to client.
	 */
	public void setTotalDiskSpace(double totalDiskSpace) {
		this.totalDiskSpace = totalDiskSpace;
	}

	public double getDiskSpaceInUse() {
		return diskSpaceInUse;
	}
	
	public double getFreeDiskSpace() {
		return getTotalDiskSpace() - getDiskSpaceInUse();
	}
	
	/**
	 * Total disk space in use is automatically calculated, and hence this method should never be called. It is required
	 * only to make sure that the element "diskSpaceInUse" gets added to the XML tag when jersey converts the server
	 * object to XML for sending to client.
	 */
	public void setDiskSpaceInUse(double diskSpaceInUse) {
		this.diskSpaceInUse = diskSpaceInUse;
	}

	@XmlElementWrapper(name = "networkInterfaces")
	@XmlElement(name = "networkInterface", type = NetworkInterface.class)
	public List<NetworkInterface> getNetworkInterfaces() {
		return networkInterfaces;
	}

	public void setNetworkInterfaces(List<NetworkInterface> networkInterfaces) {
		this.networkInterfaces = networkInterfaces;
	}

	@XmlElementWrapper(name = "disks")
	@XmlElement(name = "disk", type = Disk.class)
	public List<Disk> getDisks() {
		return disks;
	}

	public void addNetworkInterface(NetworkInterface networkInterface) {
		networkInterfaces.add(networkInterface);
	}

	public void addDisk(Disk disk) {
		if (disks.add(disk) && disk.isReady()) {
			totalDiskSpace += disk.getSpace();
			diskSpaceInUse += disk.getSpaceInUse();
		}
	}

	public void addDisks(List<Disk> disks) {
		for (Disk disk : disks) {
			addDisk(disk);
		}
	}

	public void removeDisk(Disk disk) {
		if (disks.remove(disk)) {
			totalDiskSpace -= disk.getSpace();
			diskSpaceInUse -= disk.getSpaceInUse();
		}
	}

	public void removeAllDisks() {
		disks.clear();
		totalDiskSpace = 0;
		diskSpaceInUse = 0;
	}

	public void setDisks(List<Disk> disks) {
		removeAllDisks();
		addDisks(disks);
	}

	public int getNumOfDisks() {
		return disks.size();
	}

	public String getIpAddressesAsString() {
		String ipAddresses = "";
		for (NetworkInterface networkInterface : getNetworkInterfaces()) {
			String ipAddr = networkInterface.getIpAddress();
			ipAddresses += (ipAddresses.isEmpty() ? ipAddr : ", " + ipAddr);
		}
		return ipAddresses;
	}

	@Override
	public boolean filter(String filterString, boolean caseSensitive) {
		return StringUtil.filterString(getName() + getIpAddressesAsString(), filterString, caseSensitive);
	}
	
	@SuppressWarnings("unchecked")
	public void copyFrom(Server server) {
		this.setName(server.getName());
		this.setParent(server.getParent());
		this.setChildren(( List<Entity>) server.getChildren());
		this.setNetworkInterfaces(server.getNetworkInterfaces());
		this.setNumOfCPUs(server.getNumOfCPUs());
		this.setCpuUsage(server.getCpuUsage());
		this.setTotalMemory(server.getTotalMemory());
		this.setMemoryInUse(server.getMemoryInUse());
		this.setTotalDiskSpace(server.getTotalDiskSpace());
		this.setDiskSpaceInUse(server.getDiskSpaceInUse());
		this.setDisks(server.getDisks());
	}
}
