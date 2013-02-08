package de.htw.ds.shop;

import java.awt.Color;
import java.awt.Component;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import de.sb.javase.Classes;
import de.sb.javase.TypeMetadata;
import de.sb.javase.swing.SpringSpread;


/**
 * <p>Shop (main) pane for use within the ShopClient2 class.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ShopPane extends JPanel {
	private static final long serialVersionUID = 1L;

	private final ShopService proxy;
	private final Map<String,Object> sessionMap;
	private final JTextComponent messageField;


	/**
	 * Creates a new instance.
	 * @param proxy the chat service proxy
	 * @throws NullPointerException if the given proxy is null
	 */
	public ShopPane(final ShopService proxy) {
		super();
		if (proxy == null) throw new NullPointerException();

		this.proxy = proxy;
		this.sessionMap = Collections.synchronizedMap(new HashMap<String,Object>());

		final Component messageLabel;
		final JTabbedPane tabbedPane;
		this.setLayout(new SpringLayout());
		this.add(tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT));
		this.add(messageLabel = new JLabel("Message"));
		this.add(this.messageField = new JTextField());

		tabbedPane.addTab("Articles", this.createTabPane(0));
		tabbedPane.addTab("Customer", null);
		tabbedPane.addTab("Orders", null);
		messageLabel.setForeground(Color.RED);
		this.messageField.setEditable(false);

		new SpringSpread(this, 0, 0).alignWest(tabbedPane);
		new SpringSpread(this.messageField, 2, 0).alignSouth(tabbedPane);
		new SpringSpread(this, 2, null).alignSouth(this.messageField);
		new SpringSpread(this.messageField, 0, null).alignVertical(messageLabel);
		new SpringSpread(this, 2, null).alignWest(messageLabel);
		new SpringSpread(messageLabel, 5, -2).alignWest(this.messageField);

		// lazy initialize pane tabs 1-n in order to spread panel creation cost
		final ChangeListener selectionAction = new ChangeListener() {
			public void stateChanged(final ChangeEvent event) {
				ShopPane.this.setMessage(null);

				final int selectionIndex = tabbedPane.getSelectedIndex();
				if (selectionIndex >= 0 && tabbedPane.getComponentAt(selectionIndex) == null) {
					tabbedPane.setComponentAt(selectionIndex, ShopPane.this.createTabPane(selectionIndex));
				}
			}
		};
		tabbedPane.addChangeListener(selectionAction);
     }


	/**
	 * Creates a pane for the given tab index. Note that this allows lazy
	 * initialization of the pane tabs in order to spread the creation cost.
	 * @param tabIndex the tab index
	 * @return the tab pane
	 */
	private JPanel createTabPane(final int tabIndex) {
		switch (tabIndex) {
			case 0: return new ArticlesPane(this);
			case 1: return new CustomerPane(this);
			case 2: return new OrdersPane(this);
			default: throw new AssertionError();
		}
	}


	/**
	 * Returns the shop service proxy.
	 * @return the proxy
	 */
	public ShopService getProxy() {
		return this.proxy;
	}


	/**
	 * Returns the session map.
	 * @return the session map
	 */
	public Map<String,Object> getSessionMap() {
		return this.sessionMap;
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
}