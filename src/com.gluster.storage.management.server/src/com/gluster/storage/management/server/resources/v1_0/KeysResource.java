/**
 * KeysResource.java
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
package com.gluster.storage.management.server.resources.v1_0;

import static com.gluster.storage.management.core.constants.RESTConstants.RESOURCE_PATH_KEYS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import com.gluster.storage.management.core.exceptions.GlusterRuntimeException;
import com.gluster.storage.management.core.utils.FileUtil;
import com.gluster.storage.management.core.utils.ProcessResult;
import com.gluster.storage.management.core.utils.ProcessUtil;
import com.gluster.storage.management.server.utils.SshUtil;
import com.sun.jersey.multipart.FormDataParam;

@Path(RESOURCE_PATH_KEYS)
public class KeysResource extends AbstractResource {
	ProcessUtil processUtil = new ProcessUtil();

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response exportSshkeys() {
		try {
			StreamingOutput output = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try {
						File archiveFile = new File(createSskKeyZipFile());
						output.write(FileUtil.readFileAsByteArray(archiveFile));
						archiveFile.delete();
					} catch (Exception e) {
						output.write(("Exception while archiving SSH Key files : " + e.getMessage()).getBytes());
					}
				}
			};
			return streamingOutputResponse(output);
		} catch (Exception e) {
			return errorResponse("Exporting SSH keys failed! [" + e.getMessage() + "]");
		}
	}

	private String createSskKeyZipFile() {
		String targetDir = System.getProperty("java.io.tmpdir");
		String zipFile = targetDir + "ssh-keys.tar";
		String sourcePemFile = SshUtil.PEM_FILE.getAbsolutePath();
		String sourcePubKeyFile = SshUtil.PUBLIC_KEY_FILE.getAbsolutePath();
		String targetPemFile = targetDir + File.separator + SshUtil.PEM_FILE.getName();
		String targetPubKeyFile = targetDir + File.separator + SshUtil.PUBLIC_KEY_FILE.getName();

		// Copy keys to temp folder
		processUtil.executeCommand("cp", sourcePemFile, targetPemFile);
		processUtil.executeCommand("cp", sourcePubKeyFile, targetPubKeyFile);

		// To zip the key files
		processUtil.executeCommand("tar", "cvf", zipFile, "-C", "/tmp", SshUtil.PEM_FILE.getName(),
				SshUtil.PUBLIC_KEY_FILE.getName());

		// To remove the copied key files
		processUtil.executeCommand("rm", "-f", targetPubKeyFile, targetPubKeyFile);

		return zipFile;
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response importSshKeys(@FormDataParam("file") InputStream uploadedInputStream) {
		File uploadedFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "keys.tar");
		String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		try {
			writeToFile(uploadedInputStream, uploadedFile.getAbsolutePath());

			// To backup existing SSH pem and public keys
			if (SshUtil.PEM_FILE.isFile()) {
				if (!SshUtil.PEM_FILE.renameTo(new File(SshUtil.PEM_FILE.getAbsolutePath() + "-" + timestamp))) {
					throw new GlusterRuntimeException("Unable to backup pem key!");
				}
			}

			if (SshUtil.PUBLIC_KEY_FILE.isFile()) {
				if (!SshUtil.PUBLIC_KEY_FILE.renameTo(new File(SshUtil.PUBLIC_KEY_FILE.getAbsolutePath() + "-"
						+ timestamp))) {
					throw new GlusterRuntimeException("Unable to backup public key!");
				}
			}
			// Extract SSH pem and public key files.
			ProcessResult output = processUtil.executeCommand("tar", "xvf", uploadedFile.getName(), "-C",
					SshUtil.SSH_AUTHORIZED_KEYS_DIR);
			uploadedFile.delete();
			if (output.isSuccess()) {
				return createdResponse("SSH Key imported successfully");
			} else {
				return errorResponse(output.getOutput());
			}
		} catch (Exception e) {
			return errorResponse(e.getMessage());
		}
	}
	
	// save uploaded file to the file (with path)
	private void writeToFile(InputStream inputStream, String toFile) {
		try {
			int read = 0;
			byte[] bytes = new byte[1024];

			OutputStream out = new FileOutputStream(new File(toFile));
			while ((read = inputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new GlusterRuntimeException(e.getMessage());
		}
	}
}