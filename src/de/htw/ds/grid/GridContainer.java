package de.htw.ds.grid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.sb.javase.Classes;
import de.sb.javase.JarFileLoader;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.xml.Namespaces;


/**
 * <p>P2P grid container executing peers and managing the communication with other grid
 * container peers, based on the JAX-WS. In their router role, instances receive requests
 * and forward them to a randomly selected peer container for execution. In their service
 * provider role, instances spawn agent threads for processing which later send the
 * processing results asynchronously back to the originator.</p>
 */
@WebService (endpointInterface="de.htw.ds.grid.GridService", serviceName="GridService", portName="GridPort")
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GridContainer implements GridService, AutoCloseable {
	private static final Random RANDOMIZER = new Random();
	
	private static final String LOG_TASK_BEGIN = "Task %d: Scheduled for maximally {0} routing attempts every {1} seconds.";
	private static final String LOG_TASK_END = "Task %d: Ended nominally by storing {0} bytes response data.";
	private static final String LOG_TASK_ROUTE_OK = "Task %d: Routed to {0} for processing, awaiting response.";
	private static final String LOG_TASK_ROUTE_PEERS = "Task %d: Currently no peers available for task routing.";
	private static final String LOG_TASK_ROUTE_ABORT = "Task %d: Aborted after {0} routing attempts.";
	private static final String LOG_TASK_ERROR = "Task %d: Aborted because of persistent problem.";

	private final Endpoint endpoint;
	private final URI localServiceURI;
	private final URI[] peerServiceURIs;
	private final Path localOutputPath;
	private final Map<Long,GridTask> tasks;
	private final ScheduledExecutorService monitorScheduler;


	/**
	 * Creates a new grid container that is published as a JAX-WS web-service
	 * under the given service port and service name.
	 * @param servicePort the local SOAP service port
	 * @param serviceName the local SOAP service name
	 * @param outputPath the local directory for the output files
	 * @param peerServiceURIs the peer service URIs
	 * @throws NullPointerException if one of the given arguments is null
	 * @throws IllegalArgumentException if the given service port is not within
	 *    range ]0, 0xFFFF], if the given service name cannot be used to
	 *    construct a valid URI, or if any of the given peer service URIs is
	 *    relative or contains a query or fragment
	 * @throws NotDirectoryException if the given output path is not a directory
	 * @throws WebServiceException if the service URI's port is already in use
	 */
	public GridContainer(final int servicePort, final String serviceName, final Path outputPath, final URI... peerServiceURIs) throws NotDirectoryException {
		super();
		if (serviceName == null) throw new NullPointerException();
		if (servicePort <= 0 ) throw new IllegalArgumentException();
		for (final URI peerServiceURI : peerServiceURIs) {
			if (!peerServiceURI.isAbsolute() | peerServiceURI.getQuery() != null | peerServiceURI.getFragment() != null) throw new IllegalArgumentException();
		}
		if (!Files.isDirectory(outputPath)) throw new NotDirectoryException(outputPath.toString());

		try {
			this.localServiceURI = new URI("http", null, SocketAddress.getLocalAddress().getCanonicalHostName(), servicePort, "/" + serviceName, null, null);
		} catch (final URISyntaxException exception) {
			throw new IllegalArgumentException();
		}

		this.localOutputPath = outputPath;
		this.peerServiceURIs = peerServiceURIs;
		this.tasks = Collections.synchronizedMap(new HashMap<Long,GridTask>());
		this.monitorScheduler = Executors.newSingleThreadScheduledExecutor();
		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.getLocalServiceURI().toASCIIString());
	}


	/**
	 * Closes this container, thereby shutting down it's monitor
	 * scheduler, and closing it's associated JAX-WS endpoint.
	 */
	public void close() {
		try { this.endpoint.stop(); } catch (final Exception exception) {}
		this.monitorScheduler.shutdown();
	}


	/**
	 * {@inheritDoc}
	 */
	public URI getLocalServiceURI() {
		return this.localServiceURI;
	}


	/**
	 * {@inheritDoc}
	 */
	public Path getLocalOutputPath() {
		return this.localOutputPath;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean hasActiveTask(final long taskIdentity) {
		return this.tasks.containsKey(taskIdentity);
	}


	/**
	 * {@inheritDoc}
	 * @throws NullPointerException {@inheritDoc}
	 * @throws NoSuchFileException {@inheritDoc}
	 * @throws AccessDeniedException {@inheritDoc}
	 */
	public long createTask(final String agentClassName, final Path jarPath, final Path dataPath, final Map<String,String> properties, int tryCount, int timeToLive) throws NoSuchFileException, AccessDeniedException {
		if (timeToLive < 1) timeToLive = 1;
		if (tryCount < 1) tryCount = 1;

		final GridTask task = new GridTask(this.localServiceURI, agentClassName, jarPath, dataPath, properties, tryCount);
		final long taskIdentity = task.getIdentity();
		this.tasks.put(taskIdentity, task);

		final Runnable taskMonitor = new Runnable() {
			private int tryIndex = -1;
			public void run() throws ThreadDeath {
				GridContainer.this.routeTask(taskIdentity, ++this.tryIndex);
				if (!GridContainer.this.hasActiveTask(taskIdentity)) throw new ThreadDeath();
			}
		};
		this.monitorScheduler.scheduleWithFixedDelay(taskMonitor, 0, timeToLive, TimeUnit.SECONDS);

		Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_BEGIN, taskIdentity), new Object[] {tryCount, timeToLive});
		return taskIdentity;
	}


	/**
	 * {@inheritDoc}
	 */
	public void processTask(final GridTask task) {
		if (task == null) return;

		final Path jarPath = task.getJarPath();
		if (jarPath == null) {
			task.run();
		} else {
			try {
				final Thread thread = new Thread(task, "grid-task");
				final ClassLoader jarClassLoader = new JarFileLoader(thread.getContextClassLoader(), jarPath, true);
				thread.setContextClassLoader(jarClassLoader);
				thread.start();
			} catch (final IOException | OutOfMemoryError exception) {
				Logger.getGlobal().log(Level.WARNING, String.format(LOG_TASK_ERROR, task.getIdentity()), exception);
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void processTaskResponse(final GridTaskResponse taskResponse) {
		if (taskResponse == null) return;

		if (this.hasActiveTask(taskResponse.getIdentity())) {
			final Path dataPath = this.localOutputPath.resolve(taskResponse.getOutputPath());
			try {
				Files.createFile(dataPath);
				Files.write(dataPath, taskResponse.getDataContent(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
				Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_END, taskResponse.getIdentity()), taskResponse.getDataContent().length);
			} catch (final FileAlreadyExistsException exception) {
				return;
			} catch (final Exception exception) {
				Logger.getGlobal().log(Level.WARNING, String.format(LOG_TASK_ERROR, taskResponse.getIdentity()), exception);
			}
			this.tasks.remove(taskResponse.getIdentity());
		}
	}


	/**
	 * Selects an active peer from the peer collection, and asks it to process
	 * the given task. Aborts the task if the given try index matches or exceeds
	 * the try count specified for the task, or if the task cannot be switched
	 * for persistent reasons. Nothing happens if the given task is inactive to
	 * begin with, or becomes so during the execution of this method.
	 * @param taskIdentity the task identity
	 * @param tryIndex the try index
	 */
	public void routeTask(final long taskIdentity, final int tryIndex) {
		final GridTask task = this.tasks.get(taskIdentity);
		if (task != null) {
			if (tryIndex < task.getTryCount()) {
				try {
					final Map.Entry<URI,GridService> peerInfo = this.selectActivePeer();
					if (this.tasks.containsKey(taskIdentity)) { // may have become inactive!
						peerInfo.getValue().processTask(task);
						Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_ROUTE_OK, taskIdentity), peerInfo.getKey());
					}
				} catch (final NoSuchElementException exception) {
					Logger.getGlobal().log(Level.INFO, String.format(LOG_TASK_ROUTE_PEERS, taskIdentity));
				} catch (final Exception exception) {
					if (!(Classes.rootCause(exception) instanceof ConnectException)) {
						this.tasks.remove(taskIdentity);
						Logger.getGlobal().log(Level.WARNING, String.format(LOG_TASK_ERROR, taskIdentity), exception);
					}
				}
			} else {
				this.tasks.remove(taskIdentity);
				Logger.getGlobal().log(Level.WARNING, String.format(LOG_TASK_ROUTE_ABORT, taskIdentity), tryIndex);
			}
		}
	}


	/**
	 * Returns the service URI and JAX-WS service proxy of a randomly selected
	 * active peer. Note that successful service proxy generation implies service
	 * availability, because it requires online HTTP access to the service's WSDL.
	 * @return the service URI and proxy of a randomly selected active peer
	 * @throws WebServiceException if there is a general problem constructing the
	 *    service proxy
	 * @throws NoSuchElementException of no active peer is available
	 */
	public Map.Entry<URI,GridService> selectActivePeer() throws NoSuchElementException {
		final List<URI> peerServiceURIs = new ArrayList<>(Arrays.asList(this.peerServiceURIs));
		while (!peerServiceURIs.isEmpty()) {
			final URI serviceURI = peerServiceURIs.remove(RANDOMIZER.nextInt(peerServiceURIs.size()));
			try {
				final URL wsdlLocator = new URL(serviceURI.toASCIIString() + "?wsdl");
				final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(GridService.class));
				final GridService proxy = proxyFactory.getPort(GridService.class);
				return new AbstractMap.SimpleImmutableEntry<>(serviceURI, proxy);
			} catch (final MalformedURLException exception) {
				throw new AssertionError(exception);
			} catch (final WebServiceException exception) {
				if (!(Classes.rootCause(exception) instanceof ConnectException)) throw exception;
			}
		}
		throw new NoSuchElementException("no active peer available.");
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * a service name, an output directory, and the service URIs of the grid container
	 * peers.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if any of the given URIs doesn't have a path,
	 *    or if the given service name cannot be used to construct a valid service URI
	 * @throws NotDirectoryException if the given output path is not a directory
	 * @throws URISyntaxException if any of the given service URIs is malformed
	 * @throws WebServiceException if the service URI's port is already in use
	 */
	public static void main (final String[] args) throws NotDirectoryException, URISyntaxException {
		final long timestamp		= System.currentTimeMillis();
		final int servicePort		= Integer.parseInt(args[0]);
		final String serviceName	= args[1];
		final Path outputPath		= Paths.get(args[2]).normalize().toAbsolutePath();

		final URI[] serviceURIs = new URI[args.length - 3];
		for (int index = 0; index < serviceURIs.length; ++index) {
			serviceURIs[index] = new URI(args[index + 3]);
		}

		try (GridContainer container = new GridContainer(servicePort, serviceName, outputPath, serviceURIs)) {
			System.out.println("JAX-WS based grid container running on one task-monitor thread, enter \"quit\" to stop.");
			System.out.format("Service URI is \"%s\".\n", container.getLocalServiceURI());
			System.out.format("Output directory is %s.\n", container.getLocalOutputPath());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);
			System.out.println(serviceURIs.length == 0 ? "No peers configured!" : "Configured peers:");
			for (final URI serviceURI : serviceURIs) {
				System.out.println(serviceURI);
			}

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}