package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.JFrame;
import javax.swing.UIManager;
import de.sb.javase.TypeMetadata;


/**
 * <p>ChatPanel based client class using a Java-RMI service.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class RmiChatClient {

	private final ChatPane pane;
	private final Registry serviceRegistry;
	private final String serviceName;
	private transient RmiChatService proxy;


	/**
	 * Public constructor.
	 * @param serviceURI the RMI service URI
	 * @throws NullPointerException if the given service URI is null
	 */
	public RmiChatClient(final URI serviceURI) {
		super();

		try {
			this.serviceRegistry = LocateRegistry.getRegistry(serviceURI.getHost(), serviceURI.getPort());
		} catch (final RemoteException exception) {
			throw new AssertionError();
		}
		this.serviceName = serviceURI.getPath().substring(1);

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RmiChatClient.this.addChatEntry();
				RmiChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				RmiChatClient.this.refreshChatEntries();
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
			try {
				if (this.proxy == null) throw new RemoteException();
				this.proxy.addEntry(this.pane.getAlias(), this.pane.getContent());
			} catch (final RemoteException exception) {
				this.proxy = (RmiChatService) this.serviceRegistry.lookup(this.serviceName);
				this.proxy.addEntry(this.pane.getAlias(), this.pane.getContent());
			}
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Refreshes the chat entries from the chat service.
	 */
	public void refreshChatEntries() {
		try {
			ChatEntry[] chatEntries;
			try {
				if (this.proxy == null) throw new RemoteException();
				chatEntries = this.proxy.getEntries();
			} catch (final RemoteException exception) {
				this.proxy = (RmiChatService) this.serviceRegistry.lookup(this.serviceName);
				chatEntries = this.proxy.getEntries();
			}

			this.pane.setChatEntries(chatEntries);
			this.pane.setMessage(null);
		} catch (final Exception exception) {
			this.pane.setMessage(exception);
		}
	}


	/**
	 * Application entry point. The given runtime parameter must be an RMI service URI.
	 * @param args the given runtime arguments
	 * @throws URISyntaxException if the given URI is malformed
	 */
	public static void main(final String[] args) throws URISyntaxException {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final URI serviceURI = new URI(args[0]);

		final RmiChatClient client = new RmiChatClient(serviceURI);
		final JFrame frame = new JFrame("RMI Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getPane());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}