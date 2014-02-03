/**
 * ************************************************************************
 * 
 *    server-objects - a contrib to the Qooxdoo project that makes server 
 *    and client objects operate seamlessly; like Qooxdoo, server objects 
 *    have properties, events, and methods all of which can be access from
 *    either server or client, regardless of where the original object was
 *    created.
 * 
 *    http://qooxdoo.org
 * 
 *    Copyright:
 *      2010 Zenesis Limited, http://www.zenesis.com
 * 
 *    License:
 *      LGPL: http://www.gnu.org/licenses/lgpl.html
 *      EPL: http://www.eclipse.org/org/documents/epl-v10.php
 *      
 *      This software is provided under the same licensing terms as Qooxdoo,
 *      please see the LICENSE file in the Qooxdoo project's top-level directory 
 *      for details.
 * 
 *    Authors:
 *      * John Spackman (john.spackman@zenesis.com)
 * 
 * ************************************************************************
 */
package com.zenesis.qx.remote;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;
import com.zenesis.qx.remote.CommandId.CommandType;
import com.zenesis.qx.remote.FileApi.FileInfo;
import com.zenesis.qx.remote.RequestHandler.ExceptionDetails;
import com.zenesis.qx.remote.RequestHandler.FunctionReturn;

/**
 * Handles file uploads and attaches them to a ProxySessionTracker.  NOTE:: The ProxySessionTracker.getBootstrapObject()
 * MUST BE an instance of FileApiProvider
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
public class UploadHandler {
	
	private static final Logger log = Logger.getLogger(UploadHandler.class);

	public static final int MAX_UPLOAD_SIZE = 1024 * 1024 * 50; 	// Default max size of uploads, 50Mb
	public static final String DEFAULT_ENCODING = "ISO-8859-1";		// Default encoding for parameters

	// Number of uploads received to date - used for a unique upload ID if none is given with the file
	private static int s_numberOfUploads;
	
	// The session tracker
	private final ProxySessionTracker tracker;
	
	// Maximum overall upload size, in bytes
	private long maxUploadSize = MAX_UPLOAD_SIZE;
	
	// String encoding for parameters
	@SuppressWarnings("unused")
	private String encoding = DEFAULT_ENCODING;
	
	/**
	 * @param tracker
	 */
	public UploadHandler(ProxySessionTracker tracker) {
		super();
		this.tracker = tracker;
	}
	
	/**
	 * Handles the upload
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	public void processUpload(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || (!contentType.startsWith("multipart/form-data") && !contentType.equals("application/octet-stream"))) {
        	log.error("Unsuitable content type: " + contentType);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Unsuitable content type " + contentType);
        	return;
        }
        if (!request.getMethod().equals("POST")) {
        	log.error("Unsuitable method: " + request.getMethod());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST, "Unsuitable method: " + request.getMethod());
        	return;
        }
		if (maxUploadSize > -1 && request.getContentLength() > maxUploadSize) {
        	log.error("Upload is too big: " + request.getContentLength() + " exceeds " + maxUploadSize);
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE, "Upload is too big: " + request.getContentLength() + " exceeds " + maxUploadSize);
        	return;
		}
		response.setContentType("text/html; charset=utf-8");
        
		// Accumulate POST parameters
		HashMap<String, Object> params = new HashMap<String, Object>();
		
	    // Get query parameters
		if (request.getQueryString() != null)
			parseQuery(params, request.getQueryString());
		
		try {
			if (contentType.equals("application/octet-stream"))
				receiveOctetStream(request);
			else
				receiveMultipart(request, response, params);
            //response.getWriter().print("{ \"success\": true }");
    		//response.setStatus(HttpServletResponse.SC_OK);
		}catch(IOException e) {
            log.error("Exception during upload: " + e.getMessage(), e);
            //response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            //response.getWriter().print("{ \"success\": false }");
    		tracker.getQueue().queueCommand(CommandType.EXCEPTION, null, null, new ExceptionDetails(e.getClass().getName(), e.getMessage()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
		if (tracker.hasDataToFlush())	
    		tracker.getObjectMapper().writeValue(response.getWriter(), tracker.getQueue());
	}
	
	/**
	 * Receives an uploaded file from an application/octet-stream (ie Ajax upload)
	 * @param request
	 * @throws IOException
	 */
    protected void receiveOctetStream(HttpServletRequest request) throws IOException {
    	FileApi api = getFileApi();
    	Object tmp = request.getParameter("apiServerObjectId");
    	if (tmp != null && tmp instanceof FileApi)
    		api = (FileApi)tmp;
        String filename = request.getHeader("X-File-Name");
        int pos = filename.lastIndexOf('/');
        if (pos > -1)
        	filename = filename.substring(pos + 1);
		File file = ProxyManager.getInstance().createTemporaryFile(filename);
		UploadingFile uploading = new UploadingFile("0", file, filename, Collections.EMPTY_MAP);
        ArrayList<FileApi.FileInfo> files = new ArrayList<FileApi.FileInfo>();
        file = receiveFile(api, request.getInputStream(), uploading);
        FileApi.FileInfo info = api.getFileInfo(file);
        if (info != null)
        	files.add(info);
		tracker.getQueue().queueCommand(CommandId.CommandType.UPLOAD, null, null, files);
    }
    
    /**
     * Receives uploaded file(s) from a multi-part request
     * @param request
     * @param response
     * @param params
     * @throws IOException
     */
    protected void receiveMultipart(HttpServletRequest request, HttpServletResponse response, HashMap<String, Object> params) throws IOException {
        ArrayList<FileApi.FileInfo> files = new ArrayList<FileApi.FileInfo>();
        
		MultipartParser parser = new MultipartParser(request, Integer.MAX_VALUE, true, true, null);
		FileApi api = getFileApi();
		Part part;
		while ((part = parser.readNextPart()) != null) {
			String name = part.getName();
			
			if (part.isParam()) {
				ParamPart paramPart = (ParamPart) part;
				Object value = paramPart.getStringValue();
				
				/*
				 * TODO/HACK
				 * This is a quick hack for getting server objects passed as parameters
				 * 
				 */
				if (value != null) {
					try {
						int serverId = Integer.parseInt((String)value);
						if (serverId > -1)
							value = tracker.getProxied(serverId);
					} catch(IllegalArgumentException e) {
						if (value instanceof String)
							value = URLDecoder.decode((String)value, "utf-8");				
					}
				}
				if (name.equals("apiServerObjectId") && value instanceof FileApi)
					api = (FileApi)value;
				params.put(name, value);
				
			} else {
				FilePart filePart = (FilePart) part;
				String fileName = filePart.getFileName();
				if (fileName == null || fileName.trim().length() == 0)
					fileName = "unnamed-upload";
				filePart.setRenamePolicy(null);
				File file = ProxyManager.getInstance().createTemporaryFile(fileName);
				String uploadId = (String)params.get("uploadId");
				s_numberOfUploads++;
				if (uploadId == null)
					uploadId = "__UPLOAD_ID_" + s_numberOfUploads;
				
				log.info("Starting receive of " + file.getAbsolutePath());
				UploadingFile uploading = new UploadingFile(uploadId, file, fileName, params);
				
				File receivedFile = receiveFile(api, filePart.getInputStream(), uploading);
				FileInfo info = api.getFileInfo(receivedFile);
				if (info != null) {
					files.add(info);
					info.uploadId = uploadId;
				}
			}
		}
		tracker.getQueue().queueCommand(CommandId.CommandType.UPLOAD, null, null, files);
	}
    
    /**
     * Called to handle the uploading of the file and passing it to the FileApi 
     * @param is
     * @param uploading
     * @return
     * @throws IOException
     */
    protected File receiveFile(FileApi api, InputStream is, UploadingFile uploading) throws IOException {
		api.beginUploadingFile(uploading);
		
		FileOutputStream os = null;
		File file = uploading.getFile();
		try {
			os = new FileOutputStream(file);
			byte[] buffer = new byte[32 * 1024];
			int length;
			while ((length = is.read(buffer)) > -1) {
				uploading.addBytesUploaded(length);
				os.write(buffer, 0, length);
			}
			is.close();
			is = null;
			os.close();
			os = null;
			log.info("Receive complete");
			return api.endUploadingFile(uploading, true);
		}catch(IOException e) {
			log.error("Failed to upload " + file.getName() + ": " + e.getMessage(), e);
			
			api.endUploadingFile(uploading, false);
			file.delete();
			if (is != null)
				try {
					is.close();
				} catch(IOException e2) {
					
				}
			if (os != null)
				try {
					os.close();
				} catch(IOException e2) {
					
				}
			throw e;
		}
    	
    }
    
    /**
     * Returns the FileApi instance to use
     * @return
     */
    protected FileApi getFileApi() {
        FileApi api = ((FileApiProvider)tracker.getBootstrap()).getFileApi();
        return api;
    }
    
    /**
     * Helper function which converts a query string into a map of
     * parameters
     * @param query
     * @return
     */
    private static void parseQuery(HashMap<String, Object> params, String query) {
    	if (query == null || query.length() == 0)
    		return;
		StringTokenizer st = new StringTokenizer(query, "&");
		while (st.hasMoreTokens()) {
			String name = st.nextToken();
			String value = "";
			int pos = name.indexOf('=');
			if (pos > -1) {
				value = name.substring(pos + 1);
				name = name.substring(0, pos);
			}
			
			try {
				value = URLDecoder.decode(value, "utf-8");
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
			params.put(name, value);
		}
    }
}
