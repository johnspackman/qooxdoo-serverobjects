package com.zenesis.qx.remote;

import java.io.File;
import java.io.IOException;

import com.zenesis.qx.remote.annotations.DoNotProxy;

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
