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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.sb.javase.Classes;
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
public final class GridContainerSkeleton implements GridServiceSkeleton, AutoCloseable {
	private static final Random RANDOMIZER = new Random();

	private final Endpoint endpoint;
	private final ScheduledExecutorService monitorScheduler;
	private final URI localServiceURI;
	private final URI[] peerServiceURIs;
	private final Path localOutputPath;
	private final Map<Long,GridTaskSkeleton> tasks;


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
	public GridContainerSkeleton(final int servicePort, final String serviceName, final Path outputPath, final URI... peerServiceURIs) throws NotDirectoryException {
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
		this.tasks = Collections.synchronizedMap(new HashMap<Long,GridTaskSkeleton>());
		this.monitorScheduler = Executors.newSingleThreadScheduledExecutor();
		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(this.getLocalServiceURI().toASCIIString());
	}


	/**
	 * Closes this container, thereby shutting down it's task
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
		return 0;

		// TODO:
		// - create a new task using this container's local service URI and the given
		//   parameters. The new task will have a generated identity after construction.
		// - put the new task into the container's tasks map under it's own identity,
		//   in order to register it as active.
		// - use the container's monitor scheduler to schedule a monitor runnable for
		//   immediate and then repeated (after timeToLive seconds delay) execution.
		//   The monitor runnable should have an instance variable "tryIndex" initialized
		//   to -1, and do the following every time it is run:
		//   - increment tryIndex by one.
		//   - Remove the task from the tasks map and throws a ThreadDeath() error if
		//     the task is no longer active, or tryIndex == task.getTryCount(); the error
		//     will cause the scheduler to abstain from running this monitor instance again
		//   - randomly select a peer that is online using GridContainer#selectActivePeer(),
		//     which returns the peer's service URI (for logging) and the service proxy
		//   - check again if task is still active (may have become inactive while the peer
		//     was selected
		//   - call proxy.processTask(GridTask) to marshal the task object, and route
		//     task processing to the peer; note that the marshaling WILL FAIL here if you
		//     take care of it beforehand
		//  - return the new task's identity so the grid clients can start checking when
		//    a task response becomes available
	}


	/**
	 * {@inheritDoc}
	 */
	public void processTask(final GridTaskSkeleton task) {
		if (task == null) return;

		final Path jarPath = task.getJarPath();
		if (jarPath == null) {
			task.run();
		} else {
			// TODO:
			// create a new thread for the given task. Create an instance of the
			// JarClassLoader class, passing the new thread's current context class loader
			// as parent and the task's jar-path. Then set it as the thread's new context
			// class loader in order to enlarge the thread's class path by the jar file.
			// Then start the thread, and handle any IOException (JarFileLoader creation)
			// or OutOfMemoryError (maximum native thread count reached) by simply logging
			// them to the global logger.
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void processTaskResponse(final GridTaskResponse taskResponse) {
		if (taskResponse == null) return;

		// TODO:
		// If the task is no longer active, return. Otherwise, use
		// "this.localOutputPath.resolve(taskResponse.getOutputPath())" to create
		// a unique file path for the response data within the container's output
		// directory.
		//    Create the file atomically with respect to OS operations using one of
		// the static methods in class "java.nio.file.Files"; if this fails with a
		// FileAlreadyExistsException, it is a sure sign that another thread is
		// concurrently in the process of storing the same file, likely because the
		// task was routed more than once due to a timeout, and Murphy's law ensured
		// that they both tried to respond at roughly the same time. Silently return
		// in this case to avoid writing the same response twice, or even worse
		// writing it in parallel of another thread.
		//    On the other hand, if the file could be atomically created, store the
		// response's data content in it by appending or truncating the empty file.
		// Afterwards, remove the task from the task map to indicate to the clients
		// that the response has been written to the output directory.
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
	public Map.Entry<URI,GridServiceSkeleton> selectActivePeer() throws NoSuchElementException {
		final List<URI> peerServiceURIs = new ArrayList<>(Arrays.asList(this.peerServiceURIs));
		while (!peerServiceURIs.isEmpty()) {
			final URI serviceURI = peerServiceURIs.remove(RANDOMIZER.nextInt(peerServiceURIs.size()));
			try {
				final URL wsdlLocator = new URL(serviceURI.toASCIIString() + "?wsdl");
				final Service proxyFactory = Service.create(wsdlLocator, Namespaces.toQualifiedName(GridServiceSkeleton.class));
				final GridServiceSkeleton proxy = proxyFactory.getPort(GridServiceSkeleton.class);
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

		try (GridContainerSkeleton container = new GridContainerSkeleton(servicePort, serviceName, outputPath, serviceURIs)) {
			System.out.println("JAX-WS based grid container running on one task-monitor thread, enter \"quit\" to stop.");
			System.out.format("Service URI is \"%s\".\n", container.getLocalServiceURI().toASCIIString());
			System.out.format("Output directory is %s.\n", container.getLocalOutputPath());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);
			System.out.println(serviceURIs.length == 0 ? "No peers configured!" : "Configured peers:");
			for (final URI serviceURI : serviceURIs) {
				System.out.println(serviceURI.toASCIIString());
			}

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}