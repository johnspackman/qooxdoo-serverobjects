package com.zenesis.qx.remote;

/**
 * Implemented by bootstrap objects to be notified about (and change) uploaded
 * files
 * 
 * @author john
 *
 */
public interface UploadInterceptor {

  public void interceptUpload(UploadingFile upfile);
}
