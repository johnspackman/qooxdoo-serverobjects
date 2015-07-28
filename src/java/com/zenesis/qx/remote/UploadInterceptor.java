package com.zenesis.qx.remote;

import java.io.File;

/**
 * Implemented by bootstrap objects to be notified about (and change) uploaded files 
 * @author john
 *
 */
public interface UploadInterceptor {

	public File interceptUpload(File file);
}
