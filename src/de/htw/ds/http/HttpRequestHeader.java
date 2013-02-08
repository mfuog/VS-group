package de.htw.ds.http;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.ProtocolException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import de.sb.javase.TypeMetadata;


/**
 * <p>HTTP request header information, see RFC 2616 for details.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class HttpRequestHeader extends HttpHeader<HttpRequestHeader.Type> {
	public static enum Type {
		HEAD, GET, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT
	};

	private String path;
	private final Map<String,String> parameters;


	/**
	 * Public constructor for use in case an HTTP request shall be sent.
	 * @param version the HTTP version
	 * @param revision the HTTP revision
	 * @param bodySource an optional body source, or <tt>null</tt>
	 * @param requestSink the request output stream
	 * @throws NullPointerException if the given path or request sink is <tt>null</tt> 
	 * @throws IllegalArgumentException if the given version or revision is
	 *    strictly negative
	 */
	public HttpRequestHeader(final byte version, final byte revision, final InputStream bodySource, final OutputStream requestSink) {
		super(version, revision);

		this.setType(Type.GET);
		this.parameters = new HashMap<>();
		this.setBodySource(new HeaderInputStream(bodySource == null? new ByteArrayInputStream(new byte[0]) : bodySource));
		this.setBodySink(new HeaderOutputStream(requestSink));
	}


	/**
	 * Public constructor for use in case an HTTP request is received.
	 * Note that the request source is stored within a wrapper limiting
	 * the content to the number of bytes defined in the "Content-Length"
	 * property, in order to allow request bodies to be processed properly.
	 * @param requestSource a request source
	 * @param requestSink an optional request sink, or <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	public HttpRequestHeader(final InputStream requestSource, final OutputStream requestSink) throws IOException {
		this(readAsciiLine(requestSource).split(" "), requestSource, requestSink);
	}


	/**
	 * Private constructor for use in case an HTTP request is received.
	 * @param requestWords a request line split into it's constituent words
	 * @param requestSource a request source
	 * @param requestSink an optional request sink, or <tt>null</tt> 
	 * @throws NullPointerException if the given request words or input stream is <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	private HttpRequestHeader(final String[] requestWords, final InputStream requestSource, final OutputStream requestSink) throws IOException {
		super(parseVersionRevision(requestWords, 2, true), parseVersionRevision(requestWords, 2, false));
		if (requestWords.length != 3 | requestWords[2].length() != 8 | !requestWords[2].startsWith("HTTP/") | requestWords[2].charAt(6) != '.') throw new ProtocolException();
		String uriCharset = "ISO-8859-1"; // Early Microsoft IE causes de-facto HTTP standard deviation!

		try {
			this.setType(Type.valueOf(requestWords[0]));
		} catch (final IllegalArgumentException exception) {
			throw new ProtocolException();
		}


		final String[] uriFragments = requestWords[1].split("\\?");
		if (uriFragments.length > 2) throw new ProtocolException();
		String parameterText = (uriFragments.length == 2) ? uriFragments[1] : "";

		// parse request properties and cookies
		for (String line = readAsciiLine(requestSource); !line.isEmpty(); line = readAsciiLine(requestSource)) {
			int colonOffset = line.indexOf(':');
			if (colonOffset == -1) throw new ProtocolException();

			final String propertyName = line.substring(0, colonOffset).trim();
			final String propertyValue = line.substring(colonOffset + 2).trim();
			if (propertyName.equals("Cookie")) {
				for (final String association : propertyValue.split(";")) {
					final String[] fragments = association.split("=");
					if (fragments.length != 2) throw new ProtocolException();

					final Cookie cookie = new Cookie(fragments[0].trim(), fragments[1].trim(), null, null, null);
					this.getCookies().put(cookie.getName(), cookie);
				}
			} else {
				this.getProperties().put(propertyName, propertyValue);	
			}
		}

		// parse request body if type=POST and contentType="application/x-www-form-urlencoded"
		if (this.getType() == Type.POST) {
			final String contentLengthText = this.getProperties().get("Content-Length");
			final String contentType = this.getProperties().get("Content-Type");

			if (contentLengthText != null & contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
				for (final String contentTypeFragment : contentType.split(";")) {
					if (contentTypeFragment.startsWith("charset=")) uriCharset = contentTypeFragment.substring(8).trim();
				}

				try {
					int totalBytesRead = 0;
					final byte[] buffer = new byte[Integer.parseInt(contentLengthText)];

					for (int bytesRead = requestSource.read(buffer); bytesRead != -1; bytesRead = requestSource.read(buffer, totalBytesRead, buffer.length - totalBytesRead)) {
						totalBytesRead += bytesRead;
						if (totalBytesRead == buffer.length) break;
					}

					final String text = new String(buffer, 0, totalBytesRead, uriCharset);
					parameterText = parameterText.isEmpty() ? text : text + '&' + parameterText;
					this.getProperties().put("Content-Length", "0");
				} catch (final Exception exception) {
					throw new ProtocolException();
				}
			}
		}

		final Map<String,String> parameters = new HashMap<String,String>();
		if (!parameterText.isEmpty()) {
			final String[] parameterTextFragments = parameterText.split("&");
			for (final String parameter : parameterTextFragments) {
				if (parameter.startsWith("_charset_=")) uriCharset = parameter.substring(10);
			}
			
			for (final String parameter : parameterTextFragments) {
				int colonOffset = parameter.indexOf('=');
				if (colonOffset == -1) {
					final String key = URLDecoder.decode(parameter, uriCharset);
					parameters.put(key, "");
				} else {
					final String key = URLDecoder.decode(parameter.substring(0, colonOffset), uriCharset);
					final String value = URLDecoder.decode(parameter.substring(colonOffset + 1), uriCharset);
					parameters.put(key, value);
				}
			}
		}

		this.path = URLDecoder.decode(uriFragments[0], uriCharset);
		this.parameters = Collections.unmodifiableMap(parameters);
		this.setBodySource(new HeaderInputStream(requestSource));
		this.setBodySink(new HeaderOutputStream(requestSink == null ? new ByteArrayOutputStream() : requestSink));
	}


	/**
	 * Returns the path.
	 * @return the HTTP request path
	 */
	public String getPath() {
		return this.path;
	}


	/**
	 * Sets the HTTP request path.
	 * @param path the HTTP request path
	 * @throws NullPointerException if the given path is <tt>null</tt>
	 */
	public void setPath(final String path) {
		if (path == null) throw new NullPointerException();
		this.path = path;
	}


	/**
	 * Returns the HTTP request parameters as an unmodifiable map.
	 * @return the parameters
	 */
	public Map<String,String> getParameters() {
		return this.parameters;
	}


	/**
	 * Writes the request header and it's associated body stream to the given sink.
	 * Note that the given byte sink is not closed upon completion of this method!
	 * @param byteSink the byte sink to write the header to
	 * @throws IOException if there is a general I/O related problem writing the
	 *    header information
	 */
	protected void write(final OutputStream byteSink) throws IOException {
		final String parameterText = parameterText(this.getParameters());
		final byte[] parameterBytes = parameterText.getBytes(US_ASCII);
		final boolean bodyParameters = this.getType() == Type.POST & "application/x-www-form-urlencoded".equals(this.getProperties().get("Content-Type"));

		final BufferedWriter charSink = new BufferedWriter(new OutputStreamWriter(byteSink, US_ASCII));
		charSink.write(this.getType().toString());
		charSink.write(' ');
		charSink.write(this.getPath());
		if (!bodyParameters) charSink.write(parameterText);
		charSink.write(" HTTP/");
		charSink.write(Byte.toString(this.getVersion()));
		charSink.write('.');
		charSink.write(Byte.toString(this.getRevision()));
		charSink.newLine();

		for (final Map.Entry<String,String> property : this.getProperties().entrySet()) {
			if (!bodyParameters | !property.getKey().equals("Content-Length")) {
				charSink.write(property.getKey());
				charSink.write(": ");
				charSink.write(property.getValue());
				charSink.newLine();
			}
		}

		if (bodyParameters) {
			charSink.write("Content-Length: ");
			charSink.write(Integer.toString(parameterBytes.length));
			charSink.newLine();
		}			

		if (!this.getCookies().isEmpty()) {
			charSink.write(cookiesText(this.getCookies()));
			charSink.newLine();
		}

		charSink.newLine();
		charSink.flush();

		if (bodyParameters) {
			byteSink.write(parameterBytes);
			byteSink.flush();
		} else {
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = this.getBodySource().read(buffer); bytesRead != -1; bytesRead = this.getBodySource().read(buffer)) {
				byteSink.write(buffer, 0, bytesRead);
			}
		}
		this.getBodySource().close();
	}


	/**
	 * Creates a text representation of the given parameters.
	 * @param parameters the parameters
	 * @return the text representation
	 */
	private static final String parameterText(final Map<String,String> parameters) {
		final StringWriter charSink = new StringWriter();
		char separator = '?';

		synchronized (parameters) {
			for (final Map.Entry<String,String> parameter : parameters.entrySet()) {
				charSink.write(separator);
				charSink.write(parameter.getKey());
				charSink.write('=');
				charSink.write(parameter.getValue());
				separator = '&';
			}
		}

		return charSink.toString();
	}


	/**
	 * Creates a text representation of the given cookies.
	 * @param parameters the cookies
	 * @return the text representation
	 */
	private static final String cookiesText(final Map<String,Cookie> cookies) {
		final StringWriter charSink = new StringWriter();
		charSink.write("Cookie");
		char separator = ' ';

		synchronized (cookies) {
			for (final Cookie cookie : cookies.values()) {
				charSink.write(separator);
				charSink.write(cookie.getName());
				charSink.write('=');
				charSink.write(cookie.getValue());
				separator = ';';
			}
		}

		return charSink.toString();
	}
}