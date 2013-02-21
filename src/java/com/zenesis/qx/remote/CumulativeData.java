package com.zenesis.qx.remote;

/**
 * CommandId's which are cumulative must pass data of type CumulativeData, which
 * are objects capable of merging in other CumulativeData instances.  It is used
 * to allow cumulative changes to be queued for delivery to the client.
 * 
 * @author John Spackman
 *
 */
public interface CumulativeData {

	public void merge(CumulativeData data);
}
