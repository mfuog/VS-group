package de.htw.ds.http;

import de.htw.ds.TypeMetadata;


/**
 * <p>This class models HTTP cookies.</p>
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class Cookie {
	private final String name;
	private final String value;
	private final String expires;
	private final String path;
	private final String domain;


	/**
	 * Public constructor.
	 * @param name the name
	 * @param value the value
	 * @param expires the expiration timestamp, or null
	 * @param path the path, or null
	 * @param domain the domain, or null
	 */
	public Cookie(final String name, final String value, final String expires, final String path, final String domain) {
		super();
		if (name == null | value == null) throw new IllegalArgumentException();

		this.name = name;
		this.value = value;
		this.expires = expires;
		this.path = path;
		this.domain = domain;
	}


	/**
	 * Public constructor.
	 * @param cookieText the cookie text representation
	 * @throws IllegalArgumentException if the cookie representation is not valid
	 */
	public Cookie (final String cookieText) {
		final String[] fragments = cookieText.split(";");
		if (fragments.length == 0) throw new IllegalArgumentException();
		final String[] keyFragments = fragments[0].split("=");
		if (keyFragments.length != 2) throw new IllegalArgumentException();
		this.name = keyFragments[0].trim();
		this.value = keyFragments[1].trim();

		String expires = null, path = null, domain = null;
		for (int index = 1; index < fragments.length; ++index) {
			final String[] propertyFragments = fragments[index].split("=");
			if (fragments.length != 2) throw new IllegalArgumentException();
			propertyFragments[0] = propertyFragments[0].trim();
			if (propertyFragments[0].equals("expires")) {
				expires = propertyFragments[1].trim();
			} else if (propertyFragments[0].equals("path")) {
				path = propertyFragments[1].trim();
			} else if (propertyFragments[0].equals("domain")) {
				domain = propertyFragments[1].trim();
			}
		}
		this.expires = expires;
		this.path = path;
		this.domain = domain;
	}


	/**
	 * Returns the name.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}


	/**
	 * Returns the value.
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}


	/**
	 * Returns the expiration timestamp.
	 * @return the expiration timestamp
	 */
	public String getExpires() {
		return this.expires;
	}


	/**
	 * Returns the path.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}


	/**
	 * Returns the domain.
	 * @return the domain
	 */
	public String getDomain() {
		return this.domain;
	}


	/**
	 * Returns the string representation.
	 * @return the string representation
	 */
	public String toString() {
		return this.name + "=" + this.value;
	}
}