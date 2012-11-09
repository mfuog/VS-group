package de.htw.ds.http;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import de.htw.ds.TypeMetadata;


/**
 * <p>This class models container context information holding scoped variables.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class Context {
	private final Map<String,Serializable> globalMap;
	private final Map<String,Serializable> sessionMap;
	private final Map<String,Serializable> requestMap;


	/**
	 * Create a new context based on the given global and session map.
	 * @param globalMap the global map
	 * @param sessionMap the session map
	 * @throws NullPointerException if one of the given values is null
	 */
	public Context(final Map<String,Serializable> globalMap, final Map<String,Serializable> sessionMap) {
		super();
		if (globalMap == null || sessionMap == null) throw new NullPointerException();

		this.globalMap = globalMap;
		this.sessionMap = sessionMap;
		this.requestMap = new HashMap<>();
	}


	/**
	 * Returns the global scope variables.
	 * @return the global scope variables as a map
	 */
	public Map<String,Serializable> getGlobalMap() {
		return this.globalMap;
	}


	/**
	 * Returns the session scope variables.
	 * @return the session scope variables as a map
	 */
	public Map<String,Serializable> getSessionMap() {
		return this.sessionMap;
	}


	/**
	 * Returns the request scope variables.
	 * @return the request scope variables as a map
	 */
	public Map<String,Serializable> getRequestMap() {
		return this.requestMap;
	}
}