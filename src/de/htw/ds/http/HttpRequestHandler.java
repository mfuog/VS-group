package de.htw.ds.http;

import java.io.IOException;
import de.htw.ds.TypeMetadata;


/**
 * <p>HTTP request handler interface, the equivalent to a CGI or a Java EE Servlet.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public interface HttpRequestHandler {

	/**
	 * Handles an HTTP request and generates an HTTP response.
	 * @param context the optional server context, or <tt>null</tt>
	 * @param requestHeader the HTTP request header 
	 * @param responseHeader the HTTP response header
	 * @throws NullPointerException if one of the given headers is <tt>null</tt>,
	 *    of if the given context is required but <tt>null</tt>
	 * @throws IOException if there's an I/O related problem
	 */
	void service(Context context, HttpRequestHeader requestHeader, HttpResponseHeader responseHeader) throws IOException;
}