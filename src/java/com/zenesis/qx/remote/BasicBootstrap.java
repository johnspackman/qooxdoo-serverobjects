package com.zenesis.qx.remote;

import com.zenesis.qx.remote.annotations.AlwaysProxy;
import com.zenesis.qx.remote.annotations.Property;
import com.zenesis.qx.remote.annotations.Remote.Toggle;

/**
 * Basic implementation of a Bootstrap object, which provides methods for
 * handling uploading. The appFilesRoot is the root folder for browsing files,
 * and is used by the FileExplorer; if this is null then files cannot be
 * browsed.
 * 
 * The uploadFolder is where files which are uploaded are placed.
 * 
 * @author "John Spackman <john.spackman@zenesis.com>"
 */
public class BasicBootstrap implements FileApiProvider {

  @Property(readOnly = Toggle.TRUE)
  private FileApi fileApi;

  /**
   * Constructor
   */
  public BasicBootstrap() {
    super();
  }

  /**
   * @param appFilesRoot
   */
  public BasicBootstrap(FileApi fileApi) {
    super();
    this.fileApi = fileApi;
  }

  /**
   * Loads proxy classes on the client; this is necessary if the client wants to
   * instantiate a class before the class definition has been loaded on demand.
   * The last part can be an asterisk if all classes in a given package should be
   * loaded
   * 
   * @param name name of the class to transfer, or array of names, or collection,
   *             etc
   */
  @AlwaysProxy
  public void loadProxyType(Object data) throws ClassNotFoundException {
    ProxyManager.loadProxyType(data);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.zenesis.qx.remote.FileApiProvider#getFileApi()
   */
  @Override
  public FileApi getFileApi() {
    return fileApi;
  }

  /**
   * @param fileApi the fileApi to set
   */
  public void setFileApi(FileApi fileApi) {
    this.fileApi = fileApi;
  }

}
