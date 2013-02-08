package de.htw.ds.tcp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.Format;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
 * <p>TCP monitor pane for use within the TcpMonitor2 class.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class TcpMonitorPane extends JPanel implements TcpMonitor.Watcher {
	private static final long serialVersionUID = 1L;
	private static final Icon ICON_START, ICON_SUSPEND, ICON_RESUME, ICON_STOP, ICON_CLEAR;
	private static final String[] RECORD_COLUMN_NAMES = {"identity", "openTimestamp", "closeTimestamp", "requestLength", "responseLength"};
	private static final String[] RECORD_HEADER_NAMES = {"ID", "Opened", "Closed", "Request Bytes", "Response Bytes"};
	private static final int[] RECORD_COLUMN_WIDTH = { 60, 40, 40, 30, 30 };
	private static final Format[] RECORD_COLUMN_FORMATS = {
		null,
		DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
		DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
		null,
		null
	};

	static {
		try {
			// Load some Eclipse standard icons, published under EPL - http://www.eclipse.org/legal/epl-v10.html
			ICON_START = new ImageIcon(ImageIO.read(TcpMonitorPane.class.getResourceAsStream("start_task.gif")));
			ICON_SUSPEND = new ImageIcon(ImageIO.read(TcpMonitorPane.class.getResourceAsStream("pause.gif")));
			ICON_RESUME = new ImageIcon(ImageIO.read(TcpMonitorPane.class.getResourceAsStream("restart_task.gif")));
			ICON_STOP = new ImageIcon(ImageIO.read(TcpMonitorPane.class.getResourceAsStream("progress_stop.gif")));
			ICON_CLEAR = new ImageIcon(ImageIO.read(TcpMonitorPane.class.getResourceAsStream("trash.gif")));
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}


	private final JTextComponent messageField;
	private final AbstractButton startButton;
	private final AbstractButton clearButton;
	private final BeanTableModel<TcpMonitorRecord> recordTableModel;
	private AutoCloseable monitor;

	/**
	 * Creates a new instance.
	 */
	public TcpMonitorPane() {
		super();
		this.setLayout(new SpringLayout());

		this.messageField = new JTextField();
		this.startButton = new JButton(ICON_START);
		this.clearButton = new JButton(ICON_CLEAR);
		this.recordTableModel = new BeanTableModel<>(TcpMonitorRecord.class);
		this.monitor = null;
		this.messageField.setEditable(false);
		this.clearButton.setEnabled(false);

		final Container statusPane, messagePane;
		final JTable recordTable;
		final AbstractButton stopButton;
		final JTextComponent servicePortField, forwardHostField, forwardPortField, requestArea = new JTextArea(), responseArea = new JTextArea();
		{
			recordTable = new JTable(this.recordTableModel, createRecordColumnModel(this.recordTableModel));
			recordTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			recordTable.setShowGrid(false);
			requestArea.setEditable(false);
			responseArea.setEditable(false);

			final JSplitPane recordSplitPane, textAreaSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(requestArea), new JScrollPane(responseArea));
			this.add(statusPane = new JPanel(new SpringLayout()));
			this.add(recordSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, new JScrollPane(recordTable), textAreaSplitPane));
			this.add(messagePane = new JPanel(new SpringLayout()));
			textAreaSplitPane.setResizeWeight(0.5);
			recordSplitPane.setDividerLocation(120);
			recordSplitPane.setResizeWeight(0.25);
			recordSplitPane.setBorder(new LineBorder(Color.LIGHT_GRAY));

			new SpringSpread(this, 2, -2).alignWest(statusPane, recordSplitPane, messagePane);
			new SpringSpread(this, 2, null).alignNorth(statusPane);
			new SpringSpread(statusPane, 2, null).alignNorth(recordSplitPane);
			new SpringSpread(messagePane, 2, null).alignSouth(recordSplitPane);
			new SpringSpread(this, 2, null).alignSouth(messagePane);
		}

		{
			final Component servicePortLabel, forwardHostLabel, forwardPortLabel;
			final InputVerifier portVerifier = new PortVerifier();
			statusPane.add(servicePortLabel = new JLabel("service port"));
			statusPane.add(servicePortField = new JTextField("8010"));
			statusPane.add(forwardHostLabel = new JLabel("forward host"));
			statusPane.add(forwardHostField = new JTextField("localhost"));
			statusPane.add(forwardPortLabel = new JLabel("port"));
			statusPane.add(forwardPortField = new JTextField("80"));
			statusPane.add(this.startButton);
			statusPane.add(stopButton = new JButton(ICON_STOP));
			statusPane.add(this.clearButton);
			servicePortField.setInputVerifier(portVerifier);
			forwardPortField.setInputVerifier(portVerifier);
			stopButton.setEnabled(false);

			new SpringSpread(statusPane, 0, null).alignNorth(servicePortField, forwardHostField, forwardPortField);
			new SpringSpread(servicePortField, 0, null).alignSouth(statusPane).alignVertical(servicePortLabel, forwardHostLabel, forwardPortLabel, startButton, stopButton, clearButton);
			new SpringSpread(statusPane, 0, null).alignWest(servicePortLabel);
			new SpringSpread(servicePortLabel, 5, 60).alignWest(servicePortField);
			new SpringSpread(servicePortField, 15, null).alignWest(forwardHostLabel);
			new SpringSpread(forwardHostLabel, 5, null).alignWest(forwardHostField);
			new SpringSpread(forwardHostField, 5, null).alignWest(forwardPortLabel);
			new SpringSpread(forwardPortLabel, 5, 60).alignWest(forwardPortField);
			new SpringSpread(forwardPortField, 15, null).alignWest(this.startButton);
			new SpringSpread(this.startButton, 5, null).alignWest(stopButton);
			new SpringSpread(stopButton, 5, null).alignWest(clearButton);
			new SpringSpread(clearButton, 0, null).alignEast(statusPane);
		}

		{
			final Component messageLabel = new JLabel("Message");
			messageLabel.setForeground(Color.RED);
			messagePane.add(messageLabel);
			messagePane.add(this.messageField);

			new SpringSpread(messagePane, 0, null).alignNorth(this.messageField);
			new SpringSpread(this.messageField, 0, null).alignSouth(messagePane).alignVertical(messageLabel);
			new SpringSpread(messagePane, 0, null).alignWest(messageLabel);
			new SpringSpread(messageLabel, 5, 0).alignWest(this.messageField);
		}

		final ListSelectionListener selectionAction = new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;

				final int rowIndex = recordTable.getSelectedRow();
				if (rowIndex == -1) {
					requestArea.setText("");
					responseArea.setText("");
				} else {
					final TcpMonitorRecord record = TcpMonitorPane.this.recordTableModel.getRow(rowIndex);
					final String requestText = new String(record.getRequestData(), Charset.forName("ASCII"));
					final String responseText = new String(record.getResponseData(), Charset.forName("ASCII"));

					requestArea.setText(requestText);
					requestArea.setCaretPosition(0);
					responseArea.setText(responseText);
					responseArea.setCaretPosition(0);
				}
			}
		};
		recordTable.getSelectionModel().addListSelectionListener(selectionAction);

		final ActionListener startAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				TcpMonitorPane.this.setMessage(null);
				if (TcpMonitorPane.this.monitor == null) {
					try {
						final int servicePort = Integer.parseInt(servicePortField.getText());
						final int forwardPort = Integer.parseInt(forwardPortField.getText());
						final String forwardHost = forwardHostField.getText();
						final InetSocketAddress forwardAddress = new InetSocketAddress(forwardHost, forwardPort);
						TcpMonitorPane.this.monitor = new TcpMonitor(servicePort, forwardAddress, TcpMonitorPane.this);
						stopButton.setEnabled(true);
					} catch (final Exception exception) {
						TcpMonitorPane.this.setMessage(exception);
						return;
					}
				}

				final boolean active = TcpMonitorPane.this.startButton.getIcon() == ICON_SUSPEND;
				TcpMonitorPane.this.startButton.setIcon(active ? ICON_RESUME : ICON_SUSPEND);
			}
		};
		this.startButton.addActionListener(startAction);

		final ActionListener stopAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				TcpMonitorPane.this.setMessage(null);
				TcpMonitorPane.this.disconnect();
				stopButton.setEnabled(false);
			}
		};
		stopButton.addActionListener(stopAction);


		final ActionListener clearAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				TcpMonitorPane.this.setMessage(null);
				TcpMonitorPane.this.recordTableModel.removeRows();
				TcpMonitorPane.this.clearButton.setEnabled(false);
			}
		};
		this.clearButton.addActionListener(clearAction);
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
	 * {@inheritDoc}
	 */
	public void recordCreated(final TcpMonitorRecord record) {
		if (this.startButton.getIcon() == ICON_SUSPEND) {
			this.recordTableModel.addRow(record);
			this.clearButton.setEnabled(true);
			this.setMessage(null);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public void exceptionCatched(final Exception exception) {
		if (this.startButton.getIcon() == ICON_SUSPEND) {
			this.setMessage(exception);
		}
	}


	/**
	 * Closes and discards this pane's TCP monitor, and sets the activity
	 * state to inactive.
	 */
	public void disconnect() {
		try { this.monitor.close(); } catch (final Exception exception) {}
		this.monitor = null;
		this.startButton.setIcon(ICON_START);
	}


	/**
	 * Creates a new column model for the record table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	private static TableColumnModel createRecordColumnModel(final BeanTableModel<TcpMonitorRecord> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < RECORD_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(RECORD_COLUMN_NAMES[headerIndex]);

			final TableCellFormater formater = new TableCellFormater(RECORD_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, RECORD_COLUMN_WIDTH[headerIndex]);
			column.setHeaderValue(RECORD_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}


	/**
	 * Input verifier for port values stored in text fields.
	 */
	private static class PortVerifier extends InputVerifier {
		@Override
		public boolean verify(final JComponent component) {
			if (!(component instanceof JTextComponent)) return false;
			final String text = ((JTextComponent) component).getText();
			try {
				final int port = Integer.parseInt(text);
				return port >= 0 & port <= 0xFFFF;
			} catch (final NumberFormatException exception) {
				return false;
			}
		}
	};


	/**
	 * Application entry point.
	 * @param args the given runtime arguments
	 */
	public static void main(final String[] args) {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (final Exception exception) {
			// do nothing
		}

		final TcpMonitorPane contentPane = new TcpMonitorPane();
		final JFrame frame = new JFrame("TCP Monitor");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(contentPane);
		frame.pack();
		frame.setSize(800, 600);
		frame.setVisible(true);

		final WindowListener windowListener = new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent event) {
				try { contentPane.disconnect(); } catch (final Exception exception) {}
			}
		};
		frame.addWindowListener(windowListener);
	}
}