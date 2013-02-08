package de.htw.ds.shop;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

import de.sb.javase.TypeMetadata;
import de.sb.javase.swing.SpringSpread;


/**
 * <p>Customer pane for use within the ShopClient2 class.</p>
 */
@TypeMetadata(copyright="2012-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class CustomerPane extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * @param shopPane the shop pane
	 */
	public CustomerPane(final ShopPane shopPane) {
		super();
		this.setLayout(new SpringLayout());

		final Container westPane = new JPanel(new SpringLayout()), eastPane = new JPanel(new SpringLayout());
		final Component identityLabel, aliasLabel, passwordLabel, firstNameLabel, lastNameLabel, streetLabel, postcodeLabel, cityLabel, emailLabel, phoneLabel;
		final JTextComponent identityField, aliasField, passwordField, firstNameField, lastNameField, streetField, postcodeField, cityField, emailField, phoneField;
		final AbstractButton logonButton, createButton, deleteButton;

		{
			final JSplitPane pane;
			this.add(pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, westPane, eastPane));
			pane.setResizeWeight(0.5);
			pane.setBorder(new LineBorder(Color.LIGHT_GRAY));

			this.add(logonButton = new JButton("logon"));
			this.add(createButton = new JButton("create"));
			this.add(deleteButton = new JButton("delete"));
			new SpringSpread(this, 2, -2).alignWest(pane);
			new SpringSpread(this, 2, null).alignNorth(pane);
			new SpringSpread(pane, 2, null).alignNorth(logonButton, createButton, deleteButton);
			new SpringSpread(this, 2, 80).alignWest(logonButton);
			new SpringSpread(logonButton, 5, 80).alignWest(createButton);
			new SpringSpread(createButton, 5, 80).alignWest(deleteButton);
		}

		{ // fill left pane area
			westPane.add(identityLabel = new JLabel("Identity"));
			westPane.add(identityField = new JTextField());
			westPane.add(aliasLabel = new JLabel("Alias"));
			westPane.add(aliasField = new JTextField());
			westPane.add(passwordLabel = new JLabel("Password"));
			westPane.add(passwordField = new JPasswordField());
			westPane.add(firstNameLabel = new JLabel("First Name"));
			westPane.add(firstNameField = new JTextField());
			westPane.add(lastNameLabel = new JLabel("Last Name"));
			westPane.add(lastNameField = new JTextField());
			identityField.setEditable(false);

			new SpringSpread(westPane, 2, null).alignWest(identityLabel, aliasLabel, passwordLabel, firstNameLabel, lastNameLabel);
			new SpringSpread(identityField, 0, null).alignVertical(identityLabel);
			new SpringSpread(aliasField, 0, null).alignVertical(aliasLabel);
			new SpringSpread(passwordField, 0, null).alignVertical(passwordLabel);
			new SpringSpread(firstNameField, 0, null).alignVertical(firstNameLabel);
			new SpringSpread(lastNameField, 0, null).alignVertical(lastNameLabel);

			new SpringSpread(westPane, 90, 0).alignWest(identityField, aliasField, passwordField, firstNameField, lastNameField);
			new SpringSpread(westPane, 0, null).alignNorth(identityField);
			new SpringSpread(identityField, 0, null).alignNorth(aliasField);
			new SpringSpread(aliasField, 0, null).alignNorth(passwordField);
			new SpringSpread(passwordField, 0, null).alignNorth(firstNameField);
			new SpringSpread(firstNameField, 0, null).alignNorth(lastNameField);
			new SpringSpread(lastNameField, 0, null).alignSouth(westPane);
		}

		{ // fill right pane area
			eastPane.add(streetLabel = new JLabel("Street"));
			eastPane.add(streetField = new JTextField());
			eastPane.add(postcodeLabel = new JLabel("Postcode"));
			eastPane.add(postcodeField = new JTextField());
			eastPane.add(cityLabel = new JLabel("City"));
			eastPane.add(cityField = new JTextField());
			eastPane.add(emailLabel = new JLabel("Email"));
			eastPane.add(emailField = new JTextField());
			eastPane.add(phoneLabel = new JLabel("Phone"));
			eastPane.add(phoneField = new JTextField());

			new SpringSpread(eastPane, 2, null).alignWest(streetLabel, postcodeLabel, emailLabel, phoneLabel);
			new SpringSpread(postcodeField, 5, null).alignWest(cityLabel);
			new SpringSpread(streetField, 0, null).alignVertical(streetLabel);
			new SpringSpread(postcodeField, 0, null).alignVertical(postcodeLabel, cityLabel);
			new SpringSpread(emailField, 0, null).alignVertical(emailLabel);
			new SpringSpread(phoneField, 0, null).alignVertical(phoneLabel);			

			new SpringSpread(eastPane, 90, 0).alignWest(streetField, emailField, phoneField);
			new SpringSpread(eastPane, 90, 80).alignWest(postcodeField);
			new SpringSpread(cityLabel, 5, 0).alignWest(cityField);
			new SpringSpread(eastPane, 0, null).alignNorth(streetField);
			new SpringSpread(streetField, 0, null).alignNorth(postcodeField, cityField);
			new SpringSpread(postcodeField, 0, null).alignNorth(emailField);
			new SpringSpread(emailField, 0, null).alignNorth(phoneField);
		}

		final ActionListener logonAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					final Customer customer = shopPane.getProxy().queryCustomer(aliasField.getText(), passwordField.getText());
					identityField.setText(Long.toString(customer.getIdentity()));
					firstNameField.setText(customer.getFirstName());
					lastNameField.setText(customer.getLastName());
					streetField.setText(customer.getStreet());
					postcodeField.setText(customer.getPostcode());
					cityField.setText(customer.getCity());
					emailField.setText(customer.getEmail());
					phoneField.setText(customer.getPhone());

					shopPane.getSessionMap().put("alias", customer.getAlias());
					shopPane.getSessionMap().put("password", customer.getPassword());
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
					shopPane.getSessionMap().clear();
				}
			}
		};
		logonButton.addActionListener(logonAction);
		
		final ActionListener createAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					final Customer customer = new Customer();
					customer.setAlias(aliasField.getText());
					customer.setPassword(passwordField.getText());
					customer.setFirstName(firstNameField.getText());
					customer.setLastName(lastNameField.getText());
					customer.setStreet(streetField.getText());
					customer.setPostcode(postcodeField.getText());
					customer.setCity(cityField.getText());
					customer.setEmail(emailField.getText());
					customer.setPhone(phoneField.getText());

					final long customerIdentity = shopPane.getProxy().registerCustomer(customer);
					identityField.setText(Long.toString(customerIdentity));
					shopPane.getSessionMap().put("alias", customer.getAlias());
					shopPane.getSessionMap().put("password", customer.getPassword());
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
					shopPane.getSessionMap().clear();
				}
			}
		};
		createButton.addActionListener(createAction);

		final ActionListener deleteAction = new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				try {
					shopPane.setMessage(null);

					shopPane.getProxy().unregisterCustomer(aliasField.getText(), passwordField.getText());
					shopPane.getSessionMap().clear();
					for (final JTextComponent field : new JTextComponent[] { identityField, aliasField, passwordField, firstNameField, lastNameField, streetField, postcodeField, cityField, emailField, phoneField }) {
						field.setText("");
					}
				} catch (final Exception exception) {
					shopPane.setMessage(exception);
				}
			}
		};
		deleteButton.addActionListener(deleteAction);
	}
}