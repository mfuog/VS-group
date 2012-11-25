package de.htw.ds.sort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import de.htw.ds.TypeMetadata;


/**
 * <p>String sorter implementation that forwards all requests to a
 * stream sort server, using a custom sort protocol. Write requests are
 * forwarded by writing the given string to a buffered socket writer,
 * followed by a newline. Note that null or empty elements are not
 * allowed! Sort requests are forwarded by sending an empty string
 * followed by a newline instead. Read requests mean that a line
 * is read from a buffered socket reader, while reset requests
 * close the connection and null it's related fields.</p>
 * <p>Note that a single connection is reused for all interactions
 * with the server, which implies all non-private methods must be
 * synchronized. The protocol syntax is defined in EBNF as follows:</p><pre>
 * cspRequest	:= { element CR } CR
 * cspResponse	:= ("error" CR message) | ("ok" CR { element CR } )
 * CR			:= line separator
 * element		:= non-empty String
 * message      := String
 * </pre>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.1", authors="Sascha Baumeister")
public final class StreamMultiProgramSorter implements StreamSorter<String> {
	private final InetSocketAddress socketAddress;
	private State state;
	private transient Socket connection;
	private transient BufferedReader reader;
	private transient BufferedWriter writer;


	/**
	 * Creates a new instance able to connect to the given socket address.
	 * @param socketAddress the socket address
	 * @throws NullPointerException if the given socket address is <tt>null</tt>
	 */
	public StreamMultiProgramSorter(final InetSocketAddress socketAddress) {
		if (socketAddress == null) throw new NullPointerException();

		this.socketAddress = socketAddress;
		this.state = State.WRITE;
	}


	/**
	 * {@inheritDoc}
	 */
	public synchronized void reset() {
		if (this.connection != null) {
			try { this.connection.close(); } catch (final Throwable exception) {}
			this.connection = null;
			this.reader = null;
			this.writer = null;
			this.state = State.WRITE;
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in <tt>WRITE</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	public synchronized void write(final String element) {
		if (this.state != State.WRITE) throw new IllegalStateException();
		if (element.isEmpty()) throw new IllegalArgumentException();

		try {
			if (this.connection == null) this.connect();
			this.writer.write(element);
			this.writer.newLine();
		} catch (final IOException exception) {
			this.reset();
			throw new IllegalStateException(exception);
		}
	}


	/**
	 * {@inheritDoc}
	 * @throws IllegalStateException if the sorter is not in <tt>READ</tt> state,
	 *    or if there is a problem with the underlying data
	 */
	public synchronized void sort() {
		if (this.state != State.WRITE) throw new IllegalStateException();
		if (this.connection == null) return;

		try {
			this.writer.newLine();
			this.writer.flush();

			final String result = this.reader.readLine();
			if ("error".equals(result)) {
				final String message = this.reader.readLine();
				throw new IllegalStateException(message);
			} else if ("ok".equals(result)) {
				if (!this.connection.isClosed()) this.state = State.READ;
			} else {
				throw new IllegalStateException();
			}
		} catch (final IOException exception) {
			throw new IllegalStateException(exception);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public synchronized String read() {
		if (this.state != State.READ) throw new IllegalStateException();
		try {
			final String element = this.reader.readLine();
			if (this.connection.isClosed()) this.state = State.WRITE;
			return element;
		} catch (final IOException exception) {
			throw new IllegalStateException(exception);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public State getState() {
		return this.state;
	}


	/**
	 * Creates a new active server connection.
	 * @throws IOException if there is an I/O related problem
	 */
	private void connect() throws IOException {
		this.connection = new Socket(this.socketAddress.getAddress(), this.socketAddress.getPort());
		this.reader = new BufferedReader(new InputStreamReader(this.connection.getInputStream()));
		this.writer = new BufferedWriter(new OutputStreamWriter(this.connection.getOutputStream()));
	}
}