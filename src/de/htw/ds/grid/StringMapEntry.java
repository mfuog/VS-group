package de.htw.ds.grid;

import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import de.sb.javase.TypeMetadata;


/**
 * <p>Model class for the XML marshaling of Map<String,String> entries.</p>
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class StringMapEntry implements Map.Entry<String,String> {

	@XmlAttribute
	private final String key;

	@XmlAttribute
	private final String value;

	/**
	 * No-Arg constructor required for JAX-B marshaling.
	 */
	protected StringMapEntry() {
		this.key = null;
		this.value = null;
	}


	/**
	 * Creates a new instance.
	 * @param key the key
	 * @param value the value
	 */
	public StringMapEntry(final String key, final String value) {
		this.key = key;
		this.value = value;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getKey() {
		return this.key;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getValue() {
		return this.value;
	}


	/**
	 * {@inheritDoc}
	 * @throws UnsupportedOperationException always, because this operation is not supported
	 */
	public String setValue(final String value) {
		throw new UnsupportedOperationException();
	}
}