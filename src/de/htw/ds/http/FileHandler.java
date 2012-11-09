package de.htw.ds.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import de.htw.ds.TypeMetadata;


/**
 * <p>HTTP request handler generating a response from a file resource.</p>
 */
@TypeMetadata(copyright="2008-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class FileHandler implements HttpRequestHandler {

	private final Path resourcePath;


	/**
	 * Public constructor.
	 * @param resourcePath the resource path
	 * @throws NullPointerException if the given file path is null
	 */
	public FileHandler(final Path resourcePath) {
		super();
		if (resourcePath == null) throw new NullPointerException();
		this.resourcePath = resourcePath;
	}


	/**
	 * Handles an HTTP request and generates an HTTP response.
	 * @param context the optional server context, or <tt>null</tt>
	 * @param requestHeader the HTTP request header 
	 * @param responseHeader the HTTP response header
	 * @throws NullPointerException if one of the given headers is <tt>null</tt>
	 * @throws IOException if there's an I/O related problem
	 */
	public void service(final Context context, final HttpRequestHeader requestHeader, final HttpResponseHeader responseHeader) throws IOException {
		try {
			switch (requestHeader.getType()) {
				case GET: case POST: case HEAD: {
					final long contentSize = Files.size(this.resourcePath);
					String contentType = Files.probeContentType(this.resourcePath);
					if (contentType == null) contentType = "application/octet-stream";

					responseHeader.setType(HttpResponseHeader.Type.OK);
					responseHeader.getProperties().put("Content-Type", contentType);
					responseHeader.getProperties().put("Content-Length", Long.toString(contentSize));

					if (requestHeader.getType() != HttpRequestHeader.Type.HEAD) {
						Files.copy(this.resourcePath, responseHeader.getBodyOutputStream());
					}
					break;
				}
				case PUT: {
					if (requestHeader.getProperties().containsKey("Content-Length")) {
						Files.copy(requestHeader.getBodyInputStream(), this.resourcePath, StandardCopyOption.REPLACE_EXISTING);
						responseHeader.setType(HttpResponseHeader.Type.OK);
					} else {
						responseHeader.setType(HttpResponseHeader.Type.NO_LENGTH);
					}
					break;
				}
				case DELETE: {
					Files.delete(this.resourcePath);
					responseHeader.setType(HttpResponseHeader.Type.OK);
					break;
				}
				case OPTIONS: {
					responseHeader.setType(HttpResponseHeader.Type.OK);
					responseHeader.getProperties().put("Content-Type", "text/text");
					final OutputStreamWriter writer = new OutputStreamWriter(responseHeader.getBodyOutputStream());
					writer.write("GET\nPOST\nHEAD\nPUT\nDELETE\nOPTIONS\n");
					writer.flush();
					break;
				}
				default: {
					responseHeader.setType(HttpResponseHeader.Type.BAD_METHOD);
					break;
				}
			}
		} catch (final NoSuchFileException exception) {
			responseHeader.setType(HttpResponseHeader.Type.NOT_FOUND);
		} catch (final IOException exception) {
			responseHeader.setType(HttpResponseHeader.Type.INTERNAL);
			throw exception;
		}
	}
}