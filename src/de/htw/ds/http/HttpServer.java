package de.htw.ds.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import de.sb.javase.TypeMetadata;


/**
 * <p>This class models an abstract HTTP server.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public abstract class HttpServer implements Runnable, AutoCloseable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	/**
	 * <p>Inner interface describing resource flavors. A resource flavor decides
	 * whether requests may only address static resources (HTTP Server), or optionally
	 * dynamic ones that invoke plugins (HTTP Container).</p>
	 */
	public static interface ResourceFlavor {
		/**
		 * Returns a request handler suitable for the given resource path.
		 * @param connectionIdentity the connection Identity
		 * @param resourcePath the resource path
		 * @return the request handler
		 * @throws NullPointerException if one of the given arguments is <tt>null</tt>
		 */
		HttpRequestHandler createRequestHandler(long connectionIdentity, Path resourcePath);


		/**
		 * Returns the optional variable scope context.
		 * @param requestHeader the request header
		 * @param responseHeader the response header
		 * @return the variable scope context, or <tt>null</tt>
		 * @throws NullPointerException if any of the given headers is <tt>null</tt>
		 */
		Context createContext(HttpRequestHeader requestHeader, HttpResponseHeader responseHeader);
	}


	private final ExecutorService executorService;
	private final ServerSocket serviceSocket;
	private final ResourceFlavor resourceFlavor;
	private final Path contextPath;
	private final Map<String,String> requestPathMappings;


	/**
	 * Public constructor.
	 * @param resourceFlavor the resource flavor
	 * @param servicePort the service port
	 * @param contextPath the context directory path
	 * @param requestPathMappings the request path mappings
	 * @throws NullPointerException if one of the given arguments if <tt>null</tt>
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound, or if the
	 *    given context path is not a directory
	 */
	public HttpServer(final ResourceFlavor resourceFlavor, final int servicePort, final Path contextPath, final Map<String,String> requestPathMappings) throws IOException {
		super();
		if (resourceFlavor == null | requestPathMappings == null) throw new NullPointerException();
		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());

		this.resourceFlavor = resourceFlavor;
		this.executorService = Executors.newCachedThreadPool();
		this.serviceSocket = new ServerSocket(servicePort);
		this.contextPath = contextPath;
		this.requestPathMappings = requestPathMappings;
	}


	/**
	 * Closes this server.
	 * @throws IOException if there is an I/O related problem
	 */
	public final void close() {
		try { this.serviceSocket.close(); } catch (final Exception exception) {}
		this.executorService.shutdown();
	}


	/**
	 * Returns the service port.
	 * @return the service port
	 */
	public int getServicePort() {
		return this.serviceSocket.getLocalPort();
	}


	/**
	 * Returns the resource flavor.
	 * @return the resource flavor
	 */
	protected final ResourceFlavor getResourceFlavor() {
		return this.resourceFlavor;
	}


	/**
	 * Returns the executor service.
	 * @return the executor service
	 */
	protected final ExecutorService getExecutorService() {
		return this.executorService;
	}


	/**
	 * Returns the service socket.
	 * @return the service socket
	 */
	protected final ServerSocket getServiceSocket() {
		return this.serviceSocket;
	}


	/**
	 * Returns the context path.
	 * @return the context path
	 */
	protected final Path getContextPath() {
		return this.contextPath;
	}


	/**
	 * Returns the request path mappings.
	 * @return the request path mappings
	 */
	protected final Map<String,String> getRequestPathMappings() {
		return this.requestPathMappings;
	}


	/**
	 * Prints the common parts of a welcome message to System.out.
	 * @param startupTimestamp the startup timestamp
	 */
	protected void printCommonWelcomeMessage(final long startupTimestamp) {
		System.out.format("Service port is %s.\n", this.getServicePort());
		System.out.format("Context path is %s.\n", this.contextPath);
		System.out.format("Registered request path mappings: %s\n", this.requestPathMappings);
		System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - startupTimestamp);
	}


	/**
	 * Blocks until the line "quit" is typed into the process console.
	 */
	protected final void waitForShutdownSignal() {
		final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
		try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
	}


	/**
	 * Parses request path mappings from the given arguments, starting with
	 * the element at the given offset. The mappings must be formatted as
	 * oldPath->newPath.
	 * @param args the arguments
	 * @param offset the offset of the first path mapping to be parsed
	 * @return the path mappings
	 * @throws NullPointerException if the given arguments are <tt>null</tt>
	 * @throws IllegalArgumentException if the given offset is out of range
	 */
	protected final static Map<String,String> parseRequestPathMappings(final String[] args, final int offset) {
		if (offset < 0 | offset >= args.length) throw new IllegalArgumentException();
		final Map<String,String> requestPathMappings = new HashMap<>();
		for (int index = offset; index < args.length; ++index) {
			final String[] texts = args[index].split("->", 2);
			requestPathMappings.put(texts[0].trim(), texts[1].trim());
		}
		return requestPathMappings;
	}
}