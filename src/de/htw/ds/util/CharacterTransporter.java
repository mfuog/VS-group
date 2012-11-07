package de.htw.ds.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.SocketException;
import java.util.concurrent.Callable;

import de.htw.ds.TypeMetadata;


/**
 * <p>Character stream transporter class. Note that instances
 * can be used both as runnables and as callables!</p>
 */
@TypeMetadata(copyright="2011-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public class CharacterTransporter implements Callable<Long> {
	private final boolean closeUponCompletion;
	private final int bufferSize;
	private final Reader source;
	private final Writer sink;


	/**
	 * Creates a new instance.
	 * @param closeUponCompletion <tt>true</tt> if the streams shall be
	 *    closed upon completion, <tt>false</tt> otherwise
	 * @param bufferSize the buffer size in characters
	 * @param source the data source
	 * @param sinks the data sink
	 * @throws NullPointerException if the given source or sink is <tt>null</tt>
	 * @throws IllegalArgumentException if the given buffer size is negative
	 */
	public CharacterTransporter(final boolean closeUponCompletion, final int bufferSize, final Reader source, final Writer sink) {
		super();
		if (source == null | sink == null) throw new NullPointerException();
		if (bufferSize <= 0) throw new IllegalArgumentException();

		this.closeUponCompletion = closeUponCompletion;
		this.bufferSize = bufferSize;
		this.source = source;
		this.sink = sink;
	}


	/**
	 * Transports all data from the source to the target, and returns the
	 * number of bytes transferred. Terminates normally if the source indicates
	 * EOF, or if a socket exception occurs which indicates the sink has been
	 * closed by another thread.
	 * @return the number of bytes transferred
	 * @throws IOException if there is an I/O related problem
	 */
	public Long call() throws IOException {
		long result = 0;

		final char[] buffer = new char[this.bufferSize];
		try {
			for (int bytesRead = this.source.read(buffer); bytesRead != -1; bytesRead = this.source.read(buffer)) {
				this.sink.write(buffer, 0, bytesRead);
				result += bytesRead;
			}
		} catch (final SocketException exception) {
			// treat as EOF because a TCP stream has been closed by the other side
		} finally {
			if (this.closeUponCompletion) {
				try { this.source.close(); } catch (final Throwable exception) {}
				try { this.sink.close(); } catch (final Throwable exception) {}
			}
		}

		return result;
	}
}