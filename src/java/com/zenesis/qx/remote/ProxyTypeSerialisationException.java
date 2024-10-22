package com.zenesis.qx.remote;

import java.io.IOException;

/**
 * Thrown when an exception is raised while serialising a type; this special
 * case is made because the type information available on the client is now
 * incomplete and potentially not recoverable
 *
 * @author john
 *
 */
public class ProxyTypeSerialisationException extends IOException {

  public ProxyTypeSerialisationException() {
    super();
  }

  public ProxyTypeSerialisationException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public ProxyTypeSerialisationException(String arg0) {
    super(arg0);
  }

  public ProxyTypeSerialisationException(Throwable arg0) {
    super(arg0);
  }

}
