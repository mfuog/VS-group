package de.htw.ds.http;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;
import de.sb.javase.TypeMetadata;


/**
 * <p>HTTP response header information, see RFC 2616 for details.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class HttpResponseHeader extends HttpHeader<HttpResponseHeader.Type> {
	public static enum Type {
		CONTINUE		((short) 100, "Continue"),
		SWITCH			((short) 101, "Switching Protocols"),
	    OK				((short) 200, "OK"),
	    CREATED			((short) 201, "Created"),
		ACCEPTED		((short) 202, "Accepted"),
		INFO			((short) 203, "Non-Authoritative Information"),
		NO_CONTENT		((short) 204, "No Content"),
		RESET_CONTENT	((short) 205, "Reset Content"),
		PARTIAL_CONTENT	((short) 206, "Partial Content"),
		SELECT_CONTENT	((short) 300, "Multiple Choices"),
		MOVED			((short) 301, "Moved Permanently"),
		FOUND			((short) 302, "Found"),
		OTHER			((short) 303, "See Other"),
		ORIGINAL		((short) 304, "Not Modified"),
		PROXY			((short) 305, "Use Proxy"),
		REDIRECT		((short) 307, "Temporary Redirect"),
		BAD_REQUEST		((short) 400, "Bad Request"),
		UNAUTHORIZED	((short) 401, "Unauthorized"),
		NO_PAYMENT		((short) 402, "Payment Required"),
		FORBIDDEN		((short) 403, "Forbidden"),
		NOT_FOUND		((short) 404, "Not Found"),
		BAD_METHOD		((short) 405, "Method Not Allowed"),
		UNACCEPTABLE	((short) 406, "Not Acceptable"),
		NO_PROXYAUTH	((short) 407, "Proxy Authentication Required"),
		REQUEST_TIMEOUT	((short) 408, "Request Time-out"),
		CONFLICT		((short) 409, "Conflict"),
		GONE			((short) 410, "Gone"),
		NO_LENGTH		((short) 411, "Length Required"),
		PRECONDITION	((short) 412, "Precondition Failed"),
		SIZE_ENTITY		((short) 413, "Request Entity Too Large"),
		SIZE_REQUEST	((short) 414, "Request-URI Too Large"),
		UNSUPPORTED		((short) 415, "Unsupported Media Type"),
		RANGE_REQUEST	((short) 416, "Requested range not satisfiable"),
		EXPECTATION		((short) 417, "Expectation Failed"),
	    INTERNAL		((short) 500, "Internal Server Error"),
		NO_IMPL			((short) 501, "Not Implemented"),
		BAD_GATEWAY		((short) 502, "Bad Gateway"),
		NO_SERVICE		((short) 503, "Service Unavailable"),
		GATEWAY_TIMEOUT	((short) 504, "Gateway Time-out"),
		VERSION			((short) 505, "HTTP Version not supported");

		private final short code;
		private final String comment;

		private Type(final short code, final String comment) {
			this.code = code;
			this.comment = comment;
		}

		public short getCode() {
			return this.code;
		}

		public String getComment() {
			return this.comment;
		}

		public static Type valueOf (short code) {
			for (final Type type : Type.values()) {
				if (type.getCode() == code) return type; 
			}
			return null;
		}
	}


	/**
	 * Public constructor for use in case an HTTP response shall be sent.
	 * @param version the HTTP version
	 * @param revision the HTTP revision
	 * @param bodySource an optional body source, or <tt>null</tt>
	 * @param responseSink a response sink
	 * @throws NullPointerException if the given response sink is <tt>null</tt> 
	 * @throws IllegalArgumentException if the given version or revision is
	 *    strictly negative
	 */
	public HttpResponseHeader(final byte version, final byte revision, final InputStream bodySource, final OutputStream responseSink) {
		super(version, revision);

		this.setType(Type.OK);
		this.setBodySource(new HeaderInputStream(bodySource == null? new ByteArrayInputStream(new byte[0]) : bodySource));
		this.setBodySink(new HeaderOutputStream(responseSink));
	}


	/**
	 * Public constructor for use in case an HTTP response is received.
	 * @param responseSource a response source
	 * @param responseSink an optional response sink, or <tt>null</tt>
	 * @throws NullPointerException if the given response source is <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	public HttpResponseHeader(final InputStream responseSource, final OutputStream responseSink) throws IOException {
		this(readAsciiLine(responseSource).split(" "), responseSource, responseSink);
	}


	/**
	 * Public constructor for use in case an HTTP response is received.
	 * @param responseWords a response line split into it's constituent words
	 * @param responseSource a response source
	 * @param responseSink an optional response sink, or <tt>null</tt>
	 * @throws NullPointerException if the given response words or source is <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	private HttpResponseHeader(final String[] responseWords, final InputStream responseSource, final OutputStream responseSink) throws IOException {
		super(parseVersionRevision(responseWords, 0, true), parseVersionRevision(responseWords, 0, false));
		if (responseWords.length < 3 | responseWords[0].length() != 8 | !responseWords[0].startsWith("HTTP/") | responseWords[0].charAt(6) != '.') throw new ProtocolException();

		try {
			final short code = Short.parseShort(responseWords[1]);
			this.setType(Type.valueOf(code));
		} catch (final Exception exception) {
			throw new ProtocolException();
		}

		for (String line = readAsciiLine(responseSource); !line.isEmpty(); line = readAsciiLine(responseSource)) {
			int colonOffset = line.indexOf(':');
			if (colonOffset == -1) throw new ProtocolException();

			final String propertyName = line.substring(0, colonOffset).trim();
			final String propertyValue = line.substring(colonOffset + 2).trim();
			if (propertyName.equals("Set-Cookie")) {
				final String[] cookieAssociations = propertyValue.split(";");
				final String[] nameValueFragments = cookieAssociations[0].split("=");
				if (nameValueFragments.length != 2) throw new ProtocolException();

				final Map<String,String> properties = new HashMap<String,String>();
				for (final String association : propertyValue.split(";")) {
					final String[] fragments = association.split("=");
					if (fragments.length != 2) throw new ProtocolException();
					properties.put(fragments[0].trim(), fragments[1].trim());
				}
				final Cookie cookie = new Cookie(nameValueFragments[0].trim(), nameValueFragments[1].trim(), properties.get("expires"), properties.get("path"), properties.get("domain"));
				this.getCookies().put(cookie.getName(), cookie);
			} else {
				this.getProperties().put(propertyName, propertyValue);
			}
		}

		this.setBodySource(new HeaderInputStream(responseSource));
		this.setBodySink(new HeaderOutputStream(responseSink == null ? new ByteArrayOutputStream() : responseSink));
	}


	/**
	 * Writes the receiver to the given byte sink. Note that the given sink
	 * is not closed upon completion of this method!
	 * @param byteSink the byte sink to write the header to
	 * @throws IOException if there is a general I/O related problem writing
	 *    the header information
	 */
	protected void write(final OutputStream byteSink) throws IOException {
		final BufferedWriter charSink = new BufferedWriter(new OutputStreamWriter(byteSink, US_ASCII));
		charSink.write("HTTP/");
		charSink.write(Byte.toString(this.getVersion()));
		charSink.write(".");
		charSink.write(Byte.toString(this.getRevision()));
		charSink.write(' ');
		charSink.write(Short.toString(this.getType().getCode()));
		charSink.write(' ');
		charSink.write(this.getType().getComment());
		charSink.newLine();

		for (final Map.Entry<String, String> property : this.getProperties().entrySet()) {
			charSink.write(property.getKey());
			charSink.write(": ");
			charSink.write(property.getValue());
			charSink.newLine();
		}

		for (final Cookie cookie : this.getCookies().values()) {
			charSink.write("Set-Cookie: ");
			charSink.write(cookie.getName());
			charSink.write("=");
			charSink.write(cookie.getValue());

			if (cookie.getExpires() != null) {
				charSink.write("; expires=");
				charSink.write(cookie.getExpires());
			}
			if (cookie.getPath() != null) {
				charSink.write("; path=");
				charSink.write(cookie.getPath());
			}
			if (cookie.getDomain() != null) {
				charSink.write("; domain=");
				charSink.write(cookie.getDomain());
			}
			charSink.newLine();
		}
		charSink.newLine();
		charSink.flush();

		final byte[] buffer = new byte[0x10000];
		for (int bytesRead = this.getBodySource().read(buffer); bytesRead != -1; bytesRead = this.getBodySource().read(buffer)) {
			byteSink.write(buffer, 0, bytesRead);
		}
	}
}