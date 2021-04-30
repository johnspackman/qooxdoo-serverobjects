package com.zenesis.qx.remote;

import java.io.File;
import java.util.Map;

import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;

/**
 * Represents a file that is in the process of being uploaded to the server via
 * UploadHandler; the server will update this with progress by calling
 * <code>addBytesUploaded</code>.
 * 
 * Each upload is associated with a unique "upload ID" which allows the client
 * to get upload statistics.
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
@Properties({ @Property(value = "bytesUploaded", onDemand = true), @Property("uploadId") })

public class UploadingFile implements Proxied {

  // Upload ID
  private final String uploadId;

  // Parameters sent with the form POST
  private Map<String, Object> params;

  // Where the file is being uploaded to; temporary file initially
  private File file;

  // Whether the upload has been aborted
  private boolean aborted;

  // The name on the users PC
  private String originalName;

  // Number of bytes to go
  private long bytesUploaded;

  // What the URL is to download the file
  private String downloadUrl;

  /**
   * @param uploadId
   * @param file
   */
  public UploadingFile(String uploadId, File file, String originalName, Map<String, Object> params) {
    super();
    this.uploadId = uploadId;
    this.file = file;
    this.originalName = originalName;
    this.params = params;
  }

  public boolean isAborted() {
    return aborted;
  }

  public void abort() {
    aborted = true;
  }

  public FileApi.FileInfo getFileInfo(FileApi api) {
    FileApi.FileInfo info = api.getFileInfo(file);
    if (info != null && downloadUrl != null)
      info.absolutePath = downloadUrl;
    return info;
  }

  /**
   * Called to update the number of files uploaded
   * 
   * @param size
   */
  public void addBytesUploaded(int size) {
    bytesUploaded += size;
  }

  /**
   * @return the uploadId
   */
  public String getUploadId() {
    return uploadId;
  }

  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = ProxyManager.changeProperty(this, "file", file, this.file);
  }

  /**
   * @return the bytesUploaded
   */
  public long getBytesUploaded() {
    return bytesUploaded;
  }

  /**
   * @return the originalName
   */
  public String getOriginalName() {
    return originalName;
  }

  /**
   * @return the params
   */
  public Map<String, Object> getParams() {
    return params;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = ProxyManager.changeProperty(this, "downloadUrl", downloadUrl, this.downloadUrl);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return file.toString() + " " + bytesUploaded + "/" + file.length();
  }

}
