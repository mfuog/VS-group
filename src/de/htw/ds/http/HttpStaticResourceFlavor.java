package de.htw.ds.http;

import java.nio.file.Path;


/**
 * <p>Concrete class modeling static resource flavors for use within connection
 * handlers. Such flavors support only static file resources, and no extended
 * scope variables.</p>
 */
public final class HttpStaticResourceFlavor implements HttpServer.ResourceFlavor {
	/**
	 * {@inheritDoc}
	 */
	public HttpRequestHandler createRequestHandler(final long connectionIdentity, final Path resourcePath) {
		return new FileHandler(resourcePath);
	}


	/**
	 * {@inheritDoc}
	 */
	public Context createContext(final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) {
		return null;
	}
}