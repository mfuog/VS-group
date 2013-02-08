package de.htw.ds.shop;

import javax.xml.bind.annotation.XmlAttribute;

import de.sb.javase.TypeMetadata;


/**
 * <p>This class models simplistic order items. Note that this entity is
 * equivalent to the "PurchaseItem" table, because "order" is a reserved
 * word in SQL.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class OrderItem extends Entity {
	private static final long serialVersionUID = 1L;

	@XmlAttribute private long articleIdentity;
	@XmlAttribute private long articleGrossPrice;
	@XmlAttribute private int count;


	/**
	 * Creates a new instance.
	 */
	public OrderItem () {
		super();
	}


	/**
	 * Returns the identity of the related article.
	 * @return the article identity
	 */
	public long getArticleIdentity() {
		return this.articleIdentity;
	}

	/**
	 * Sets the identity of the related article.
	 * @param articleIdentity the article identity
	 */
	public void setArticleIdentity(final long articleIdentity) {
		this.articleIdentity = articleIdentity;
	}

	
	/**
	 * Returns the article price (gross).
	 * @return the article gross price
	 */
	public long getArticleGrossPrice() {
		return this.articleGrossPrice;
	}


	/**
	 * Sets the article price (gross).
	 * @param articleGrossPrice the article gross price
	 */
	public void setArticleGrossPrice(final long articleGrossPrice) {
		this.articleGrossPrice = articleGrossPrice;
	}


	/**
	 * Returns the number of units ordered.
	 * @return the unit count
	 */
	public int getCount() {
		return this.count;
	}


	/**
	 * Sets the number of units ordered.
	 * @param count unit count
	 */
	public void setCount(final int count) {
		this.count = count;
	}
}