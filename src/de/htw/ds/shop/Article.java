package de.htw.ds.shop;

import javax.xml.bind.annotation.XmlAttribute;
import de.sb.javase.TypeMetadata;


/**
 * <p>This class models simplistic articles.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class Article extends Entity {
	private static final long serialVersionUID = 1L;

	private String description;
	@XmlAttribute private long price;
	@XmlAttribute private int count;


	/**
	 * Creates a new instance.
	 */
	public Article () {
		super();
	}


	/**
	 * Returns the description.
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}


	/**
	 * Sets the description.
	 * @param description the description
	 */
	public void setDescription(final String description) {
		this.description = description;
	}


	/**
	 * Returns the unit price (gross).
	 * @return the unit price
	 */
	public long getPrice() {
		return this.price;
	}

	/**
	 * Sets the unit price (gross).
	 * @param price the unit price
	 */
	public void setPrice(final long price) {
		this.price = price;
	}


	/**
	 * Returns the number of units on stock.
	 * @return the unit count
	 */
	public int getCount() {
		return this.count;
	}


	/**
	 * Sets the number of units on stock.
	 * @param count the unit count
	 */
	public void setCount(final int count) {
		this.count = count;
	}
}