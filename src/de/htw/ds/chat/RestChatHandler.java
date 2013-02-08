package de.htw.ds.chat;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import de.htw.ds.http.Context;
import de.htw.ds.http.HttpRequestHandler;
import de.htw.ds.http.HttpRequestHeader;
import de.htw.ds.http.HttpResponseHeader;
import de.sb.javase.TypeMetadata;


/**
 * </p>Java SE REST based Web-Service implementation based on a chat service.
 * For use with the HttpContainer class, the latter must be started with an additional
 * "/services/chatEntries->/de.htw.ds.chat.RestChatHandler.class" parameter that maps
 * all HTTP requests beginning with the given path prefix to the latter class!
 * Note that JAX-RS should be used to model REST Services in Java EE 6+.</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RestChatHandler implements HttpRequestHandler {
	private static final ChatService CHAT_SERVICE = new ChatService(50);
	private static final String CHATENTRY_PATTERN = "<chatEntry alias=\"%s\" content=\"%s\" timestamp=\"%s\" />\n";


	/**
	 * Handles an HTTP request and generates an HTTP response.
	 * @param context the optional server context, or <tt>null</tt>
	 * @param requestHeader the HTTP request header 
	 * @param responseHeader the HTTP response header
	 * @throws NullPointerException if one of the given headers is <tt>null</tt>
	 * @throws IOException if there's an I/O related problem
	 */
	public void service(Context context, HttpRequestHeader requestHeader, HttpResponseHeader responseHeader) throws IOException {
		final HttpResponseHeader.Type responseType = requestHeader.getType() == HttpRequestHeader.Type.POST
			? HttpResponseHeader.Type.CREATED
			: HttpResponseHeader.Type.OK;
		responseHeader.setType(responseType);

		final Writer charSink = new OutputStreamWriter(responseHeader.getBodySink());
		if (requestHeader.getType() == HttpRequestHeader.Type.OPTIONS) {
			responseHeader.getProperties().put("Content-Type", "text/text");
			charSink.write("GET\nPUT\nDELETE\nOPTIONS\n");
		} else {
			responseHeader.getProperties().put("Content-Type", "text/xml");
			charSink.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			charSink.write("<chatEntries>\n");

			final Integer chatEntryIndex = extractTrailingInteger(requestHeader.getPath());
			switch (requestHeader.getType()) {
				case GET: {
					try {
						final ChatEntry[] chatEntries = chatEntryIndex == null
							? CHAT_SERVICE.getEntries()
							: new ChatEntry[] { CHAT_SERVICE.getEntries()[chatEntryIndex] };
						for (int index = 0; index < chatEntries.length; ++index) {
							final ChatEntry chatEntry = chatEntries[index];
							charSink.write(String.format(CHATENTRY_PATTERN, chatEntry.getAlias(), chatEntry.getContent(), chatEntry.getTimestamp()));
						}
					} catch (final Exception exception) {
						// do nothing
					}
					break;
				}
				case PUT: {
					try {
						final String alias = requestHeader.getParameters().get("alias");
						final String content = requestHeader.getParameters().get("content");
						final long timestamp;
						if (chatEntryIndex == null) {
							timestamp = CHAT_SERVICE.addEntry(alias, content);
						} else {
							final ChatEntry chatEntry = CHAT_SERVICE.getEntries()[chatEntryIndex];
							CHAT_SERVICE.removeEntry(chatEntry);
							timestamp = CHAT_SERVICE.addEntry(alias, content);
						}
						charSink.write(String.format(CHATENTRY_PATTERN, alias, content, timestamp));
					} catch (final Exception exception) {
						// do nothing
					}
					break;
				}
				case DELETE: {
					try {
						if (chatEntryIndex == null) {
							final String alias = requestHeader.getParameters().get("alias");
							final String content = requestHeader.getParameters().get("content");
							final long timestamp = Long.parseLong(requestHeader.getParameters().get("timestamp"));
							final ChatEntry chatEntry = new ChatEntry(alias, content, timestamp);
							if (CHAT_SERVICE.removeEntry(chatEntry)) {
								charSink.write(String.format(CHATENTRY_PATTERN, chatEntry.getAlias(), chatEntry.getContent(), chatEntry.getTimestamp()));
							}
						} else {
							final ChatEntry chatEntry = CHAT_SERVICE.getEntries()[chatEntryIndex];
							CHAT_SERVICE.removeEntry(chatEntry);
							charSink.write(String.format(CHATENTRY_PATTERN, chatEntry.getAlias(), chatEntry.getContent(), chatEntry.getTimestamp()));
						}
					} catch (final Exception exception) {
						// do nothing
					}
					break;
				}
				default: {
					break;
				}
			}
			charSink.write("</chatEntries>\n");
		}
		charSink.flush();
	}


	/**
	 * Returns an integer if the given path ends with an integer text representation
	 * after the last slash, or null if it doesn't.
	 * @param path the path
	 * @return an integer or null
	 */
    private static Integer extractTrailingInteger(final String path) {
    	try {
    		return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    	} catch (final Exception exception) {
    		return null;
    	}
    }
}