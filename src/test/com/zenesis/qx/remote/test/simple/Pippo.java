package com.zenesis.qx.remote.test.simple;

import java.util.ArrayList;

import com.zenesis.qx.remote.Proxied;
import com.zenesis.qx.remote.annotations.Method;
import com.zenesis.qx.remote.annotations.Property;

public class Pippo implements Proxied {

	@Property
	private String name;
	
	@Property(onDemand = true, arrayType = Pippo.class)
	private ArrayList<Pippo> collezioni;
	
	public Pippo() {
		collezioni = new ArrayList<Pippo>();
	}

	@Method
	public ArrayList<Pippo> getExampleCode() {
		Pippo prova1 = new Pippo();
		prova1.setName("prova1");
		Pippo prova2 = new Pippo();
		prova2.setName("prova2");
		collezioni.add(prova1);
		collezioni.add(prova2);
		return collezioni;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the collezioni
	 */
	public ArrayList<Pippo> getCollezioni() {
		return collezioni;
	}
	
	
}
