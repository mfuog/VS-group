package de.htw.ds.sort;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import de.htw.ds.TypeMetadata;


/**
 * <p>Server that provides stateful sorting using a custom sort
 * protocol. Note that the protocol only allows for string sorters.
 * The protocol syntax is defined in EBNF as follows:</p><pre>
 * cspRequest	:= { element CR } CR
 * cspResponse	:= ("error" CR) | ("ok" CR { element CR } )
 * CR			:= line separator
 * element		:= non-empty String
 * message      := String
 * </pre>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class SortServerSkeleton implements Runnable, Closeable {
	private final ServerSocket serviceSocket;
	private final StreamSorter<String> streamSorter;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param streamSorter the stream sorter
	 * @throws NullPointerException if the given stream sorter is null
	 * @throws IllegalArgumentException if the given service port is negative
	 * @throws IOException if the given port is already in use
	 */
	public SortServerSkeleton(final int servicePort, final StreamSorter<String> streamSorter) throws IOException {
		super();
		if (streamSorter == null) throw new NullPointerException();

		this.serviceSocket = new ServerSocket(servicePort);
		this.streamSorter = streamSorter;

		// Only a single service thread is required, even if this server is
		// implemented using the (static) service-only pattern. This is
		// because the stateful stream sorter cannot handle multiple sort
		// operations at once!
		final Thread thread = new Thread(this, "csp-service");
		thread.setDaemon(true);
		thread.start();
	}


	/**
	 * Closes the server.
	 * @throws IOException if there is an I/O related problem
	 */
	public void close() throws IOException {
		this.serviceSocket.close();
	}


	/**
	 * Periodically blocks until a request arrives, handles the latter subsequently.
	 */
	public void run() {
		while (!this.serviceSocket.isClosed()) {
			try (Socket connection = this.serviceSocket.accept()) {

				// TODO: write the server logic conforming to above protocol as the
				// counterpart to the given ProxySorter class. Note that the proxies
				// hold a connection for an entire write-sort-read cycle, therefore we
				// should do the same here. Make sure that both the connection is closed
				// and the underlying sorter is reset after the last sorted element has
				// been transmitted. Otherwise you'd have to restart the server after
				// every request!

			} catch (final Throwable exception) {
				exception.printStackTrace();
			} finally {
				this.streamSorter.reset();
			}
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a service port.
	 * @param args the given runtime arguments
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use
	 */
	public static void main(final String[] args) throws IOException {
		final long timestamp = System.currentTimeMillis();
		final int servicePort = Integer.parseInt(args[0]);

		// Only a single service thread is required because a stateful
		// sorter cannot handle multiple sort operations at once!
		final StreamSorter<String> streamSorter = SortServerSkeleton.createSorter();
		try (SortServerSkeleton server = new SortServerSkeleton(servicePort, streamSorter)) {
			System.out.println("Sort server running on one service thread, type \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.serviceSocket.getLocalPort());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			// wait for stop signal on System.in
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(reader.readLine())); } catch (final IOException exception) {}
		}
	}


	/**
	 * Returns a new stream sorter.
	 * @return the stream sorter
	 */
	private static StreamSorter<String> createSorter() {
		return null; // TODO: create stream sorter.
	}
}