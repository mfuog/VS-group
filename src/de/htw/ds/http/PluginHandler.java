package de.htw.ds.http;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.ClassFileLoader;


/**
 * <p> HTTP request handler capable of reflectively loading a plugin handler
 * class from an extended class path, instantiating it using the Java
 * Reflection API, and delegating the service message to said instance.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class PluginHandler implements HttpRequestHandler {
	private final String handlerClassName;
	private final Path handlerClassPath;


	/**
	 * Public constructor.
	 * @param handlerClassPath the directory containing the plugin handler's class file
	 * @param handlerClassName the plugin handler's class name
	 * @throws NullPointerException if one of the given arguments is <tt>null</tt>
	 */
	public PluginHandler(final Path handlerClassPath, final String handlerClassName) {
		super();
		if (handlerClassPath == null | handlerClassName == null) throw new NullPointerException();

		this.handlerClassPath = handlerClassPath;
		this.handlerClassName = handlerClassName;
	}


	/**
	 * Reflectively creates an HTTP request handler delegate within a separate thread
	 * featuring an extended class path, and sends it the service-message before
	 * resynchronizing said thread. This allows the Java Reflection API to load
	 * plugin-classes whose code is unavailable during process startup, and which
	 * can therefore not be instantiated directly. This pattern additionally ensures
	 * that the plugin code can be changed while an HTTP container is running.
	 * @param context the optional server context, or <tt>null</tt>
	 * @param requestHeader the HTTP request header 
	 * @param responseHeader the HTTP response header
	 * @throws NullPointerException if one of the given headers is <tt>null</tt>
	 * @throws IllegalArgumentException if the given context is required but <tt>null</tt>
	 * @throws IOException if there's an I/O related problem
	 */
	public void service(final Context context, final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) throws IOException {
		final Callable<Object> callable = new Callable<Object>() {
			public Object call() throws IOException {
				try {
					final Class<?> handlerClass = Class.forName(PluginHandler.this.handlerClassName, true, Thread.currentThread().getContextClassLoader());
					final HttpRequestHandler handler = (HttpRequestHandler) handlerClass.newInstance();
					handler.service(context, requestHeader, responseHeader);
				} catch (final ClassNotFoundException | IllegalAccessException | InstantiationException exception) {
					responseHeader.setType(HttpResponseHeader.Type.NO_IMPL);
				}
				return null;
			}
		};

		final RunnableFuture<Object> future = new FutureTask<>(callable);
		final Thread thread = new Thread(future, "plugin-executor");
		thread.setContextClassLoader(new ClassFileLoader(thread.getContextClassLoader(), this.handlerClassPath));
		thread.start();

		while (true) {
			try {
				future.get();
				break;
			} catch (final InterruptedException exception) {
				// try again
			} catch (final ExecutionException exception) {
				final Throwable cause = exception.getCause();
				if (cause instanceof Error) throw (Error) cause;
				if (cause instanceof RuntimeException) throw (RuntimeException) cause;
				if (cause instanceof IOException) throw (IOException) cause;
				throw new AssertionError();
			}
		}
	}
}