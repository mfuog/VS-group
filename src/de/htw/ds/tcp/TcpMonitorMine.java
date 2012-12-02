package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import de.htw.ds.TypeMetadata;
import de.htw.ds.util.BinaryTransporter;
import de.htw.ds.util.MultiOutputStream;
import de.htw.ds.util.SocketAddress;


/**
 * <p>This class models a TCP monitor, i.e. a TCP router that mirrors all
 * information between two ports, while logging it at the same time.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
@SuppressWarnings("unused")	//TODO: remove this line
public final class TcpMonitorMine implements Runnable, Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }

	private static final Random RANDOMIZER = new Random();
	private static final String CONNECTION_ID_PATTERN = "%1$tF-%1$tT.%tL-%06d";
	private static final String LOG_FILE_PATTERN = "%s-%s.log";
	private static final int MAX_PACKET_SIZE = 0xFFFF;

	private final ServerSocket serviceSocket;
	private final ExecutorService executorService;
	private final InetSocketAddress forwardAddress;
	private final Path contextPath;


	/**
	 * Public constructor.
	 * @param servicePort the service port (monitor)
	 * @param forwardAddress the forward address (destination)
	 * @param contextPath the context directory path (monitor)
	 * @throws NullPointerException if the given socket-address of context directory is null
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *    or the given context path is not a directory
	 */
	public TcpMonitorMine(final int servicePort, final InetSocketAddress forwardAddress, final Path contextPath) throws IOException {
		super();	//warum?
		if (forwardAddress == null) throw new NullPointerException();
		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());

		this.serviceSocket = new ServerSocket(servicePort);
		this.executorService = Executors.newCachedThreadPool();
		this.forwardAddress = forwardAddress;
		this.contextPath = contextPath;

		final Thread thread = new Thread(this, "tcp-acceptor");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes this server.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		this.serviceSocket.close();
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run() {
		while (true) {
			Socket clientConnection = null;
			try {
				clientConnection = this.serviceSocket.accept();	//auf Anfrage horchen & blocken
				//diese connection wird in ConnectionHandler indirekt beendet durch schließen des zugehörigen Sockets

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection);
				this.executorService.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Throwable exception) {
				try { clientConnection.close(); } catch (final Throwable nestedException) {}
				try { Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception); } catch (final Throwable nestedException) {}
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
		//System.out.println("Args:");
		//for(int i=0; i<args.length;i++){System.out.println(args[i]);}
		
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress forwardAddress = new SocketAddress(args[2]).toInetSocketAddress();

		try (TcpMonitorMine server = new TcpMonitorMine(servicePort, forwardAddress, contextPath)) {
			// print welcome message
			System.out.println("TCP monitor running on one acceptor thread, type \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.serviceSocket.getLocalPort());
			System.out.format("Context directory is %s.\n", contextPath);
			System.out.format("Forward address is %s.\n", forwardAddress.toString());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(reader.readLine())); } catch (final IOException exception) {}
		}
	}


	/**
	 * <p>Instances of this inner class handle TCP client connections
	 * accepted by a TCP monitor.</p> 
	 */
	private class ConnectionHandler implements Runnable {	//defines job of TCP Monitor
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
			final String connectionID = String.format(CONNECTION_ID_PATTERN, System.currentTimeMillis(), RANDOMIZER.nextInt(1000000));
			final Path requestLogPath = TcpMonitorMine.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "request"));
			final Path responseLogPath = TcpMonitorMine.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "response"));

			try (Socket serverConnection = new Socket(TcpMonitorMine.this.forwardAddress.getAddress(), TcpMonitorMine.this.forwardAddress.getPort()))
			{

				
				InputStream requestStreamFromClient = clientConnection.getInputStream();
				OutputStream responseStreamToClient = clientConnection.getOutputStream();
				
				InputStream responseStreamFromServer = serverConnection.getInputStream();
				OutputStream requestStreamToServer = serverConnection.getOutputStream();
				
				//FileInputStream responseStreamToFile = new FileInputStream(newFilerequestLogPath);
				
				//Socket dataConnection = new Socket(serverSocketAddress.getAddress(), serverSocketAddress.getPort());
				OutputStream requestStreamToLog = Files.newOutputStream(requestLogPath);
				OutputStream responseStreamToLog = Files.newOutputStream(responseLogPath);
				
				MultiOutputStream requestStreams = new MultiOutputStream(requestStreamToServer, requestStreamToLog);
				MultiOutputStream responseStreams = new MultiOutputStream(responseStreamToClient, responseStreamToLog);
				

				final Future<Long> rf1 = TcpMonitorMine.this.executorService.submit((Callable<Long>)new BinaryTransporter(true, 0xFFFF, requestStreamFromClient, requestStreams));
				final Future<Long> rf2 = TcpMonitorMine.this.executorService.submit((Callable<Long>)new BinaryTransporter(true, 0xFFFF, responseStreamFromServer, responseStreams));
							
				
				try {
					final long requestBytes = rf1.get();
					final long responseBytes = rf2.get(); 
					Logger.getGlobal().log(Level.INFO, "Connection {0} transported {1} request bytes and {2} response bytes.", new Object[] { connectionID, requestBytes, responseBytes });
				} catch (final ExecutionException exception) {
					throw exception.getCause();
				}
			} catch (final Throwable exception) {
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			} finally {
				try { this.clientConnection.close(); } catch (final Throwable exception) {}
			}
		}
	}
}