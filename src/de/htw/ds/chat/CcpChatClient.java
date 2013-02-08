package de.htw.ds.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.swing.JFrame;
import javax.swing.UIManager;
import de.sb.javase.TypeMetadata;
import de.sb.javase.io.SocketAddress;


/**
 * <p>ChatPanel based client class using a custom protocol based service.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class CcpChatClient {

	private final ChatPane pane;
	private final CcpChatStub stub;


	/**
	 * Public constructor.
	 * @param stub the chat service stub
	 * @throws NullPointerException if the given stub is null
	 */
	public CcpChatClient(final CcpChatStub stub) {
		super();
		if (stub == null) throw new NullPointerException();

		this.stub = stub;

		final ActionListener sendButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				CcpChatClient.this.addChatEntry();
				CcpChatClient.this.refreshChatEntries();
			}
		};
		final ActionListener refreshButtonActionListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				CcpChatClient.this.refreshChatEntries();
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
	 * Returns the chat pane.
	 * @return the chat pane.
	 */
	public ChatPane getPane() {
		return this.pane;
	}


	/**
	 * Application entry point. The given runtime parameters must be a socket-address.
	 * @param args the given runtime arguments
	 * @throws IOException if there is an I/O related problem
	 */
	public static void main(final String[] args) throws IOException {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final InetSocketAddress serviceAddress = new SocketAddress(args[0]).toInetSocketAddress();
		final CcpChatStub chatServiceProxy = new CcpChatStub(serviceAddress);
		final CcpChatClient client = new CcpChatClient(chatServiceProxy);

		final JFrame frame = new JFrame("CCP Chat Client");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(client.getPane());
		frame.pack();
		frame.setSize(640, 480);
		frame.setVisible(true);
	}
}