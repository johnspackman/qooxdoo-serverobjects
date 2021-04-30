package com.zenesis.qx.remote;

import com.zenesis.qx.remote.annotations.Method;

/**
 * Used to show that this class can receive LogEntry's from the client
 * 
 * @author john
 *
 */
public interface LogEntrySink extends Proxied {

  @Method
  public void addLogEntries(LogEntry[] entries);
}
