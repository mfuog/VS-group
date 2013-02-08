package de.htw.ds.shop;

import javax.xml.bind.annotation.XmlAttribute;

import de.sb.javase.TypeMetadata;


/**
 * <p>This class models simplistic customers.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class Customer extends Entity {
	private static final long serialVersionUID = 1L;

	@XmlAttribute private String alias;
	@XmlAttribute private String password;
	@XmlAttribute private String firstName;
	@XmlAttribute private String lastName;
	@XmlAttribute private String street;
	@XmlAttribute private String postcode;
	@XmlAttribute private String city;
	@XmlAttribute private String email;
	@XmlAttribute private String phone;


	/**
	 * Creates a new instance.
	 */
	public Customer () {
		super();
	}


	/**
	 * Returns the unique alias.
	 * @return the alias
	 */
	public String getAlias() {
		return this.alias;
	}

	/**
	 * Sets the unique alias.
	 * @param alias the alias
	 */
	public void setAlias(final String alias) {
		this.alias = alias;
	}

	/**
	 * Returns the password.
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password.
	 * @param password the password
	 */
	public void setPassword(final String password) {
		this.password = password;
	}

	/**
	 * Returns the first (given) name.
	 * @return the first name
	 */
	public String getFirstName() {
		return this.firstName;
	}

	/**
	 * Sets the first (given) name.
	 * @param firstName the first name
	 */
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Returns the last (family) name.
	 * @return
	 */
	public String getLastName() {
		return this.lastName;
	}

	/**
	 * Sets the last (family) name.
	 * @param lastName the last name
	 */
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Returns the street and street number.
	 * @return the street
	 */
	public String getStreet() {
		return this.street;
	}

	/**
	 * Sets the street and street number.
	 * @param street the street
	 */
	public void setStreet(final String street) {
		this.street = street;
	}

	/**
	 * Returns the post code.
	 * @return the post code
	 */
	public String getPostcode() {
		return this.postcode;
	}

	/**
	 * Sets the post code.
	 * @param postcode the post code
	 */
	public void setPostcode(final String postcode) {
		this.postcode = postcode;
	}

	/**
	 * Returns the city.
	 * @return the city
	 */
	public String getCity() {
		return this.city;
	}

	/**
	 * Sets the city.
	 * @param city the city
	 */
	public void setCity(final String city) {
		this.city = city;
	}

	/**
	 * Returns the e-mail address.
	 * @return the e-mail address
	 */
	public String getEmail() {
		return this.email;
	}

	/**
	 * Sets the e-mail address.
	 * @param email the e-mail address
	 */
	public void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Returns the phone number.
	 * @return the phone number
	 */
	public String getPhone() {
		return this.phone;
	}

	/**
	 * Sets the phone number.
	 * @param phone the phone number
	 */
	public void setPhone(final String phone) {
		this.phone = phone;
	}
}