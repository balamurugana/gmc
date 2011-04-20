/**
 * VolumesResource.java
 *
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
 */
package com.gluster.storage.management.server.resources;

import static com.gluster.storage.management.core.constants.RESTConstants.FORM_PARAM_OPERATION;
import static com.gluster.storage.management.core.constants.RESTConstants.FORM_PARAM_VALUE_START;
import static com.gluster.storage.management.core.constants.RESTConstants.FORM_PARAM_VALUE_STOP;
import static com.gluster.storage.management.core.constants.RESTConstants.PATH_PARAM_VOLUME_NAME;
import static com.gluster.storage.management.core.constants.RESTConstants.QUERY_PARAM_DISK_NAME;
import static com.gluster.storage.management.core.constants.RESTConstants.QUERY_PARAM_LINE_COUNT;
import static com.gluster.storage.management.core.constants.RESTConstants.RESOURCE_PATH_VOLUMES;
import static com.gluster.storage.management.core.constants.RESTConstants.SUBRESOURCE_DEFAULT_OPTIONS;
import static com.gluster.storage.management.core.constants.RESTConstants.SUBRESOURCE_LOGS;
import static com.gluster.storage.management.core.constants.RESTConstants.SUBRESOURCE_OPTIONS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.gluster.storage.management.core.constants.CoreConstants;
import com.gluster.storage.management.core.constants.RESTConstants;
import com.gluster.storage.management.core.exceptions.GlusterRuntimeException;
import com.gluster.storage.management.core.model.LogMessage;
import com.gluster.storage.management.core.model.Status;
import com.gluster.storage.management.core.model.Volume;
import com.gluster.storage.management.core.model.Volume.TRANSPORT_TYPE;
import com.gluster.storage.management.core.response.GenericResponse;
import com.gluster.storage.management.core.response.LogMessageListResponse;
import com.gluster.storage.management.core.response.VolumeListResponse;
import com.gluster.storage.management.core.response.VolumeOptionInfoListResponse;
import com.gluster.storage.management.server.constants.VolumeOptionsDefaults;
import com.gluster.storage.management.server.utils.GlusterUtil;
import com.gluster.storage.management.server.utils.ServerUtil;
import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path(RESOURCE_PATH_VOLUMES)
public class VolumesResource {
	private static final String PREPARE_BRICK_SCRIPT = "create_volume_directory.py";
	private static final String VOLUME_DIRECTORY_CLEANUP_SCRIPT = "clear_volume_directory.py";
	private static final String VOLUME_DISK_LOG_SCRIPT = "get_volume_disk_log.py";

	@InjectParam
	private static ServerUtil serverUtil;
	private final GlusterUtil glusterUtil = new GlusterUtil();

	@InjectParam
	private VolumeOptionsDefaults volumeOptionsDefaults;

	@GET
	@Produces(MediaType.TEXT_XML)
	public VolumeListResponse getAllVolumes() {
		try {
			return new VolumeListResponse(Status.STATUS_SUCCESS, glusterUtil.getAllVolumes());
		} catch (Exception e) {
			// TODO: log the error
			e.printStackTrace();
			return new VolumeListResponse(new Status(Status.STATUS_CODE_FAILURE, e.getMessage()), null);
		}
	}

	@POST
	@Consumes(MediaType.TEXT_XML)
	@Produces(MediaType.TEXT_XML)
	public Status createVolume(Volume volume) {
		// Create the directories for the volume
		List<String> disks = volume.getDisks();
		Status status = createDirectories(disks, volume.getName());
		if (status.isSuccess()) {
			List<String> bricks = Arrays.asList(status.getMessage().split(" "));
			status = glusterUtil.createVolume(volume, bricks);
			if (status.isSuccess()) {
				Status optionsStatus = glusterUtil.createOptions(volume);
				if(!optionsStatus.isSuccess()) {
					status.setCode(Status.STATUS_CODE_PART_SUCCESS);
					status.setMessage("Error while setting volume options: " + optionsStatus);
				}
			} else {
				Status cleanupStatus = cleanupDirectories(disks, volume.getName(), disks.size());
				if(!cleanupStatus.isSuccess()) {
					status.setMessage(status.getMessage() + CoreConstants.NEWLINE + "Cleanup errors: "
							+ CoreConstants.NEWLINE + cleanupStatus);
				}
			}
		}
		return status;
	}

	@GET
	@Path("{" + PATH_PARAM_VOLUME_NAME + "}")
	@Produces(MediaType.TEXT_XML)
	public Volume getVolume(@PathParam(PATH_PARAM_VOLUME_NAME) String volumeName) {
		return glusterUtil.getVolume(volumeName);
	}

	@PUT
	@Path("{" + PATH_PARAM_VOLUME_NAME + "}")
	@Produces(MediaType.TEXT_XML)
	public Status performOperation(@FormParam(FORM_PARAM_OPERATION) String operation,
			@PathParam(PATH_PARAM_VOLUME_NAME) String volumeName) {

		if (operation.equals(FORM_PARAM_VALUE_START)) {
			return glusterUtil.startVolume(volumeName);
		}
		if (operation.equals(FORM_PARAM_VALUE_STOP)) {
			return glusterUtil.stopVolume(volumeName);
		}
		return new Status(Status.STATUS_CODE_FAILURE, "Invalid operation code [" + operation + "]");
	}

	@POST
	@Path("{" + PATH_PARAM_VOLUME_NAME + " }/" + SUBRESOURCE_OPTIONS)
	@Produces(MediaType.TEXT_XML)
	public Status setOption(@PathParam(PATH_PARAM_VOLUME_NAME) String volumeName,
			@FormParam(RESTConstants.FORM_PARAM_OPTION_KEY) String key,
			@FormParam(RESTConstants.FORM_PARAM_OPTION_VALUE) String value) {
		return glusterUtil.setOption(volumeName, key, value);
	}

	@PUT
	@Path("{" + PATH_PARAM_VOLUME_NAME + " }/" + SUBRESOURCE_OPTIONS)
	@Produces(MediaType.TEXT_XML)
	public Status resetOptions(@PathParam(PATH_PARAM_VOLUME_NAME) String volumeName) {
		return glusterUtil.resetOptions(volumeName);
	}

	@GET
	@Path(SUBRESOURCE_DEFAULT_OPTIONS)
	@Produces(MediaType.TEXT_XML)
	public VolumeOptionInfoListResponse getDefaultOptions() {
		// TODO: Fetch all volume options with their default values from GlusterFS
		// whenever such a CLI command is made available in GlusterFS
		return new VolumeOptionInfoListResponse(Status.STATUS_SUCCESS, volumeOptionsDefaults.getDefaults());
	}

	@SuppressWarnings("rawtypes")
	private Status prepareBrick(String serverName, String diskName, String volumeName) {
		return (Status) ((GenericResponse) serverUtil.executeOnServer(true, serverName, PREPARE_BRICK_SCRIPT + " "
				+ diskName + " " + volumeName, GenericResponse.class)).getStatus();
	}

	private Status createDirectories(List<String> disks, String volumeName) {
		List<String> bricks = new ArrayList<String>();
		Status status = null;
		for (int i = 0; i < disks.size(); i++) {
			String disk = disks.get(i);

			String[] diskParts = disk.split(":"); 
			String serverName = diskParts[0];
			String diskName = diskParts[1];
			
			status = prepareBrick(serverName, diskName, volumeName);
			if (status.isSuccess()) {
				String brickDir =  status.getMessage().trim().replace(CoreConstants.NEWLINE, "");
				bricks.add(serverName + ":" + brickDir);
			} else {
				// Brick preparation failed. Cleanup directories already created and return failure status
				Status cleanupStatus = cleanupDirectories(disks, volumeName, i + 1);
				if (!cleanupStatus.isSuccess()) {
					// append cleanup error to prepare brick error
					status.setMessage(status.getMessage() + CoreConstants.NEWLINE + status.getMessage());
				}
				return status;
			}
		}
		status.setMessage(bricksAsString(bricks));
		return status;
	}

	private String bricksAsString(List<String> bricks) {
		String bricksStr = "";
		for (String brickInfo : bricks) {
			bricksStr += brickInfo + " ";
		}
		return bricksStr.trim();
	}
	
	private Status cleanupDirectories(List<String> disks, String volumeName, int maxIndex) {
		String serverName, diskName, diskInfo[];
		Status result;
		for (int i = 0; i < maxIndex; i++) {
			diskInfo = disks.get(i).split(":");
			serverName = diskInfo[0];
			diskName = diskInfo[1];
			result = (Status) serverUtil.executeOnServer(true, serverName, VOLUME_DIRECTORY_CLEANUP_SCRIPT + " "
					+ diskName + " " + volumeName, Status.class);
			if (!result.isSuccess()) {
				return result;
			}
		}
		return new Status(Status.STATUS_CODE_SUCCESS, "Directories cleaned up successfully!");
	}
	
	private List<LogMessage> getDiskLogs(String volumeName, String diskName, Integer lineCount)
			throws GlusterRuntimeException {
		String[] diskParts = diskName.split(":"); 
		String server = diskParts[0];
		String disk = diskParts[1];
				
		// Usage: get_volume_disk_log.py <volumeName> <diskName> <lineCount>
		Status logStatus = (Status) serverUtil.executeOnServer(true, server, VOLUME_DISK_LOG_SCRIPT + " " + volumeName
				+ " " + disk + " " + lineCount, Status.class);
		if(!logStatus.isSuccess()) {
			throw new GlusterRuntimeException(logStatus.toString());
		}
		
		return extractLogMessages(logStatus.getMessage());
	}

	private List<LogMessage> extractLogMessages(String logContent) {
		List<LogMessage> logMessages = new ArrayList<LogMessage>();
		for(String logMessage : logContent.split(CoreConstants.NEWLINE)) {
			logMessages.add(new LogMessage(logMessage));
		}
		
		return logMessages;
	}
	
	@GET
	@Path("{" + PATH_PARAM_VOLUME_NAME + "}/" + SUBRESOURCE_LOGS)
	public LogMessageListResponse getLogs(@PathParam(PATH_PARAM_VOLUME_NAME) String volumeName,
			@QueryParam(QUERY_PARAM_DISK_NAME) String diskName, @QueryParam(QUERY_PARAM_LINE_COUNT) Integer lineCount) {
		List<LogMessage> logMessages = null;
		
		try {
			if (diskName == null || diskName.isEmpty()) {
				logMessages = new ArrayList<LogMessage>();
				// fetch logs for every brick of the volume
				Volume volume = getVolume(volumeName);
				for (String volumeDisk : volume.getDisks()) {
					logMessages.addAll(getDiskLogs(volumeName, volumeDisk, lineCount));
				}
			} else {
				// fetch logs for given brick of the volume
				logMessages = getDiskLogs(volumeName, diskName, lineCount);
			}
		} catch (Exception e) {
			return new LogMessageListResponse(new Status(e), null);
		}
		
		return new LogMessageListResponse(Status.STATUS_SUCCESS, logMessages);
	}

	public static void main(String[] args) {
		VolumesResource vr = new VolumesResource();
//		VolumeListResponse response = vr.getAllVolumes();
//		for (Volume volume : response.getVolumes()) {
//			System.out.println("\nName:" + volume.getName() + "\nType: " + volume.getVolumeTypeStr() + "\nStatus: "
//					+ volume.getStatusStr());
//		}
		Volume volume = new Volume();
		volume.setName("vol3");
		volume.setTransportType(TRANSPORT_TYPE.ETHERNET);
		List<String> disks = new ArrayList<String>();
		disks.add("192.168.1.210:sdb");
		volume.addDisks(disks);
		volume.setAccessControlList("192.168.*");
		Status status = vr.createVolume(volume);
		System.out.println(status.getMessage());
	}
}
