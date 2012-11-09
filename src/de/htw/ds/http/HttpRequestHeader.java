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
import de.htw.ds.TypeMetadata;


/**
 * <p>HTTP request header information, see RFC 2616 for details.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
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
	 * @param bodyInputStream an optional body input stream, or <tt>null</tt>
	 * @param requestOutputStream the request output stream
	 * @throws NullPointerException if the given path or request output stream is <tt>null</tt> 
	 * @throws IllegalArgumentException if the given version or revision is strictly negative
	 */
	public HttpRequestHeader(final byte version, final byte revision, final InputStream bodyInputStream, final OutputStream requestOutputStream) {
		super(version, revision);

		this.setType(Type.GET);
		this.parameters = new HashMap<>();
		this.setBodyInputStream(new HeaderInputStream(bodyInputStream == null? new ByteArrayInputStream(new byte[0]) : bodyInputStream));
		this.setBodyOutputStream(new HeaderOutputStream(requestOutputStream));
	}


	/**
	 * Public constructor for use in case an HTTP request is received.
	 * Note that the input stream is stored in a version limited to the number
	 * of bytes defined in the "Content-Length" property, in order to allow
	 * the request body to be processed properly.
	 * @param requestInputStream a request input stream
	 * @param requestOutputStream an optional request output stream, or <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	public HttpRequestHeader(final InputStream requestInputStream, final OutputStream requestOutputStream) throws IOException {
		this(readAsciiLine(requestInputStream).split(" "), requestInputStream, requestOutputStream);
	}


	/**
	 * Private constructor for use in case an HTTP request is received.
	 * @param requestWords a request line split into it's constituent words
	 * @param requestInputStream a request input stream
	 * @param requestOutputStream an optional request output stream, or <tt>null</tt> 
	 * @throws NullPointerException if the given request words or input stream is <tt>null</tt> 
	 * @throws IOException in case there is an I/O related problem
	 */
	private HttpRequestHeader(final String[] requestWords, final InputStream requestInputStream, final OutputStream requestOutputStream) throws IOException {
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
		for (String line = readAsciiLine(requestInputStream); !line.isEmpty(); line = readAsciiLine(requestInputStream)) {
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
					final byte[] buffer = new byte[Integer.parseInt(contentLengthText)];

					int totalBytesRead = 0;
					for (int bytesRead = requestInputStream.read(buffer); bytesRead != -1; bytesRead = requestInputStream.read(buffer, totalBytesRead, buffer.length - totalBytesRead)) {
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
		this.setBodyInputStream(new HeaderInputStream(requestInputStream));
		this.setBodyOutputStream(new HeaderOutputStream(requestOutputStream == null ? new ByteArrayOutputStream() : requestOutputStream));
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
	 * Writes the request header and it's associated body stream to the given output stream.
	 * Note that the given output stream is not closed upon completion of this method!
	 * @param outputStream the output stream to write the header to
	 * @throws IOException if there is a general I/O related problem writing the header information
	 */
	protected void write(final OutputStream outputStream) throws IOException {
		final String parameterText = parameterText(this.getParameters());
		final byte[] parameterBytes = parameterText.getBytes(US_ASCII);
		final boolean bodyParameters = this.getType() == Type.POST & "application/x-www-form-urlencoded".equals(this.getProperties().get("Content-Type"));

		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, US_ASCII));
		writer.write(this.getType().toString());
		writer.write(' ');
		writer.write(this.getPath());
		if (!bodyParameters) writer.write(parameterText);
		writer.write(" HTTP/");
		writer.write(Byte.toString(this.getVersion()));
		writer.write('.');
		writer.write(Byte.toString(this.getRevision()));
		writer.newLine();

		for (final Map.Entry<String,String> property : this.getProperties().entrySet()) {
			if (!bodyParameters | !property.getKey().equals("Content-Length")) {
				writer.write(property.getKey());
				writer.write(": ");
				writer.write(property.getValue());
				writer.newLine();
			}
		}

		if (bodyParameters) {
			writer.write("Content-Length: ");
			writer.write(Integer.toString(parameterBytes.length));
			writer.newLine();
		}			

		if (!this.getCookies().isEmpty()) {
			writer.write(cookiesText(this.getCookies()));
			writer.newLine();
		}

		writer.newLine();
		writer.flush();

		if (bodyParameters) {
			outputStream.write(parameterBytes);
			outputStream.flush();
		} else {
			final byte[] buffer = new byte[0x10000];
			for (int bytesRead = this.getBodyInputStream().read(buffer); bytesRead != -1; bytesRead = this.getBodyInputStream().read(buffer)) {
				outputStream.write(buffer, 0, bytesRead);
			}
		}
		this.getBodyInputStream().close();
	}


	/**
	 * Creates a text representation of the given parameters.
	 * @param parameters the parameters
	 * @return the text representation
	 */
	private static final String parameterText(final Map<String,String> parameters) {
		final StringWriter writer = new StringWriter();
		char separator = '?';

		synchronized (parameters) {
			for (final Map.Entry<String,String> parameter : parameters.entrySet()) {
				writer.write(separator);
				writer.write(parameter.getKey());
				writer.write('=');
				writer.write(parameter.getValue());
				separator = '&';
			}
		}

		return writer.toString();
	}


	/**
	 * Creates a text representation of the given cookies.
	 * @param parameters the cookies
	 * @return the text representation
	 */
	private static final String cookiesText(final Map<String,Cookie> cookies) {
		final StringWriter writer = new StringWriter();
		writer.write("Cookie");
		char separator = ' ';

		synchronized (cookies) {
			for (final Cookie cookie : cookies.values()) {
				writer.write(separator);
				writer.write(cookie.getName());
				writer.write('=');
				writer.write(cookie.getValue());
				separator = ';';
			}
		}

		return writer.toString();
	}
}