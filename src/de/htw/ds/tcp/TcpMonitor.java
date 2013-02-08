package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.BinaryTransporter;
import de.sb.javase.io.MultiOutputStream;
import de.sb.javase.io.SocketAddress;


/**
 * <p>This class models a TCP monitor, i.e. a TCP router that mirrors all
 * information between two ports, while logging it at the same time.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class TcpMonitor implements Runnable, AutoCloseable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	/**
	 * <p>TCP monitor watchers are registered with a TCP monitor to act
	 * whenever communication data becomes available, or a communication
	 * transmission causes exceptions. This callback design allows
	 * different kinds of applications to profit from a single monitor
	 * implementation.
	 */
	public static interface Watcher {
		/**
		 * Called whenever a TCP monitor notices that two corresponding
		 * connections have finished exchanging data. The given record
		 * contains all the data exchanged, plus the open and close
		 * timestamps of the data exchange.
		 * @param record the monitor record
		 */
		void recordCreated(final TcpMonitorRecord record);

		/**
		 * Called whenever a TCP monitor catches an exception while
		 * communication transmissions take place.
		 * @param exception the exception
		 */
		void exceptionCatched(final Exception exception);
	}


	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;
	private final InetSocketAddress forwardAddress;
	private final TcpMonitor.Watcher watcher;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param forwardAddress the forward address
	 * @param watcher the monitor watcher that is notified of connection activity
	 * @throws NullPointerException if the given address or watcher is <tt>null</tt>
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public TcpMonitor(final int servicePort, final InetSocketAddress forwardAddress, final TcpMonitor.Watcher watcher) throws IOException {
		super();
		if (forwardAddress == null | watcher == null) throw new NullPointerException();

		this.executorService = Executors.newCachedThreadPool();
		this.serviceSocket = new ServerSocket(servicePort);
		this.forwardAddress = forwardAddress;
		this.watcher = watcher;

		final Thread thread = new Thread(this, "tcp-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes this server.
	 */
	public void close() {
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
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run() {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.serviceSocket.accept();

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection);
				this.executorService.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Exception exception) {
				try { clientConnection.close(); } catch (final Exception nestedException) {}
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		}
	}



	/**
	 * <p>Instances of this inner class handle TCP client
	 * connections accepted by a TCP monitor.</p> 
	 */
	private class ConnectionHandler implements Runnable {
		final Socket clientConnection;

		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @throws NullPointerException if the given connection is <tt>null</tt>
		 */
		public ConnectionHandler(final Socket clientConnection) {
			super();
			if (clientConnection == null) throw new NullPointerException();

			this.clientConnection = clientConnection;
		}


		/**
		 * Handles the client connection by transporting all data to a new
		 * server connection, and vice versa. Closes all connections upon
		 * completion.
		 */
		public void run() {
			try (Socket serverConnection = new Socket(TcpMonitor.this.forwardAddress.getHostName(), TcpMonitor.this.forwardAddress.getPort())) {
				final long openTimestamp = System.currentTimeMillis();
				try (
					ByteArrayOutputStream requestDataSink = new ByteArrayOutputStream();
					ByteArrayOutputStream responseDataSink = new ByteArrayOutputStream();
					OutputStream requestSink = new MultiOutputStream(serverConnection.getOutputStream(), requestDataSink);
					OutputStream responseSink = new MultiOutputStream(this.clientConnection.getOutputStream(), responseDataSink)
				) {
					final Callable<?> requestTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, this.clientConnection.getInputStream(), requestSink);
					final Callable<?> responseTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, serverConnection.getInputStream(), responseSink);
					final Future<?> requestFuture = TcpMonitor.this.executorService.submit(requestTransporter);
					final Future<?> responseFuture = TcpMonitor.this.executorService.submit(responseTransporter);
					requestFuture.get();
					responseFuture.get();

					final long closeTimestamp = System.currentTimeMillis();
					final byte[] requestData = requestDataSink.toByteArray();
					final byte[] responseData = responseDataSink.toByteArray();
					final TcpMonitorRecord record = new TcpMonitorRecord(openTimestamp, closeTimestamp, requestData, responseData);
					TcpMonitor.this.watcher.recordCreated(record);
				} catch (final InterruptedException interrupt) {
					throw new ThreadDeath();
				} catch (final ExecutionException exception) {
					final Throwable cause = exception.getCause();
					if (cause instanceof Error) throw (Error) cause;
					if (cause instanceof RuntimeException) throw (RuntimeException) cause;
					if (cause instanceof IOException) throw (IOException) cause;
					throw new AssertionError();
				}
			} catch (final Exception exception) {
				TcpMonitor.this.watcher.exceptionCatched(exception);
			} finally {
				try { this.clientConnection.close(); } catch (final Exception exception) {}
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port,
	 * a server context directory, and the forward socket-address as address:port combination.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *    or the given context path is not a directory
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress forwardAddress = new SocketAddress(args[2]).toInetSocketAddress();

		final TcpMonitor.Watcher watcher = new TcpMonitor.Watcher() {
			public void recordCreated(final TcpMonitorRecord record) {
				final String fileName = String.format("%1$tF-%1$tH.%1$tM.%1$tS.%tL-%d.log", record.getOpenTimestamp(), record.getIdentity());
				final Path filePath = contextPath.resolve(fileName);
				try (OutputStream fileSink = Files.newOutputStream(filePath)) {
					fileSink.write(record.getRequestData());
					fileSink.write("\n\n*** RESPONSE DATA ***\n\n".getBytes("ASCII"));
					fileSink.write(record.getResponseData());
				} catch (final Exception exception) {
					this.exceptionCatched(exception);
				}
			}

			public void exceptionCatched(final Exception exception) {
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			}
		};

		try (TcpMonitor server = new TcpMonitor(servicePort, forwardAddress, watcher)) {
			// print welcome message
			System.out.println("TCP monitor running on one acceptor thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Forward socket address is %s:%s.\n", forwardAddress.getHostName(), forwardAddress.getPort());
			System.out.format("Context directory is %s.\n", contextPath);
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}
}