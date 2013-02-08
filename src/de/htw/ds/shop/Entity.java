package de.htw.ds.shop;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import de.sb.javase.TypeMetadata;


/**
 * <p>This class models simplistic entities.</p>
 */
@XmlSeeAlso({ Article.class, Customer.class, Order.class, OrderItem.class })
@XmlAccessorType(XmlAccessType.FIELD)
@TypeMetadata(copyright="2010-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public abstract class Entity implements Comparable<Entity>, Serializable {
	private static final long serialVersionUID = 1L;

	@XmlAttribute private long identity;


	/**
	 * Creates a new instance.
	 */
	public Entity () {
		super();
	}


	/**
	 * Returns the identity.
	 * @return the identity
	 */
	public final long getIdentity() {
		return this.identity;
	}


	/**
	 * Sets the identity.
	 * @param identity the identity
	 */
	protected final void setIdentity(final long identity) {
		this.identity = identity;
	}


	/**
	 * {@inheritDoc}
	 */
	public int compareTo(final Entity entity) {
		if (this.identity < entity.identity) return -1;
		if (this.identity > entity.identity) return  1;
		if (this.hashCode() < entity.hashCode()) return -1;
		if (this.hashCode() > entity.hashCode()) return  1;
		return 0;
	}
}