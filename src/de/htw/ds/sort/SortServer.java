package de.htw.ds.sort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;
import de.sb.javase.TypeMetadata;


/**
 * <p>Server that provides stateful sorting using a custom sort
 * protocol. Note that the protocol only allows for string sorters.
 * The protocol syntax is defined in EBNF as follows:</p><pre>
 * cspRequest	:= { element CR } CR
 * cspResponse	:= ("error" CR message) | ("ok" { CR element } )
 * CR			:= line separator
 * element		:= non-empty String
 * message      := String
 * </pre>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class SortServer implements Runnable, AutoCloseable {
	private final ServerSocket serviceSocket;
	private final StreamSorter<String> streamSorter;


	/**
	 * Public constructor.
	 * @param servicePort the service port
	 * @param streamSorter the stream sorter
	 * @throws NullPointerException if the given sorterTemplate is null
	 * @throws IllegalArgumentException if the given service port is outside range [0, 0xFFFF]
	 * @throws IOException if the given port is already in use, or cannot be bound
	 */
	public SortServer(final int servicePort, final StreamSorter<String> streamSorter) throws IOException {
		super();
		if (streamSorter == null) throw new NullPointerException();

		this.streamSorter = streamSorter;
		this.serviceSocket = new ServerSocket(servicePort);

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
		while (!this.serviceSocket.isClosed()) {
			try (Socket connection = this.serviceSocket.accept()) {
				try (BufferedReader charSource = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					try (BufferedWriter charSink = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {

						long elementCount = 0;
						for(String element = charSource.readLine(); !element.isEmpty(); element = charSource.readLine()) {
							this.streamSorter.write(element);
							elementCount += 1;
						}

						try {
							this.streamSorter.sort();
						} catch (final Exception exception) {
							final String message = exception.getMessage();
							charSink.write("error");
							charSink.newLine();
							charSink.write(message == null ? "" : message);
							throw exception;
						}

						charSink.write("ok");
						for (long todo = elementCount; todo > 0; --todo) {
							final String element = this.streamSorter.read();
							charSink.newLine();
							charSink.write(element);
						}
						charSink.flush();
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace();
			} finally {
				try { this.streamSorter.reset(); } catch (final Exception exception) {}
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

		final StreamSorter<String> streamSorter = SortServer.createSorter();
		try (SortServer server = new SortServer(servicePort, streamSorter)) {
			System.out.println("Sort server running on one service thread, enter \"quit\" to stop.");
			System.out.format("Service port is %s.\n", server.getServicePort());
			System.out.format("Startup time is %sms.\n", System.currentTimeMillis() - timestamp);

			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			try { while (!"quit".equals(charSource.readLine())); } catch (final IOException exception) {}
		}
	}


	/**
	 * Returns a new stream sorter.
	 * @return the stream sorter
	 */
	private static StreamSorter<String> createSorter() {
		final Queue<StreamSorter<String>> sorterQueue = new ArrayDeque<StreamSorter<String>>();
		for (int index = Runtime.getRuntime().availableProcessors(); index > 0; --index) {
			sorterQueue.add(new StreamSingleThreadSorter<String>());
		}

		while (sorterQueue.size() > 1) {
			sorterQueue.add(new StreamMultiThreadSorter<String>(sorterQueue.poll(), sorterQueue.poll()));
		}

		return sorterQueue.poll();
	}
}