package de.htw.ds.grid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.WebServiceException;
import de.sb.javase.TypeMetadata;


/**
 * <p>Instances of this POJO class model grid tasks. Such tasks
 * are usually forked within grid containers, and their threads
 * usually possess an extended class loader in order to load
 * agent classes as a plug-in. Note that while tasks are based on
 * jar and data files, these are marshaled as byte arrays (i.e.
 * their content) to allow tasks to be passed over OS instance
 * boundaries. Also note that maps must be represented as arrays
 * of map entries during marshaling.</p>
 */

@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GridTaskSkeleton implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;
	private static final Random RANDOMIZER = new Random();
	private static final Map<String,String> EMPTY_PROPERTIES = Collections.emptyMap();

	private static final String LOG_TASK_PROCESS_START = "Task %d, thread %s: Execution started for agent class {0}.";
	private static final String LOG_TASK_AGENT_OK = "Task %d, thread %s: Agent creation successful, processing task.";
	private static final String LOG_TASK_PROCESS_OK = "Task %d, thread %s: Task processing successful, responding.";
	private static final String LOG_TASK_RESPOND_OK = "Task %d, thread %s: Response transmitted, execution complete.";
	private static final String LOG_TASK_RESPOND_ABORT = "Task %d, thread %s: Response discarded after {0} return attempts";
	private static final String LOG_TASK_ERROR = "Task %d, thread %s: Aborted because of persistent problem.";

	private final long identity;
	private final URI responseURI;
	private final String agentClassName;
	private final Path jarPath;
	private final Path dataPath;
	private final Map<String,String> properties;
	private final int tryCount;


	/**
	 * Creates a new uninitialized instance, which is
	 * required for XML marshaling.
	 */
	protected GridTaskSkeleton() {
		this.identity = 0;
		this.responseURI = null;
		this.agentClassName = null;
		this.jarPath = null;
		this.dataPath = null;
		this.properties = null;
		this.tryCount = 0;
	}


	/**
	 * Creates a new instance with a generated identity that is sufficiently
	 * unique across multiple systems.
	 * @param responseURI the response URI
	 * @param agentClassName the agent class name
	 * @param jarPath the path to the jar-file containing the agent's code,
	 *    or <tt>null</tt>
	 * @param dataPath the path to the data-file to be consumed by the agent,
	 *    or <tt>null</tt>
	 * @param properties the properties to be consumed by the agent,
	 *    or <tt>null</tt>
	 * @throws NullPointerException if the response URI or the agent class
	 *    is <tt>null</tt>
	 * @param tryCount the maximum number of times a grid container will try
	 *    to issue requests or responses for this task
	 * @throws IllegalArgumentException if the given response URI is not
	 *    absolute, or if the given try count is negative
	 * @throws NoSuchFileException if any of the given file paths is not
	 *    <tt>null</tt>, but doesn't point to a regular file
	 * @throws AccessDeniedException if any of the given file paths is not
	 *    <tt>null</tt>, but the file cannot be read
	 */
	public GridTaskSkeleton(final URI responseURI, final String agentClassName, final Path jarPath, final Path dataPath, final Map<String,String> properties, final int tryCount) throws NoSuchFileException, AccessDeniedException {
		super();
		if (agentClassName == null) throw new NullPointerException();
		if (!responseURI.isAbsolute() | tryCount < 1) throw new IllegalArgumentException();

		for (final Path path : new Path[] {jarPath, dataPath}) {
			if (path != null) {
				if (!Files.isRegularFile(path)) throw new NoSuchFileException(path.toString());
				if (!Files.isReadable(path)) throw new AccessDeniedException(path.toString());
			}
		}

		this.identity = new BigInteger(63, RANDOMIZER).longValue();
		this.responseURI = responseURI;
		this.agentClassName = agentClassName;
		this.jarPath = jarPath;
		this.dataPath = dataPath;
		this.properties = properties;
		this.tryCount = tryCount;
	}


	/**
	 * Returns this task's identity.
	 * @return the identity
	 */
	public long getIdentity() {
		return this.identity;
	}


	/**
	 * Returns this task's response URI.
	 * @return the response URI
	 */
	public URI getResponseURI() {
		return this.responseURI;
	}


	/**
	 * Returns this task's agent class name
	 * @return the agent class name
	 */
	public String getAgentClassName() {
		return this.agentClassName;
	}


	/**
	 * Returns the path to the jar-file containing the agent's code.
	 * @return the jar path, or <tt>null</tt>
	 */
	public Path getJarPath() {
		return this.jarPath;
	}


	/**
	 * Returns the path to the data-file to be consumed by the agent.
	 * @return the data path, or <tt>null</tt>
	 */
	public Path getDataPath() {
		return this.dataPath;
	}


	/**
	 * Returns the properties to be consumed by the agent.
	 * @return the properties, or <tt>null</tt>
	 */
	public Map<String,String> getProperties() {
		return this.properties;
	}


	/**
	 * Returns the the maximum number of times a task request or response
	 *    is to be sent before the action is aborted.
	 * @return the try count
	 */
	public int getTryCount() {
		return tryCount;
	}


	/**
	 * Process this task, and sends the task response back to the
	 * originating grid peer, identified by this task's response URI.</p>
	 */
	public void run() {
		final long threadIdentity = Thread.currentThread().getId();

		Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_PROCESS_START, this.identity, threadIdentity), this.agentClassName);
		try {
			final Agent agent = newAgent(this.agentClassName);
			Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_AGENT_OK, this.identity, threadIdentity));

			final GridTaskResponse response = this.process(agent);
			Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_PROCESS_OK, this.identity, threadIdentity));

			final boolean responseSuccess = this.respond(response);
			if (responseSuccess) {
				Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_RESPOND_OK, this.identity, threadIdentity));
			} else {
				Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_RESPOND_ABORT, this.identity, threadIdentity), this.tryCount);
			}
		} catch (final Exception exception) {
			Logger.getGlobal().log(Level.WARNING, String.format(LOG_TASK_ERROR, this.identity, threadIdentity), exception);
		}
	}


	/**
	 * Processes this task using the given agent instance, and returns the
	 * corresponding task response. Makes sure that the given (temporary) data
	 * file is removed after processing is done.
	 * @param agent an agent instance
	 * @return the task response
	 * @throws NullPointerException if the given agent is <tt>null</tt>
	 * @throws Exception if there is a problem processing the agent
	 */
	private GridTaskResponse process(final Agent agent) throws Exception {
		try (InputStream byteSource = this.dataPath == null ? new ByteArrayInputStream(new byte[0]) : Files.newInputStream(this.dataPath)) {
			try (ByteArrayOutputStream byteSink = new ByteArrayOutputStream()) {
				final Map<String,String> properties = this.properties == null ? EMPTY_PROPERTIES : this.properties;
				final String responseType = agent.process(byteSource, byteSink, properties);
				final byte[] responseData = byteSink.toByteArray();
				return new GridTaskResponse(this.identity, responseType, responseData);
			}
		} finally {
			if (this.dataPath != null) {
				try { Files.delete(this.dataPath); } catch (final IOException exception) {}
			}
		}
	}


	/**
	 * Tries up to this task's <tt>tryCount</tt> times to communicate the given
	 * response back to the task originator. Waits for an increasing time after
	 * each failed attempt before trying again. Returns <tt>true</tt> if a
	 * response transmission was successful, otherwise <tt>false</tt>.
	 * @param response the task response
	 * @return whether or not a response transmission was successful
	 * @throws NullPointerException if the given task response is <tt>null</tt>
	 * @throws WebServiceException if there is a general problem constructing
	 *    a service proxy
	 */
	private boolean respond(final GridTaskResponse response) {
		if (response == null) throw new NullPointerException();

		// TODO:
		// try up to this task's tryCount times to create a grid service proxy for
		// this task's response URI (see GridContainer#selectActivePeer(), and use it to
		// call #processTaskResponse(response) with the given response. After each failed
		// attempt, sleep for a duration that is double the one before (start with 15
		// seconds). Return true if one of the respond attempts succeeds, false otherwise.
		//    Note that in case of transient connectivity problems (service down, service
		// unreachable, etc) a WebServiceException is thrown that contains a root cause
		// (an exception cause that has a null cause, use Classes.rootCause(Exception))
		// to get them) of class ConnectException. Only filter these away during your
		// respond attempts, and rethrow the others because these may point to general
		// proxy construction problems!
		return false;
	}


	/**
	 * Factory for reflectively created agent instances.
	 * @param agentClassName the agent class name
	 * @return the agent
	 * @throws ClassCastException if the agent class doesn't implement
	 *    the Agent interface
	 * @throws ClassNotFoundException if the agent class cannot be loaded
	 * @throws InstantiationException if the agent class is not a public
	 *    concrete class
	 * @throws NoSuchMethodException if the agent class doesn't have a
	 *    no-arg constructor (of any visibility)
	 * @throws InvocationTargetException if the agent class throws
	 *    throws an exception during no-arg construction
	 */
	private static Agent newAgent(final String agentClassName) throws ClassCastException, ClassNotFoundException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		final Class<?> agentClass = Class.forName(agentClassName, true, Thread.currentThread().getContextClassLoader());
		try {
			final Constructor<?> constructor = agentClass.getDeclaredConstructor();
			constructor.setAccessible(true);
			return (Agent) constructor.newInstance();
		} catch (final IllegalAccessException exception) {
			throw new AssertionError();
		}
	}
}