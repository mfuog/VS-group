package de.htw.ds.tcp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
public final class TcpMonitor implements Runnable, Closeable {
	// workaround for Java7 bug initializing global logger without parent!
	static { LogManager.getLogManager(); }
	//static initializer, kann nur einen geben: Klassenkonstrukor der nichts außer die Klasse zurückgibt.->static
	//aufgerufen wenn die Klasse das erste mal geladen wird

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
	 * @param servicePort the service port
	 * @param forwardAddress the forward address
	 * @param contextPath the context directory path
	 * @throws NullPointerException if the given socket-address of context directory is null
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound,
	 *    or the given context path is not a directory
	 */
	public TcpMonitor(final int servicePort, final InetSocketAddress forwardAddress, final Path contextPath) throws IOException {
		super();
		if (forwardAddress == null) throw new NullPointerException();
		if (!Files.isDirectory(contextPath)) throw new NotDirectoryException(contextPath.toString());

		this.contextPath = contextPath;
		this.executorService = Executors.newCachedThreadPool();
		this.serviceSocket = new ServerSocket(servicePort);
		this.forwardAddress = forwardAddress;

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
				clientConnection = this.serviceSocket.accept();

				final ConnectionHandler connectionHandler = new ConnectionHandler(clientConnection);
				this.executorService.execute(connectionHandler);
			} catch (final SocketException exception) {
				break;
			} catch (final Throwable exception) {
				try { clientConnection.close(); } catch (final Throwable nestedException) {}
				//unbedingt verhindern, dass while-Schleife Thread verlassen wird. Was kann passieren? -> error (kann immer passieren)
				//wenn While verlassen wird, läuft mein Thread noch, aber Service tut nix sinnvolles mehr (kein request wird mehr abgeholt, weil nciht mehr gehorcht wird).
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
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);
		final Path contextPath = Paths.get(args[1]).normalize();
		final InetSocketAddress forwardAddress = new SocketAddress(args[2]).toInetSocketAddress();

		//Server ist nichts als ein Objekt/Resource, das irgendwann auch wieder geschloßen wird.
		//Dieser Server könnte auch als Non-deamon betrieben werden, weil ... xD
		try (TcpMonitor server = new TcpMonitor(servicePort, forwardAddress, contextPath)) {
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
	private class ConnectionHandler implements Runnable {
		final Socket clientConnection;


		/**
		 * Creates a new instance from a given client connection.
		 * @param clientConnection the connection
		 * @throws NullPointerException if the given connection is <tt>null</tt>
		 */
		public ConnectionHandler(final Socket clientConnection) {
			super();
			//vor jedem Konstruktor wird der Konstruktor der Superklasse aufgerufen. 
			//Nicht nötig, weil Compiler amcht das eh, aber expliziter zum Coderverständnis
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
			final Path requestLogPath = TcpMonitor.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "request"));
			final Path responseLogPath = TcpMonitor.this.contextPath.resolve(String.format(LOG_FILE_PATTERN, connectionID, "response"));

			try (
				
				Socket serverConnection = new Socket(TcpMonitor.this.forwardAddress.getAddress(), TcpMonitor.this.forwardAddress.getPort());
				//schreiben auf 
				OutputStream requestSink = new MultiOutputStream(serverConnection.getOutputStream(), Files.newOutputStream(requestLogPath));
				OutputStream responseSink = new MultiOutputStream(this.clientConnection.getOutputStream(), Files.newOutputStream(responseLogPath));
			) {
				final Callable<Long> requestTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, this.clientConnection.getInputStream(), requestSink);
				final Callable<Long> responseTransporter = new BinaryTransporter(true, MAX_PACKET_SIZE, serverConnection.getInputStream(), responseSink);
				
				//Holt einen Thread aus dem Pool und beauftragt es auf dem Future auszuführen.
				final Future<Long> requestFuture = TcpMonitor.this.executorService.submit(requestTransporter);
				final Future<Long> responseFuture = TcpMonitor.this.executorService.submit(responseTransporter);

				try {
					//normalerweise müsste man das aufteilen, aber hier kann man sich das sparen weil ich in einem RUnnable bin und die Exception nur werfe um sie gleich wieder zu fangen. 
					// Normalerweise müsste ich die exception gecastet neu werfen und... nochwas xD
					final long requestBytes = requestFuture.get();
					final long responseBytes = responseFuture.get(); 
					Logger.getGlobal().log(Level.INFO, "Connection {0} transported {1} request bytes and {2} response bytes.", new Object[] { connectionID, requestBytes, responseBytes });
				} catch (final ExecutionException exception) {
					throw exception.getCause(); //wird in der Konsole geloggt
				}
			} catch (final Throwable exception) {
				Logger.getGlobal().log(Level.WARNING, exception.getMessage(), exception);
			} finally {
				try { this.clientConnection.close(); } catch (final Throwable exception) {}//Verbindung schließen, die Verbindung die mir (dem Verbindungshandler) gegeben wurde. 
				//...kein Try-for-resources möglich, weil Verbindung außerhalb erstellt wurde
				//Merke: immer überlegen: wo wird meine Resource wieder geschloßen? Ausnahme: In-Memory-Resourcen wie Buffer
			}
		}
	}
}