package de.htw.ds.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import de.sb.javase.util.CallbackMap;


/**
 * <p>Concrete class modeling dynamic resource flavors for use within connection
 * handlers. Such flavors support both static and dynamic resources, and
 * extended scope variables.</p>
 */
public final class HttpDynamicResourceFlavor implements HttpServer.ResourceFlavor {
	private static final byte[] EMPTY_BUFFER = new byte[0];
	private static final Random RANDOMIZER = new Random();
	private static final String SESSION_IDENTITY_KEY = "JSESSIONID";
	private static final String SOURCE_FILE_EXTENSION = ".java";
	private static final String COMPILE_FILE_EXTENSION = ".class";
	private static final Map<String,Map<String,Serializable>> SESSION_MAPS = Collections.synchronizedMap(new HashMap<String,Map<String,Serializable>>());
	private static final CallbackMap<String,Serializable> GLOBAL_MAP = new CallbackMap<>(Collections.synchronizedMap(new HashMap<String,Serializable>()));


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException if any of the given headers is <tt>null</tt>
	 */
	public Context createContext(final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) {
		return new Context(GLOBAL_MAP, sessionMap(requestHeader.getCookies(), responseHeader.getCookies()));
	}


	/**
	 * {@inheritDoc}
	 */
	public HttpRequestHandler createRequestHandler(final long connectionIdentity, final Path resourcePath) {
		final String fileName = resourcePath.getFileName().toString();
		if (fileName.endsWith(SOURCE_FILE_EXTENSION)) {
			final String className = fileName.substring(0, fileName.length() - SOURCE_FILE_EXTENSION.length());
			if (!compileClass(resourcePath.getParent(), className)) {
				Logger.getGlobal().log(Level.WARNING, "Connection handler {0} failed compilation of plugin class {1}!", new Object[] { connectionIdentity, className });
			}

			return new PluginHandler(resourcePath.getParent(), className);
		} else if (fileName.endsWith(COMPILE_FILE_EXTENSION)) {
			final String className = fileName.substring(0, fileName.length() - COMPILE_FILE_EXTENSION.length());

			return new PluginHandler(resourcePath.getParent(), className);
		} else {
			return new FileHandler(resourcePath);
		}
	}


	/**
	 * Returns the global scope map.
	 * @return the global map
	 */
	protected static CallbackMap<String,Serializable> globalMap() {
		return GLOBAL_MAP;
	}


	/**
	 * Returns the session scope map for the current session, as defined in one of the given
	 * request cookies named "JSESSIONID". If there is no session yet, or if there was a
	 * session that has timed out by now, register a new unique session-ID as a response
	 * cookie, register a new session scope map under said session-ID, and return the map.
	 * @param requestCookies the cookies contained in the HTTP request
	 * @param responseCookies the cookies to be set with the HTTP response
	 * @return the session map
	 */
	private static Map<String,Serializable> sessionMap(final Map<String,Cookie> requestCookies, final Map<String,Cookie> responseCookies) {
		Cookie sessionCookie = requestCookies.get(SESSION_IDENTITY_KEY);
		synchronized(SESSION_MAPS) {
			Map<String,Serializable> sessionMap = sessionCookie == null ? null : SESSION_MAPS.get(sessionCookie.getValue());

			if (sessionMap == null) {
				String sessionIdentity;
				do {
					sessionIdentity = new BigInteger(128, RANDOMIZER).toString(16);
				} while (SESSION_MAPS.containsKey(sessionIdentity));
				SESSION_MAPS.put(sessionIdentity, sessionMap = new HashMap<>());

				responseCookies.put(SESSION_IDENTITY_KEY, new Cookie(SESSION_IDENTITY_KEY, sessionIdentity, null, null, null));
			}

			return sessionMap;
		}
	}


	/**
	 * Compiles a Java source file within the given class path.
	 * @param classPath the class path
	 * @param className the class name
	 * @return true if the compilation was successful, false otherwise
	 */
	private static boolean compileClass(final Path classPath, final String className) {
		final String basePath = className.replace(".", FileSystems.getDefault().getSeparator());
		final Path sourcePath = classPath.resolve(basePath + ".java");
		final Path targetPath = classPath.resolve(basePath + ".class");

		if (!Files.isReadable(sourcePath)) return false;
		if (Files.exists(targetPath)) {
			try {
				if (Files.getLastModifiedTime(sourcePath).compareTo(Files.getLastModifiedTime(targetPath)) < 0) return true;
			} catch (final IOException exception) {
				return false;
			}
		}

		final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
		final int returnCode = javaCompiler.run(new ByteArrayInputStream(EMPTY_BUFFER), System.out, System.err, sourcePath.toString());
		return returnCode == 0;
	}
}