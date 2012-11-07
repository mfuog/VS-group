package de.htw.ds.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.Queue;

import de.htw.ds.TypeMetadata;


/**
 * <p>This class models filter-like input streams that are based
 * on multiple data sources. Reading from such a stream appears
 * to be continuously reading one source after the other.</p>
 */
@TypeMetadata(copyright="2012-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public class MultiInputStream extends InputStream {

	private final Queue<InputStream> sources;


	/**
	 * Constructs an instance from multiple data sources. Any <tt>null</tt>
	 * source given is ignored.
	 * @param sinks the data sinks
	 */
	public MultiInputStream(final InputStream... sources) {
		super();

		this.sources = new ArrayDeque<>();
		if (sources != null) {
			for (final InputStream source : sources) {
				if (source != null) this.sources.add(source);
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

		for (final InputStream source : this.sources) {
			try {
				source.close();
			} catch (final IOException e) {
				exception = e;
			}
		}
		this.sources.clear();

		if (exception != null) throw exception;
    }


	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized int available() {
		final InputStream source = this.sources.peek();

		try {
			return source.available();
		} catch (final Exception exception) {
			return 0;
		}
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public synchronized int read(final byte buffer[], final int offset, final int length) throws IOException {
		final InputStream source = this.sources.peek();
		if (source == null) return -1;

		try {
			final int bytesRead = source.read(buffer, offset, length);
			if (bytesRead != -1) return bytesRead;
		} catch (final SocketException exception) {
			// do nothing because an underlying socket stream has been
			// closed asynchronously while blocking!
		}

		try { this.sources.remove().close(); } catch (final IOException exception) {}
		return this.read(buffer, offset, length);
	}


	/**
	 * {@inheritDoc}
	 * @exception IOException if there is an I/O related problem
	 */
	@Override
	public int read() throws IOException {
		final byte[] buffer = new byte[1];
		final int bytesRead = this.read(buffer);
		return bytesRead == -1 ? -1 : buffer[0] & 0xFF;
	}
}