package de.htw.ds.shop;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAttribute;

import de.sb.javase.TypeMetadata;


/**
 * <p>This class models simplistic orders. Note that this entity is equivalent
 * to the "purchase" table, because "order" is a reserved word in SQL.</p>
 */
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class Order extends Entity {
	private static final long serialVersionUID = 1L;

	@XmlAttribute private long customerIdentity;
	@XmlAttribute private long creationTimestamp;
	@XmlAttribute private double taxRate;
	private final SortedSet<OrderItem> items;


	/**
	 * Creates a new instance.
	 */
	public Order () {
		super();

		this.items = Collections.synchronizedSortedSet(new TreeSet<OrderItem>());
	}


	/**
	 * Returns the identity of the related customer.
	 * @return the customer identity
	 */
	public long getCustomerIdentity() {
		return this.customerIdentity;
	}


	/**
	 * Sets the identity of the related customer.
	 * @param customerIdentity the customer identity
	 */
	public void setCustomerIdentity(final long customerIdentity) {
		this.customerIdentity = customerIdentity;
	}


	/**
	 * Returns the creation time stamp in milliseconds since 1/1/1970.
	 * @return the creation time stamp
	 */
	public long getCreationTimestamp() {
		return this.creationTimestamp;
	}


	/**
	 * Sets the creation time stamp in milliseconds since 1/1/1970.
	 * @param creationTimestamp the creation time stamp
	 */
	public void setCreationTimestamp(final long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}


	/**
	 * Returns the tax rate.
	 * @return the tax rate
	 */
	public double getTaxRate() {
		return this.taxRate;
	}


	/**
	 * Sets the tax rate.
	 * @param taxRate the tax rate
	 */
	public void setTaxRate(final double taxRate) {
		this.taxRate = taxRate;
	}


	/**
	 * Returns the number of items.
	 * @return the item count
	 */
	public int getItemCount() {
		return this.items.size();
	}


	/**
	 * Returns the related order items.
	 * @return the order items
	 */
	public SortedSet<OrderItem> getItems() {
		return this.items;
	}


	/**
	 * Returns the gross sum.
	 * @return the gross sum
	 */
	public long getGrossPrice() {
		int grossPrice = 0;
		for (final OrderItem item : this.items) {
			grossPrice += item.getArticleGrossPrice() * item.getCount();
		}
		return grossPrice;
	}


	/**
	 * Returns the net sum.
	 * @return the net sum
	 */
	public long getNetPrice() {
		return this.getGrossPrice() + this.getTax();
	}


	/**
	 * Returns the tax sum.
	 * @return the tax sum
	 */
	public long getTax() {
		return Math.round(this.getGrossPrice() * this.getTaxRate());
	}
}