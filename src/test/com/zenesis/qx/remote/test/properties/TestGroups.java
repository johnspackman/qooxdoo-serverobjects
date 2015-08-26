package com.zenesis.qx.remote.test.properties;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.ProxyManager;
import com.zenesis.qx.remote.annotations.Properties;
import com.zenesis.qx.remote.annotations.Property;

@Properties
public class TestGroups implements Proxied {

	@Property 
	private String alpha = "Alpha";
	
	@Property(group="one") 
	private String bravo = "Bravo";
	
	@Property(group="one")
	private String charlie = "Charlie";
	
	@Property(group="two")
	private String delta = "Delta";
	
	@Property(group="two")
	private String echo = "Echo";
	
	public String getAlpha() {
		return alpha;
	}
	public void setAlpha(String alpha) {
		this.alpha = ProxyManager.changeProperty(this, "alpha", alpha, this.alpha);
	}
	public String getBravo() {
		return bravo;
	}
	public void setBravo(String bravo) {
		this.bravo = ProxyManager.changeProperty(this, "bravo", bravo, this.bravo);
	}
	public String getCharlie() {
		return charlie;
	}
	public void setCharlie(String charlie) {
		this.charlie = ProxyManager.changeProperty(this, "charlie", charlie, this.charlie);
	}
	public String getDelta() {
		return delta;
	}
	public void setDelta(String delta) {
		this.delta = ProxyManager.changeProperty(this, "delta", delta, this.delta);
	}
	public String getEcho() {
		return echo;
	}
	public void setEcho(String echo) {
		this.echo = ProxyManager.changeProperty(this, "echo", echo, this.echo);
	}
	
	
}
