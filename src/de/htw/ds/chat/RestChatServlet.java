package de.htw.ds.chat;

import de.sb.javase.TypeMetadata;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * </p>Java EE REST based Web-Service implementation based on a chat service.
 * Invocation should be mapped to URL paths "/services/chatEntries" and
 * "/services/chatEntries/*".</p>
 * <p>Note that the project needs the JAVA EE interfaces library "javaee.jar"
 * within it's classpath to compile this class correctly. Also, note that in
 * Java EE the application name precedes URL paths during invocation.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RestChatServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final ChatService CHAT_SERVICE = new ChatService(50);
	public static enum MethodType { HEAD, GET, POST, PUT, DELETE, TRACE, OPTIONS, CONNECT };


	/**
	 * Handles the given HTTP request, and writes response into the given HTTP response object.
	 * @param request the HTTP request object
	 * @param response the HTTP response object
	 * @throws ServletException if 
	 * @throws IOException if there is an I/O related problem
	 */
	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/xml");

		// first write to char sink commits response object, implicitly
		// triggering HTTP response header generation with code 200.
		final Writer charSink = response.getWriter();
		charSink.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		charSink.write("<chatEntries>\n");

		final Integer chatEntryIndex = extractTrailingInteger(request.getRequestURI());
		switch (MethodType.valueOf(request.getMethod().toUpperCase())) {
			case GET: {
				try {
					final ChatEntry[] chatEntries = chatEntryIndex == null
						? CHAT_SERVICE.getEntries()
						: new ChatEntry[] { CHAT_SERVICE.getEntries()[chatEntryIndex] };
					for (int index = 0; index < chatEntries.length; ++index) {
						final ChatEntry chatEntry = chatEntries[index];
						charSink.write("<chatEntry alias=\"" + chatEntry.getAlias() + "\" content=\"" + chatEntry.getContent() + "\" timestamp=\"" + chatEntry.getTimestamp() + "\" />\n");
					}
				} catch (final Exception exception) {
					// do nothing
				}
				break;
			}
			case PUT: {
				try {
					final String alias = request.getParameter("alias");
					final String content = request.getParameter("content");
					final long timestamp;
					if (chatEntryIndex == null) {
						timestamp = CHAT_SERVICE.addEntry(alias, content);
					} else {
						final ChatEntry chatEntry = CHAT_SERVICE.getEntries()[chatEntryIndex];
						CHAT_SERVICE.removeEntry(chatEntry);
						timestamp = CHAT_SERVICE.addEntry(alias, content);
					}
					charSink.write("<chatEntry alias=\"" + alias + "\" content=\"" + content + "\" timestamp=\"" + timestamp + "\" />\n");
				} catch (final Exception exception) {
					// do nothing
				}
				break;
			}
			case DELETE: {
				try {
					if (chatEntryIndex == null) {
						final String alias = request.getParameter("alias");
						final String content = request.getParameter("content");
						final long timestamp = Long.parseLong(request.getParameter("timestamp"));
						final ChatEntry chatEntry = new ChatEntry(alias, content, timestamp);
						if (CHAT_SERVICE.removeEntry(chatEntry)) {
							charSink.write("<chatEntry alias=\"" + chatEntry.getAlias() + "\" content=\"" + chatEntry.getContent() + "\" timestamp=\"" + chatEntry.getTimestamp() + "\" />\n");
						}
					} else {
						final ChatEntry chatEntry = CHAT_SERVICE.getEntries()[chatEntryIndex];
						CHAT_SERVICE.removeEntry(chatEntry);
						charSink.write("<chatEntry alias=\"" + chatEntry.getAlias() + "\" content=\"" + chatEntry.getContent() + "\" timestamp=\"" + chatEntry.getTimestamp() + "\" />\n");
					}
				} catch (final Exception exception) {
					// do nothing
				}
				break;
			}
			default: {
				// do nothing
			}
		}
		charSink.write("</chatEntries>\n");
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