package de.htw.ds.share;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;
import de.sb.javase.util.PivotFilter;


/**
 * <p>JAX-WS based P2P file-sharing server.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class FileShareServer implements FileShareService, Runnable, AutoCloseable {
	private static final String LOG_DOWNLOAD_MONITOR_STARTED = "Download monitor started for file \"{0}\".";

	private final ScheduledExecutorService taskScheduler;
	private final SocketAddress localServiceAddress;
	private final Endpoint endpoint;

	private final PeerWatcher peerWatcher;
	private final FileSystemWatcher fileSystemWatcher;
	private final FileDescriptorDispenser fileDescriptorDispenser;
	private final Map<BigInteger,DownloadDescriptor> downloadDescriptors;


	/**
	 * Creates a new file share server that is published to the given service port.
	 * @param servicePort the service port
	 * @param uploadDirectory the upload directory path
	 * @param timePeerFilter a time based peer filter for use by peer monitor
	 * @param hashPeerFilter a hash based peer filter for use by file monitors
	 * @param peerAddresses the initial peer addresses used to bootstrap the network
	 * @throws NullPointerException if any of the given arguments is <tt>null</tt>
	 * @throws IllegalArgumentException if the given service port is not within range ]0, 0xFFFF]
	 * @throws WebServiceException if there is a problem publishing the JAX-WS service
	 * @throws IOException if there is a problem creating the file system watch service
	 */
	public FileShareServer(final int servicePort, final Path uploadDirectory, final PivotFilter<SocketAddress,Long> timePeerFilter, final PivotFilter<SocketAddress,BigInteger> hashPeerFilter, final SocketAddress... peerAddresses) throws IOException {
		super();
		if (servicePort == 0) throw new IllegalArgumentException();

		this.taskScheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
		this.localServiceAddress = new SocketAddress(SocketAddress.getLocalAddress(), servicePort);
		this.downloadDescriptors = Collections.synchronizedMap(new HashMap<BigInteger,DownloadDescriptor>());

		this.peerWatcher = new PeerWatcher(this.localServiceAddress, timePeerFilter, peerAddresses);
		this.fileSystemWatcher = new FileSystemWatcher(this.localServiceAddress, uploadDirectory);
		this.fileDescriptorDispenser = new FileDescriptorDispenser(this.taskScheduler, hashPeerFilter);
		this.taskScheduler.scheduleWithFixedDelay(this, 1, 15, TimeUnit.SECONDS);

		this.endpoint = Endpoint.create(SOAPBinding.SOAP11HTTP_BINDING, this);
		this.endpoint.publish(FileShareResources.serviceURI(this.localServiceAddress).toASCIIString());
	}


	/**
	 * Closes this container, thereby shutting down it's associated
	 * resources.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		//TODO: Aufgabe 2
	}


	/**
	 * Returns the local JSX-WS service URI.
	 * @return the local service URI
	 */
	public URI getLocalServiceURI() {
		return FileShareResources.serviceURI(this.localServiceAddress);
	}


	/**
	 * {@inheritDoc}
	 */
	public SocketAddress getLocalServiceAddress() {
		return this.localServiceAddress;
	}


	/**
	 * {@inheritDoc}
	 */
	public SocketAddress[] exchangePeerAddresses(final SocketAddress senderAddress) {
		if (senderAddress != null && !senderAddress.equals(this.localServiceAddress) && (senderAddress.isResolved() || senderAddress.resolve())) {
			this.peerWatcher.getPeerAddresses().add(senderAddress);
		}
		return this.peerWatcher.getPeerAddressSnapshot();
	}


	/**
	 * {@inheritDoc}
	 */
	public FileDescriptor addFileDescriptor(final FileDescriptor fileDescriptor) {
		// TODO: Aufgabe 2
		return fileDescriptor;
	}


	/**
	 * {@inheritDoc}
	 */
	public FileDescriptor[] queryFileDescriptors(final String[] fragments, final String[] types, final Long minSize, final Long maxSize, final long timeToLive) {
		//TODO: Aufgabe 2
		return new FileDescriptor[0];
	}


	/**
	 * {@inheritDoc}
	 */
	public FileDescriptor[] getFileDescriptors(final String[] fragments, final String[] types, Long minSize, Long maxSize) {
		final Set<FileDescriptor> result = new HashSet<FileDescriptor>();
		for (final FileDescriptor fileDescriptor : this.fileSystemWatcher.getFileDescriptorSnapshot()) {
			if (fileDescriptor.match(fragments, types, minSize, maxSize)) {
				result.add(fileDescriptor);
			}
		}
		return result.toArray(new FileDescriptor[0]);
	}


	/**
	 * {@inheritDoc}
	 */
	public byte[] getFileContent(final BigInteger contentHash, final long offset, final int length) throws IOException {
		// TODO: Aufgabe 2
		throw new NoSuchFileException(contentHash.toString()); 
	}


	/**
	 * {@inheritDoc}
	 * @throws NoSuchFileException {@inheritDoc}
	 * @throws AccessDeniedException {@inheritDoc}
	 * @throws FileSystemException {@inheritDoc}
	 * @throws IOException {@inheritDoc}
	 */
	public boolean startDownload(final BigInteger contentHash, final Path filePath) throws IOException {
		final FileDescriptor fileDescriptor = this.fileSystemWatcher.getFileDescriptors().get(contentHash);
		if (fileDescriptor == null) throw new NoSuchFileException(FileShareResources.toHexString(contentHash));

		final DownloadMonitor downloadMonitor;
		synchronized(this.downloadDescriptors) {
			if (this.downloadDescriptors.containsKey(contentHash)) return false; // already downloading this file

			final Path absolutePath = filePath.normalize().toAbsolutePath();
			if (Files.exists(absolutePath)) {
				if (Files.size(absolutePath) == fileDescriptor.getContentLength()) {
					return false; // download header has already been truncated, download is finished
				} else {
					downloadMonitor = DownloadMonitor.create(absolutePath, fileDescriptor.getSourceAddresses());
				}
			} else {
				downloadMonitor = DownloadMonitor.create(absolutePath, contentHash, fileDescriptor.getContentLength(), fileDescriptor.getSourceAddresses());
			}
			this.downloadDescriptors.put(contentHash, downloadMonitor.getDescriptor());
		}

		this.taskScheduler.scheduleWithFixedDelay(downloadMonitor, 0, 15, TimeUnit.SECONDS);
		Logger.getGlobal().log(Level.INFO, LOG_DOWNLOAD_MONITOR_STARTED, new Object[] { downloadMonitor.getDescriptor().getFilePath() });
		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean stopDownload(final BigInteger contentHash) {
		final DownloadDescriptor descriptor = this.downloadDescriptors.remove(contentHash);
		if (descriptor != null) descriptor.setClosed(true);
		return descriptor != null;
	}


	/**
	 * {@inheritDoc}
	 */
	public DownloadDescriptor[] getDownloadDescriptors() {
		//TODO: Aufgabe 2
		return new DownloadDescriptor[0];
	}


	/**
	 * Updates this server's resources periodically.
	 * @throws ClosedWatchServiceException if this server is closed
	 */
	public void run() {
		try {
			this.peerWatcher.updatePeerAddresses();
			this.fileSystemWatcher.updateFileDescriptors();
			this.fileDescriptorDispenser.dispenseFileDescriptors(this.fileSystemWatcher.getFileDescriptorSnapshot(), this.peerWatcher.getPeerAddressSnapshot());
		} catch (final RuntimeException exception) {
			if (exception instanceof ClosedWatchServiceException) throw exception;
			Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a log level, 
	 * a service port, an upload directory, followed by peer socket-addresses to
	 * be used for bootstrapping.
	 * @param args the given runtime arguments
	 * @throws IllegalPathException if the given upload directory is not a valid path
	 * @throws NotDirectoryException if the given upload directory is not a directory
	 * @throws IOException if the service port is already in use, or the
	 *    service class does not implement valid remote interfaces, or if there is an I/O
	 *    related problem creating the file system watch service
	 */
	public static void main(final String[] args) throws NotDirectoryException, IOException {
		final long timestamp = System.currentTimeMillis();
		final Level logLevel = Level.parse(args[0]);
		final int servicePort = Integer.parseInt(args[1]);
		final Path uploadDirectory = Paths.get(args[2]).normalize().toAbsolutePath();
		if (!Files.isDirectory(uploadDirectory)) throw new NotDirectoryException(uploadDirectory.toString());

		final SocketAddress[] bootstrapPeerAddresses = new SocketAddress[args.length - 3];
		for (int index = 0; index < bootstrapPeerAddresses.length; ++index) {
			bootstrapPeerAddresses[index] = new SocketAddress(args[index + 3]);
		}

		// set global logger and console handler levels to the given one
		for (final Handler handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(logLevel);	
			}
		}

		// define peer filters
		final PivotFilter<SocketAddress,Long> timePeerFilter = SocketAddressFilter.newTimestampBasedFilter(10);
		final PivotFilter<SocketAddress,BigInteger> hashPeerFilter = SocketAddressFilter.newFileHashBasedFilter(9, 5);

		try (FileShareServer server = new FileShareServer(servicePort, uploadDirectory, timePeerFilter, hashPeerFilter, bootstrapPeerAddresses)) {
			System.out.println("Jax-WS based P2P file-share server running on one resource monitor thread, enter \"quit\" to stop.");
			System.out.format("Service URI is %s.\n", server.getLocalServiceURI().toASCIIString());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}