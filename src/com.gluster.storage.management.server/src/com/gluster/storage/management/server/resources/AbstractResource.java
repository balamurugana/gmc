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
package com.gluster.storage.management.server.resources;

import java.net.URI;
import java.util.ArrayList;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import com.gluster.storage.management.core.constants.RESTConstants;
import com.gluster.storage.management.core.exceptions.GlusterRuntimeException;
import com.gluster.storage.management.core.model.Volume;
import com.gluster.storage.management.core.response.VolumeListResponse;

/**
 *
 */
public class AbstractResource {
	@Context
	protected UriInfo uriInfo;

	/**
	 * Creates a response with HTTP status code of 201 (created) and sets the "location" header to the URI created using
	 * the given path relative to current path.
	 * 
	 * @param relativePath
	 *            relative path of the created resource - will be set in the "location" header of response.
	 * @return the {@link Response} object
	 */
	protected Response createdResponse(String relativePath) {
		return Response.created(createURI(relativePath)).build();
	}

	/**
	 * Creates a response with HTTP status code of 204 (no content)
	 * @return the {@link Response} object
	 */
	protected Response noContentResponse() {
		return Response.noContent().build();
	}

	/**
	 * Creates a response with HTTP status code of 204 (no content), also setting the location header to given location
	 * @param location path of the location to be set relative to current path 
	 * @return the {@link Response} object
	 */
	protected Response noContentResponse(String location) {
		return Response.noContent().location(createURI(location)).build();
	}

	protected URI createURI(String location) {
		return uriInfo.getAbsolutePathBuilder().path(location).build();
	}

	/**
	 * Creates a response with HTTP status code of 500 (internal server error) and sets the error message in the
	 * response body
	 * 
	 * @param errMessage
	 *            Error message to be set in the response body
	 * @return the {@link Response} object
	 */
	protected Response errorResponse(String errMessage) {
		return Response.serverError().type(MediaType.TEXT_HTML).entity(errMessage).build();
	}

	/**
	 * Creates a response with HTTP status code of 400 (bad request) and sets the error message in the
	 * response body
	 * 
	 * @param errMessage
	 *            Error message to be set in the response body
	 * @return the {@link Response} object
	 */
	protected Response badRequestResponse(String errMessage) {
		return Response.status(Status.BAD_REQUEST).type(MediaType.TEXT_HTML).entity(errMessage).build();
	}

	/**
	 * Creates an OK response and sets the entity in the response body.
	 * 
	 * @param entity
	 *            Entity to be set in the response body
	 * @param mediaType
	 *            Media type to be set on the response
	 * @return the {@link Response} object
	 */
	protected Response okResponse(Object entity, String mediaType) {
		return Response.ok(entity).type(mediaType).build();
	}

	/**
	 * Creates a streaming output response and sets the given streaming output in the response. Typically used for
	 * "download" requests
	 * 
	 * @param entity
	 *            Entity to be set in the response body
	 * @param mediaType
	 *            Media type to be set on the response
	 * @return the {@link Response} object
	 */
	protected Response streamingOutputResponse(StreamingOutput output) {
		return Response.ok(output).type(MediaType.APPLICATION_OCTET_STREAM).build();
	}
}