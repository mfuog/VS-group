package de.htw.ds.grid;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import de.sb.javase.TypeMetadata;


/**
 * <p>Instances of this class model grid task responses.</p>
 */
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class GridTaskResponse {

	@XmlAttribute(required=true)
	private final long identity;

	@XmlAttribute(required=true)
	private final String dataType;

	@XmlElement
	private final byte[] dataContent;


	/**
	 * Creates a new uninitialized instance, which is
	 * required for XML marshaling.
	 */
	protected GridTaskResponse() {
		this.identity = 0;
		this.dataType = null;
		this.dataContent = null;
	}


	/**
	 * Creates a new instance.
	 * @param identity the task identity 
	 * @param dataType the data type
	 * @param dataContent the data content
	 * @throws NullPointerException if the given data type or
	 *    data content is <tt>null</tt>
	 */
	public GridTaskResponse(final long identity, final String dataType, final byte[] dataContent) {
		super();
		if (dataType == null | dataContent == null) throw new NullPointerException();

		this.identity = identity;
		this.dataType = dataType;
		this.dataContent = dataContent;
	}


	/**
	 * Returns the task identity.
	 * @return the task identity
	 */
	public long getIdentity() {
		return this.identity;
	}


	/**
	 * Returns the data type.
	 * @return the data type
	 */
	public String getDataType() {
		return this.dataType;
	}


	/**
	 * Returns the data content.
	 * @return the data content
	 */
	public byte[] getDataContent() {
		return this.dataContent;
	}


	/**
	 * Returns the relative file path that should be used to create
	 * the response file. Note that grid clients depend on this file
	 * name pattern, as they will look for the response file under
	 * this name.
	 * @return the relative output path (file name) for a response file
	 */
	public Path getOutputPath() {
		return Paths.get(String.format("%s.%s", this.identity, this.dataType));
	}
}