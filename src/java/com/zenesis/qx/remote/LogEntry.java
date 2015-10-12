package com.zenesis.qx.remote;

import java.util.Date;

public class LogEntry {

	public Date time;
	public int offset;
	public String level;
	public String loggerName;
	public String message;
	
	@Override
	public String toString() {
		return offset + " [" + level + "] " + loggerName + " " + message;
	}
	
	
}
