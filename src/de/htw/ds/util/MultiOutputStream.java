package de.htw.ds.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.htw.ds.TypeMetadata;


/**
 * <p>This class models filter-like output streams that are based
 * on multiple data sinks. Writing into such a stream means writing
 * into multiple streams at once.</p>
 */
@TypeMetadata(copyright="2012-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public class MultiOutputStream extends OutputStream {

	private final List<OutputStream> sinks;


	/**
	 * Constructs an instance from multiple data sinks. Any <tt>null</tt>
	 * sinks given is ignored.
	 * @param sinks the data sinks
	 */
	public MultiOutputStream(final OutputStream... sinks) {	//varargs
		super();

		this.sinks = new ArrayList<>();	//warum überschreiben???
		if (sinks != null) {
			for (final OutputStream sink : sinks) {
				if (sink != null) this.sinks.add(sink);
			}
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public synchronized void close() throws IOException {
		IOException exception = null;

		for (final OutputStream sink : this.sinks) {
			try {
				sink.close();
			} catch (final IOException e) {
				exception = e;
			}
		}
		this.sinks.clear();

		if (exception != null) throw exception;
    }


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public void write(final byte[] buffer, final int offset, final int length) throws IOException {
		if (this.sinks.isEmpty()) throw new EOFException();

		for (final OutputStream sink : this.sinks) {
			sink.write(buffer, offset, length);
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public void write(final int value) throws IOException {
		final byte[] buffer = new byte[] { (byte) value };
		this.write(buffer);
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	public void flush() throws IOException {
		for (final OutputStream sink : this.sinks) {
			sink.flush();
		}
	}
}