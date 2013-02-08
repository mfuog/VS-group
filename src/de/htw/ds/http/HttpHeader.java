package de.htw.ds.http;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import de.sb.javase.TypeMetadata;


/**
 * <p>Abstract HTTP header information, see RFC 2616 for details.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public abstract class HttpHeader<T> {
	protected static final Charset US_ASCII = Charset.forName("US-ASCII");

	private T type;
	private final byte version;
	private final byte revision;
	private final Map<String,String> properties;
	private final Map<String,Cookie> cookies;
	private transient InputStream bodySource;
	private transient OutputStream bodySink;


	/**
	 * Public constructor.
	 * @param version the HTTP version
	 * @param revision the HTTP revision
	 * @throws IllegalArgumentException if the given version or revision
	 *    is strictly negative
	 */
	public HttpHeader(final byte version, final byte revision) {
		super();
		if (version < 0 | revision < 0) throw new IllegalArgumentException();
	
		this.version = version;
		this.revision = revision;
		this.properties = new HashMap<String,String>();
		this.cookies = new HashMap<String,Cookie>();
	}


	/**
	 * Returns the request/response type.
	 * @return the HTTP request/response type
	 */
	public final T getType() {
		return this.type;
	}


	/**
	 * Sets the type.
	 * @param the HTTP request/response type
	 * @throws NullPointerException if the given type is <tt>null</tt>
	 */
	public final void setType(final T type) {
		this.type = type;
	}


	/**
	 * Returns the version.
	 * @return the HTTP version
	 */
	public final byte getVersion() {
		return this.version;
	}


	/**
	 * Returns the revision.
	 * @return the HTTP revision
	 */
	public final byte getRevision() {
		return this.revision;
	}


	/**
	 * Returns true if this is a HTTP 1.0 header, false otherwise.
	 * @return true if version is 1 and revision is 0, false otherwise
	 */
	public final boolean isHttp10() {
		return this.version == 1 && this.revision == 0;
	}


	/**
	 * Returns true if this is a HTTP 1.1 header, false otherwise.
	 * @return true if version and revision is 1, false otherwise
	 */
	public final boolean isHttp11() {
		return this.version == 1 && this.revision == 1;
	}


	/**
	 * Returns the HTTP request/response properties.
	 * @return the properties as a map
	 */
	public final Map<String,String> getProperties() {
		return this.properties;
	}


	/**
	 * Returns the HTTP request/response cookies.
	 * @return the cookies as a map
	 */
	public final Map<String,Cookie> getCookies() {
		return this.cookies;
	}


	/**
	 * Returns the body source.
	 * @return the body source
	 */
	public final InputStream getBodySource() {
		return this.bodySource;
	}


	/**
	 * Sets the body source.
	 * @param bodySource the body source
	 * @throws NullPointerException if the given body source is <tt>null</tt>
	 */
	protected final void setBodySource(final InputStream bodySource) {
		if (bodySource == null) throw new NullPointerException();
		this.bodySource = bodySource;
	}


	/**
	 * Returns the body sink.
	 * @return the body sink
	 */
	public final OutputStream getBodySink() {
		return this.bodySink;
	}


	/**
	 * Sets the body sink.
	 * @param bodySink the body sink
	 * @throws NullPointerException if the given body sink is <tt>null</tt>
	 */
	protected final void setBodySink(final OutputStream bodySink) {
		if (bodySink == null) throw new NullPointerException();
		this.bodySink = bodySink;
	}


	/**
	 * Writes the receiver to the given byte sink. Note that the
	 * given sink is not closed upon completion of this method!
	 * @param byteSink the byte sink to write the header to
	 * @throws IOException if there is a general I/O related problem
	 *    writing the header information
	 */
	protected abstract void write(final OutputStream byteSink) throws IOException;


	/**
	 * Reads the given byte source up to the next occurrence of
	 * '\n' (in ASCII), interprets the resulting data as an ASCII
	 * string, and returns the latter.
	 * @param byteSource the byte source
	 * @return the trimmed line
	 * @throws IOException if there is an I/O related problem
	 */
	protected static String readAsciiLine(final InputStream byteSource) throws IOException {
		boolean returnEncountered = false;
		final ByteArrayOutputStream byteSink = new ByteArrayOutputStream();
		for (int asciiCharacter = byteSource.read(); asciiCharacter != -1 & asciiCharacter != '\n'; asciiCharacter = byteSource.read()) {
			if (asciiCharacter == '\r') {
				returnEncountered = true;
			} else {
				if (returnEncountered) byteSink.write('\r');
				byteSink.write(asciiCharacter);
				returnEncountered = false;
			}
		}
		return new String(byteSink.toByteArray(), US_ASCII);
	}


	/**
	 * Returns a version or revision parsed from the given request words
	 * third entry. 
	 * @param requestWords the request words
	 * @param index the index of the request word to be parsed
	 * @param version true for version parsing, false for revision parsing
	 * @return the version or revision
	 * @throws ProtocolException if there are not enough request words, or if the
	 *    request word doesn't contain bytes at offset 5 or 7 respectively
	 */
	protected static byte parseVersionRevision(final String[] requestWords, final int index, final boolean version) throws ProtocolException {
		if (requestWords.length <= index) throw new ProtocolException();

		final int offset = version ? 5 : 7;
		try {
			return Byte.parseByte(requestWords[index].substring(offset, offset + 1));
		} catch (final NumberFormatException exception) {
			throw new ProtocolException();
		}
	}




	/**
	 * <p>This local filter stream class limits the number of bytes that
	 * can be read to the amount defined in the parent header. This is
	 * useful whenever an underlying stream contains multiple "parts" that
	 * shall be read individually. Apart from this, all I/O method calls
	 * are passed to the underlying stream, except for close() and the
	 * mark/reset methods.</p>
	 */
	protected final class HeaderInputStream extends FilterInputStream {

		private Long remainingBytes;


		/**
		 * Creates a new source from the given source.
		 * @param byteSource the underlying source
		 * @throws NullPointerException if the given source is <tt>null</tt>
		 */
		public HeaderInputStream(final InputStream byteSource) {
			super(byteSource);
			this.remainingBytes = null;
		}


		/**
		 * Commits the receiver.
		 * @throws IOException if there is an underlying I/O related problem
		 */
		private void commit() throws IOException {
			if (this.remainingBytes != null) return;

			final String contentLengthText = HttpHeader.this.getProperties().get("Content-Length");
			try {
				this.remainingBytes = Long.parseLong(contentLengthText);
			} catch (final Exception exception) {
				this.remainingBytes = -1l;
			}
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		@Override
		public void close() throws IOException {
			this.remainingBytes = 0L;
			super.close();
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		@Override
		public int read() throws IOException {
			if (this.remainingBytes == null) this.commit();
			if (this.remainingBytes == 0) return -1;

			final int result = super.read();
			if (result == -1) {
				this.remainingBytes = 0L;
			} else if (this.remainingBytes > 0) {
				this.remainingBytes -= 1;
			}
			return result;
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		@Override
		public int read(final byte[] buffer, final int offset, final int length) throws IOException {
			if (this.remainingBytes == null) this.commit();
			if (this.remainingBytes == 0) return -1;

			final int bytesRead = this.remainingBytes < 0
				? super.read(buffer, offset, length)
				: super.read(buffer, offset, length < this.remainingBytes ? length : (int) this.remainingBytes.longValue());
			if (bytesRead == -1) {
				this.remainingBytes = 0l;
			} else if (this.remainingBytes >= 0) {
				this.remainingBytes -= bytesRead;
			}

			return bytesRead;
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		@Override
		public long skip(long n) throws IOException {
			if (this.remainingBytes == null) this.commit();
			if (this.remainingBytes == 0) return 0;
			if (n > this.remainingBytes) n = this.remainingBytes;

			final long bytesSkipped = super.skip(n);
			if (this.remainingBytes > 0) this.remainingBytes -= bytesSkipped;
			return bytesSkipped;
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		@Override
		public int available() throws IOException {
			if (this.remainingBytes == null) this.commit();

			final int available = super.available();
			return this.remainingBytes < 0
				? available
				: (available < this.remainingBytes ? available : (int) this.remainingBytes.longValue());
		}


		/**
		 * Does nothing because mark/reset is not supported.
		 */
		@Override
		public void mark(final int readlimit) {
			// do nothing
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException because mark/reset is not supported
		 */
		@Override
		public void reset() throws IOException {
			throw new IOException("mark/reset not supported");
		}


		/**
		 * Always returns false because mark/reset is not supported.
		 */
		@Override
		public boolean markSupported() {
			return false;
		}
	}




	/**
	 * <p>This local filter stream class precedes any content written to the
	 * underlying sink with the outer HTTP header, organized according to the
	 * HTTP protocol.</p>
	 */
	protected final class HeaderOutputStream extends FilterOutputStream {

		private boolean committed;


		/**
		 * Creates a new uncommitted header sink.
		 * @param byteSink the underlying sink
		 * @throws NullPointerException if the given sink is <tt>null</tt>
		 */
		protected HeaderOutputStream(final OutputStream byteSink) {
			super(byteSink);
			this.committed = false;
		}


		/**
		 * Commits the receiver.
		 * @throws IOException if there is an underlying I/O related problem
		 */
		private void commit() throws IOException {
			HttpHeader.this.write(this.out);
			this.committed = true;
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		public void write(final byte buffer[], final int offset, final int length) throws IOException {
			if (!this.committed) this.commit();
			super.write(buffer, offset, length);
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		public void write(final int value) throws IOException {
			if (!this.committed) this.commit();
			super.write(value);
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		public void flush() throws IOException {
			if (!this.committed) this.commit();
			super.flush();
		}


		/**
		 * {@inheritDoc}
		 * @throws IOException if there is an underlying I/O related problem
		 */
		public void close() throws IOException {
			if (this.committed) {
				try { this.flush(); } catch (final Exception exception) {}
			}
			super.close();
		}
	}
}