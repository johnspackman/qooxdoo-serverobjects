package com.zenesis.qx.remote;

/**
 * This interface must be implemented by Bootstrap objects which can be uploaded to
 * by the UploadHandler
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
public interface FileApiProvider extends Proxied {

	/**
	 * Returns a FileApi instance, eg for handling uploads
	 * @return
	 */
	public FileApi getFileApi();
}
