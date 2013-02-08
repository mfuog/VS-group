package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import de.sb.javase.TypeMetadata;


/**
 * <p>ChatPanel based bottom-up client class using a JAX-WS web-service.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RpcChatClient {

	private final ChatPane pane;
	private final RpcChatService proxy;


	/**
	 * Public constructor.
	 * @param proxy the chat service proxy
	 * @throws NullPointerException if the given proxy is null
	 */
	public RpcChatClient(final RpcChatService proxy) {
		super();
		if (proxy == null) throw new NullPointerException();

		this.proxy = proxy;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RpcChatClient.this.addChatEntry();
				RpcChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RpcChatClient.this.refreshChatEntries();
			}
		};
		this.pane = new ChatPane(sendButtonActionListener, refreshButtonActionListener);

		this.refreshChatEntries();
     }


	/**
	 * Returns the chat pane.
	 * @return the chat pane
	 */
	public ChatPane getPane() {
		return this.pane;
	}


	/**
	 * Adds a chat entry to the chat service.
	 */
	public void addChatEntry() {
		try {
			this.proxy.addEntry(this.pane.getAlias(), this.pane.getContent());
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Refreshes the chat entries from the chat service.
	 */
	public void refreshChatEntries() {
		try {
			final ChatEntry[] chatEntries = this.proxy.getEntries();
			this.pane.setChatEntries(chatEntries);
			this.pane.setMessage(null);
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Application entry point. The given runtime parameters must be a SOAP service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 * @throws MalformedURLException if the given URI cannot be converted into a URL
	 */
	public static void main(final String[] args) throws URISyntaxException, MalformedURLException {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final URL wsdlLocator = new URL(args[0] + "?wsdl");
		final Service proxyFactory = Service.create(wsdlLocator, new QName("http://chat.ds.htw.de/", RpcChatService.class.getSimpleName()));
		final RpcChatService proxy = proxyFactory.getPort(RpcChatService.class);

		final RpcChatClient client = new RpcChatClient(proxy);
		final JFrame frame = new JFrame("Dynamic (bottom-up) JAX-WS Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getPane());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}