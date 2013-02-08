package de.htw.ds.chat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import de.sb.javase.TypeMetadata;


/**
 * <p>Chat service stub for REST style web-service access.
 * Note that such proxies can be auto-generated using JAX-RS (Java EE 6+).</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RestChatProxy {
	private static final DocumentBuilder DOCUMENT_BUILDER;
	static {
		try {
			DOCUMENT_BUILDER = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (final ParserConfigurationException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private final URI serviceURI;

	/**
	 * Public constructor
	 * @param serviceURI the service URI
	 * @throws NullPointerException if the given serviceURI is null
	 */
	public RestChatProxy(final URI serviceURI) {
		if (serviceURI == null) throw new NullPointerException();
		this.serviceURI = serviceURI;
	}


	/**
	 * Delegates the addEntry method to the server.
	 * @param alias the alias
	 * @param content the content
	 * @return the creation timestamp
	 * @throws IOException if an I/O related problem occurs
	 * @throws SAXException 
	 */
	public long addEntry(final String alias, final String content) throws IOException {
		final URL url = new URL(this.serviceURI.toASCIIString() + "?alias=" + alias + "&content=" + content);
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");

		try (InputStream byteSource = (InputStream) connection.getContent()) {
			final Document document = DOCUMENT_BUILDER.parse(byteSource);
			final ChatEntry[] chatEntries = RestChatProxy.unmarshallChatEntries(document);
			return chatEntries[0].getTimestamp();
		} catch (final SAXException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Delegates the removeEntry method to the server.
	 * @param entry the chat entry
	 * @throws IOException if an I/O related problem occurs
	 */
	public boolean removeEntry(final ChatEntry entry) throws IOException {
		final URL url = new URL(this.serviceURI.toASCIIString() + "?user=" + entry.getAlias() + "&content=" + entry.getContent() + "&timestamp=" + entry.getTimestamp());
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("DELETE");

		try (InputStream byteSource = (InputStream) connection.getContent()) {
			final Document document = DOCUMENT_BUILDER.parse(byteSource);
			final ChatEntry[] chatEntries = RestChatProxy.unmarshallChatEntries(document);
			return chatEntries.length == 1;
		} catch (final SAXException exception) {
			throw new AssertionError();
		}
	}
		

	/**
	 * Delegates the getEntries method to the server.
	 * @return the chat entries
	 * @throws IOException if an I/O related problem occurs
	 */
	public ChatEntry[] getEntries() throws IOException {
		final URL url = new URL(this.serviceURI.toASCIIString());
		final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		try (InputStream byteSource = (InputStream) connection.getContent()) {
			final Document document = DOCUMENT_BUILDER.parse(byteSource);
			return RestChatProxy.unmarshallChatEntries(document);
		} catch (final SAXException exception) {
			throw new AssertionError();
		}
	}


	/**
	 * Returns the chat entries unmarshalled from the given DOM.
	 * @param document the DOM
	 * @return the chat entries
	 */
	private static ChatEntry[] unmarshallChatEntries(final Document document) {
		final List<ChatEntry> chatEntries = new ArrayList<ChatEntry>();
		final NodeList nodes = document.getDocumentElement().getElementsByTagName("chatEntry");
		for (int index = 0; index < nodes.getLength(); ++index) {
			final Node node = nodes.item(index);
			final NamedNodeMap map = node.getAttributes();

			final String alias = map.getNamedItem("alias").getNodeValue();
			final String content = map.getNamedItem("content").getNodeValue();
			final long timestamp = Long.parseLong(map.getNamedItem("timestamp").getNodeValue());
			chatEntries.add(new ChatEntry(alias, content, timestamp));
		}
		return chatEntries.toArray(new ChatEntry[0]);
	}
}