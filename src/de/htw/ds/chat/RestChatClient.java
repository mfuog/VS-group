package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JFrame;
import javax.swing.UIManager;
import de.sb.javase.TypeMetadata;


/**
 * <p>ChatPanel based client class using a REST web-service.
 * Note that REST proxies can be auto-generated using JAX-RS (Java EE 6+).</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RestChatClient {

	private final ChatPane pane;
	private final RestChatProxy stub;


	/**
	 * Public constructor.
	 * @param stub the chat service stub
	 * @throws NullPointerException if the given stub is null
	 */
	public RestChatClient(final RestChatProxy stub) {
		super();
		if (stub == null) throw new NullPointerException();

		this.stub = stub;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RestChatClient.this.addChatEntry();
				RestChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RestChatClient.this.refreshChatEntries();
			}
		};
		this.pane = new ChatPane(sendButtonActionListener, refreshButtonActionListener);

		this.refreshChatEntries();
      }


	/**
	 * Adds a chat entry to the chat service.
	 */
	public void addChatEntry() {
		try {
			this.stub.addEntry(this.pane.getAlias(), this.pane.getContent());
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Refreshes the chat entries from the chat service.
	 */
	public void refreshChatEntries() {
		try {
			final ChatEntry[] chatEntries = this.stub.getEntries();
			this.pane.setChatEntries(chatEntries);
			this.pane.setMessage(null);
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Returns the pane.
	 * @return the pane.
	 */
	public ChatPane getPane() {
		return this.pane;
	}


	/**
	 * Application entry point. The given runtime parameters must be a REST service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URL is malformed
	 */
	public static void main(final String[] args) throws URISyntaxException {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final URI serviceURI = new URI(args[0]);
		final RestChatProxy chatServiceProxy = new RestChatProxy(serviceURI);
		final RestChatClient client = new RestChatClient(chatServiceProxy);

		final JFrame frame = new JFrame("REST Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getPane());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}