package de.htw.ds.shop;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import de.sb.javase.TypeMetadata;
import de.sb.javase.swing.BeanTableModel;
import de.sb.javase.swing.ScaleFormat;
import de.sb.javase.swing.SpringSpread;
import de.sb.javase.swing.TableCellFormater;


/**
 * <p>Articles pane for use within the ShopClient2 class.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ArticlesPane extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String[] ARTICLE_COLUMN_NAMES = {"identity", "description", "price", "count"};
	private static final String[] ARTICLE_HEADER_NAMES = {"ID", "Description", "Gross", "Available"};
	private static final Format[] ARTICLE_COLUMN_FORMATS = {
		NumberFormat.getIntegerInstance(Locale.ROOT),
		null,
		new ScaleFormat(NumberFormat.getCurrencyInstance(), -2),
		NumberFormat.getIntegerInstance(Locale.ROOT)
	};

	/**
	 * Creates a new instance.
	 * @param shopPane the shop pane
	 */
	public ArticlesPane(final ShopPane shopPane) {
		super();
		this.setLayout(new SpringLayout());

		final Component articleScrollPane;
		final JTable articleTable;
		final AbstractButton refreshButton;
		{
			final BeanTableModel<Article> articleTableModel = new BeanTableModel<>(Article.class);
			articleTable = new JTable(articleTableModel, createArticleColumnModel(articleTableModel));
			articleTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			articleTable.setShowGrid(false);

			this.add(articleScrollPane = new JScrollPane(articleTable));
			this.add(refreshButton = new JButton("refresh"));

			new SpringSpread(refreshButton, 2, -2).alignSouth(articleScrollPane);
			new SpringSpread(this, 2, -2).alignWest(articleScrollPane);
			new SpringSpread(this, 2, null).alignSouth(refreshButton);
			new SpringSpread(this, 2, 80).alignWest(refreshButton);
		}

		final ActionListener refreshAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					@SuppressWarnings("unchecked")
					final BeanTableModel<Article> articleTableModel = (BeanTableModel<Article>) articleTable.getModel();
					final Article[] articles = shopPane.getProxy().queryArticles().toArray(new Article[0]);
					articleTableModel.removeRows();
					articleTableModel.addRows(articles);
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
				}
			}
		};
		refreshButton.addActionListener(refreshAction);
	}


	/**
	 * Creates a new column model for the article table.
	 * @param tableModel the table model
	 * @return the corresponding column model
	 */
	private static TableColumnModel createArticleColumnModel(final BeanTableModel<Article> tableModel) {
		final DefaultTableColumnModel result = new DefaultTableColumnModel();
		result.setColumnMargin(5);

		for (int headerIndex = 0; headerIndex < ARTICLE_COLUMN_NAMES.length; ++headerIndex) {
			final int modelIndex = tableModel.findColumn(ARTICLE_COLUMN_NAMES[headerIndex]);
			final int width = headerIndex == 1 ? 300 : 40;

			final TableCellFormater formater = new TableCellFormater(ARTICLE_COLUMN_FORMATS[headerIndex]);
			final TableColumn column = new TableColumn(modelIndex, width);
			column.setHeaderValue(ARTICLE_HEADER_NAMES[headerIndex]);
			column.setCellRenderer(formater);
			column.setCellEditor(formater);
			result.addColumn(column);
		}
		return result;
	}
}