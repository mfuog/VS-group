package de.htw.ds.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.DateFormat;
import java.text.Format;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;
import de.sb.javase.Classes;
import de.sb.javase.TypeMetadata;
import de.sb.javase.swing.BeanTableModel;
import de.sb.javase.swing.SpringSpread;
import de.sb.javase.swing.TableCellFormater;


/**
 * <p>Swing based chat pane. Note the use of a SpringLayout
 * for professional resizing behavior.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ChatPane extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Icon ICON_SEND, ICON_REFRESH;
	private static final String[] CHAT_ENTRY_COLUMN_NAMES = {"alias", "content", "timestamp"};
	private static final String[] CHAT_ENTRY_HEADER_NAMES = {"Sender", "Message", "Created"};
	private static final Format[] CHAT_ENTRY_COLUMN_FORMATS = {
		null,
		null,
		DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
		null
	};

	static {
		try {
			// Load some Eclipse standard icons, published under EPL - http://www.eclipse.org/legal/epl-v10.html
			ICON_SEND = new ImageIcon(ImageIO.read(ChatPane.class.getResourceAsStream("nav_forward.gif")));
			ICON_REFRESH = new ImageIcon(ImageIO.read(ChatPane.class.getResourceAsStream("nav_refresh.gif")));
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}


	private final BeanTableModel<ChatEntry> chatTableModel;
	private final JTextComponent aliasField;
	private final JTextField contentField;
	private final JTextComponent messageField;

	/**
	 * Public constructor.
	 * @param sendActionListener the listener adding new chat entries
	 * @param refreshActionListener the listener refreshing the chat area
	 */
	public ChatPane(final ActionListener sendActionListener, final ActionListener refreshActionListener) {
		super();

		final SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		final Component aliasLabel, contentLabel, messageLabel, chatEntryScrollPane;
		final AbstractButton sendButton, refreshButton;

		this.chatTableModel = new BeanTableModel<>(ChatEntry.class);
		final JTable chatEntryTable = new JTable(this.chatTableModel, createChatColumnModel(this.chatTableModel));
		chatEntryTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chatEntryTable.setShowGrid(false);

		this.add(aliasLabel = new JLabel("Alias"));
		this.add(this.aliasField = new JTextField(20));
		this.add(contentLabel = new JLabel("Text"));
		this.add(this.contentField = new JTextField(20));
		this.add(sendButton = new JButton(ICON_SEND));
		this.add(refreshButton = new JButton(ICON_REFRESH));
		this.add(messageLabel = new JLabel("Message"));
		this.add(this.messageField = new JTextField());
		this.add(chatEntryScrollPane = new JScrollPane(chatEntryTable));

		this.aliasField.setText(ManagementFactory.getRuntimeMXBean().getName());
		this.messageField.setEditable(false);
		messageLabel.setForeground(Color.RED);
		sendButton.addActionListener(sendActionListener);
		refreshButton.addActionListener(refreshActionListener);

		new SpringSpread(this, 2, null).alignNorth(this.aliasField);
		new SpringSpread(this.aliasField, 0, null).alignVertical(aliasLabel, contentLabel, this.contentField, refreshButton, sendButton);
		new SpringSpread(this, 2, null).alignWest(aliasLabel);
		new SpringSpread(aliasLabel, 5, null).alignWest(this.aliasField);
		new SpringSpread(this.aliasField, 15, null).alignWest(contentLabel);
		new SpringSpread(contentLabel, 5, null).alignWest(this.contentField);
		new SpringSpread(this.contentField, 15, null).alignWest(sendButton);
		new SpringSpread(sendButton, 5, null).alignWest(refreshButton);
		new SpringSpread(refreshButton, -2, null).alignEast(this);

		new SpringSpread(this.aliasField, 2, null).alignNorth(chatEntryScrollPane);
		new SpringSpread(this.messageField, 2, null).alignSouth(chatEntryScrollPane);
		new SpringSpread(this, 2, -2).alignWest(chatEntryScrollPane);

		new SpringSpread(this, 2, null).alignSouth(this.messageField);
		new SpringSpread(this.messageField, 0, null).alignVertical(messageLabel);
		new SpringSpread(this, 2, null).alignWest(messageLabel);
		new SpringSpread(messageLabel, 2, -2).alignWest(this.messageField);

		final ActionListener contentListener = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				sendButton.doClick();
			}
		};
		this.contentField.addActionListener(contentListener);
	}


	/**
	 * Returns the alias.
	 * @return the alias
	 */
	public String getAlias() {
		return this.aliasField.getText();
	}


	/**
	 * Returns the content.
	 * @return the content
	 */
	public String getContent() {
		return this.contentField.getText();
	}


	/**
	 * Sets the message field text based on the given exception.
	 * @param exception the exception
	 */
	public void setMessage(final Throwable exception) {
		if (exception == null) {
			this.messageField.setText("");
		} else {
			final Throwable rootCause = Classes.rootCause(exception);
			this.messageField.setText(String.format("%s: %s", rootCause.getClass().getSimpleName(), rootCause.getMessage()));
		}
	}


	/**
	 * Sets the chat entries
	 * @param chatEntries the chat entries
	 */
	public void setChatEntries(final ChatEntry[] chatEntries) {
		this.chatTableModel.removeRows();
		this.chatTableModel.addRows(chatEntries);
	}


	/**
	 * Creates a new column model for the chat table.
	 * @param tableModel the chat table model
	 * @return the corresponding column model
	 */
	private static TableColumnModel createChatColumnModel(final BeanTableModel<ChatEntry> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < CHAT_ENTRY_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(CHAT_ENTRY_COLUMN_NAMES[headerIndex]);

			final TableCellFormater formater = new TableCellFormater(CHAT_ENTRY_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex);
			column.setHeaderValue(CHAT_ENTRY_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);

			result.addColumn(column);
		}
		return result;
	}
}