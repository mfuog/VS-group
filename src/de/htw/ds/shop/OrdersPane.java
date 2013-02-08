package de.htw.ds.shop;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import de.sb.javase.TypeMetadata;
import de.sb.javase.swing.BeanTableModel;
import de.sb.javase.swing.ScaleFormat;
import de.sb.javase.swing.SpringSpread;
import de.sb.javase.swing.TableCellFormater;


/**
 * <p>Orders pane for use within the ShopClient2 class.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class OrdersPane extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String[] ORDER_COLUMN_NAMES = {"identity", "creationTimestamp", "taxRate", "itemCount", "grossPrice", "tax", "netPrice"};
	private static final String[] ORDER_HEADER_NAMES = {"ID", "Created", "Rate", "Items", "Gross", "Tax", "Net"};
	private static final String[] ITEM_COLUMN_NAMES = {"identity", "articleIdentity", "articleGrossPrice", "count"};
	private static final String[] ITEM_HEADER_NAMES = {"ID", "Article", "Gross", "Units"};
	private static final Format[] ORDER_COLUMN_FORMATS = {
		NumberFormat.getIntegerInstance(Locale.ROOT),
		DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM),
		NumberFormat.getPercentInstance(),
		NumberFormat.getIntegerInstance(Locale.ROOT),
		new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
		new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
		new ScaleFormat(NumberFormat.getCurrencyInstance(), -2)
	};
	private static final Format[] ITEM_COLUMN_FORMATS = {
		NumberFormat.getIntegerInstance(Locale.ROOT),
		NumberFormat.getIntegerInstance(Locale.ROOT),
		new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
		NumberFormat.getIntegerInstance(Locale.ROOT)
	};

	/**
	 * Creates a new instance.
	 * @param shopPane the shop pane
	 */
	public OrdersPane(final ShopPane shopPane) {
		super();
		this.setLayout(new GridLayout(1, 1));

		final Container northPane, southPane, orderScrollPane, itemScrollPane;
		final JTable orderTable, itemTable;
		final AbstractButton refreshButton, cancelButton, newButton, addButton, removeButton, orderButton;
		{
			final BeanTableModel<Order> orderTableModel = new BeanTableModel<>(Order.class);
			orderTable = new JTable(orderTableModel, createOrderColumnModel(orderTableModel));
			orderTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			orderTable.setShowGrid(false);
			final BeanTableModel<OrderItem> itemTableModel = new BeanTableModel<>(OrderItem.class);
			itemTable = new JTable(itemTableModel, createItemColumnModel(itemTableModel));
			itemTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			itemTable.setShowGrid(false);

			final JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, northPane = new JPanel(new SpringLayout()), southPane = new JPanel(new SpringLayout()));
			pane.setBorder(new LineBorder(Color.LIGHT_GRAY));
			pane.setResizeWeight(0.5);
			this.add(pane);

			northPane.add(orderScrollPane = new JScrollPane(orderTable));
			northPane.add(refreshButton = new JButton("refresh"));
			northPane.add(cancelButton = new JButton("cancel"));
			northPane.add(newButton = new JButton("new..."));
			southPane.add(itemScrollPane = new JScrollPane(itemTable));
			southPane.add(addButton = new JButton("add"));
			southPane.add(removeButton = new JButton("remove"));
			southPane.add(orderButton = new JButton("order"));
			cancelButton.setEnabled(false);
			removeButton.setEnabled(false);

			new SpringSpread(northPane, 0, 0).alignWest(orderScrollPane);
			new SpringSpread(refreshButton, 0, -2).alignSouth(orderScrollPane);
			new SpringSpread(northPane, 2, null).alignSouth(refreshButton, cancelButton, newButton);
			new SpringSpread(northPane, 0, 80).alignWest(refreshButton);
			new SpringSpread(refreshButton, 5, 80).alignWest(cancelButton);
			new SpringSpread(cancelButton, 5, 80).alignWest(newButton);

			new SpringSpread(southPane, 0, 0).alignWest(itemScrollPane);
			new SpringSpread(addButton, 0, -2).alignSouth(itemScrollPane);
			new SpringSpread(southPane, 2, null).alignSouth(addButton, removeButton, orderButton);
			new SpringSpread(southPane, 0, 80).alignWest(addButton);
			new SpringSpread(addButton, 5, 80).alignWest(removeButton);
			new SpringSpread(removeButton, 5, 80).alignWest(orderButton);
		}

		final ListSelectionListener selectionAction1 = new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				shopPane.setMessage(null);

				final int rowIndex = orderTable.getSelectedRow();
				cancelButton.setEnabled(rowIndex != -1);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				if (rowIndex == -1) {
					itemTableModel.removeRows();
				} else {
					@SuppressWarnings("unchecked")
					final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
					final Order order = orderTableModel.getRow(rowIndex);
					itemTableModel.removeRows();
					itemTableModel.addRows(order.getItems().toArray(new OrderItem[0]));
				}
			}
		};
		orderTable.getSelectionModel().addListSelectionListener(selectionAction1);

		final ListSelectionListener selectionAction2 = new ListSelectionListener() {
			public void valueChanged(final ListSelectionEvent event) {
				if (event.getValueIsAdjusting()) return;
				shopPane.setMessage(null);

				removeButton.setEnabled(itemTable.getSelectedRow() != -1);
			}
		};
		itemTable.getSelectionModel().addListSelectionListener(selectionAction2);

		final ActionListener refreshAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);
					final String alias = (String) shopPane.getSessionMap().get("alias");
					final String password = (String) shopPane.getSessionMap().get("password");
					final Order[] orders = shopPane.getProxy().queryOrders(alias, password).toArray(new Order[0]);

					@SuppressWarnings("unchecked")
					final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
					orderTableModel.removeRows();
					orderTableModel.addRows(orders);
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
				}
			}
		};
		refreshButton.addActionListener(refreshAction);

		final ActionListener cancelAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					final int rowIndex = orderTable.getSelectedRow();
					if (rowIndex != -1) {
						@SuppressWarnings("unchecked")
						final BeanTableModel<Order> orderTableModel = (BeanTableModel<Order>) orderTable.getModel();
						final Order order = orderTableModel.getRow(rowIndex);
						final String alias = (String) shopPane.getSessionMap().get("alias");
						final String password = (String) shopPane.getSessionMap().get("password");
						shopPane.getProxy().cancelOrder(alias, password, order.getIdentity());

						orderTableModel.removeRow(rowIndex);
					}
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
				}
			}
		};
		cancelButton.addActionListener(cancelAction);

		final ActionListener newAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				shopPane.setMessage(null);

				orderTable.getSelectionModel().clearSelection();
				((BeanTableModel<?>) itemTable.getModel()).removeRows();
				addButton.doClick();
			}
		};
		newButton.addActionListener(newAction);

		final ActionListener addAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				shopPane.setMessage(null);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				itemTableModel.addRow(new OrderItem());
			}
		};
		addButton.addActionListener(addAction);

		final ActionListener removeAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				shopPane.setMessage(null);

				@SuppressWarnings("unchecked")
				final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
				itemTableModel.removeRow(itemTable.getSelectedRow());
			}
		};
		removeButton.addActionListener(removeAction);

		final ActionListener orderAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					@SuppressWarnings("unchecked")
					final BeanTableModel<OrderItem> itemTableModel = (BeanTableModel<OrderItem>) itemTable.getModel();
					final OrderItem[] items = itemTableModel.getRows();
					final String alias = (String) shopPane.getSessionMap().get("alias");
					final String password = (String) shopPane.getSessionMap().get("password");
					shopPane.getProxy().createOrder(alias, password, Arrays.asList(items));

					itemTableModel.removeRows();
					refreshButton.doClick();
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
				}
			}
		};
		orderButton.addActionListener(orderAction);
	}


	/**
	 * Creates a new column model for the order table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	private static TableColumnModel createOrderColumnModel(final BeanTableModel<Order> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ORDER_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ORDER_COLUMN_NAMES[headerIndex]);
			final int width = headerIndex == 1 ? 140 : 40;

			final TableCellFormater formater = new TableCellFormater(ORDER_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, width);
			column.setHeaderValue(ORDER_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}


	/**
	 * Creates a new column model for the item table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	private static TableColumnModel createItemColumnModel(final BeanTableModel<OrderItem> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ITEM_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ITEM_COLUMN_NAMES[headerIndex]);
			if (headerIndex > 0) tableModel.setColumnEditable(modelIndex, true);

			final TableCellFormater formater = new TableCellFormater(ITEM_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, 40);
			column.setHeaderValue(ITEM_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}

		return result;
	}
}