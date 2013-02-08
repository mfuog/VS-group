package de.htw.ds.chat;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import de.sb.javase.TypeMetadata;


/**
 * <p>ServiceProtocol independent POJO chat entry class. Note that it implements
 * both the serializable interface to support serialization (for RMI and custom
 * access protocols), and a JAX-B annotation to support marshaling (for JAX-WS).</p>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ChatEntry implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String FORMAT_TEXT = "%s(alias=%s, content=%s, timestamp=%tFT%tT)";

	private final String alias;
	private final String content;
	private final long timestamp;


	/**
	 * Protected constructor, required for marshaling because JAX-WS (unlike the
	 * serialization API) cannot instantiate classes lacking a no-arg constructor.
	 */
	protected ChatEntry() {
		super();

		this.alias = null;
		this.content = null;
		this.timestamp = 0;
	}


	/**
	 * Public constructor.
	 * @param alias the user alias
	 * @param content the content
	 * @param timestamp the creation timestamp in milliseconds since 1.1.1970
	 * @throws NullPointerException if the given alias or content is null
	 */
	public ChatEntry(final String alias, final String content, final long timestamp) {
		super();
		if (alias == null || content == null) throw new NullPointerException();

		this.alias = alias;
		this.content = content;
		this.timestamp = timestamp;
	}


	/**
	 * 
	 * Returns the user alias.
	 * @return the alias
	 */
	public String getAlias() {
		return this.alias;
	}


	/**
	 * Returns the content.
	 * @return the content
	 */
	public String getContent() {
		return this.content;
	}


	/**
	 * Returns the creation timestamp.  
	 * @return the creation timestamp in milliseconds since 1.1.1970
	 */
	public long getTimestamp() {
		return this.timestamp;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return this.alias.hashCode() ^ this.content.hashCode() ^ new Long(this.timestamp).hashCode();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object object) {
		if (object == null || this.getClass() != object.getClass()) return false;
		final ChatEntry chatEntry = (ChatEntry) object;
		return this.alias.equals(chatEntry.alias) && this.content.equals(chatEntry.content) && this.timestamp == chatEntry.timestamp;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format(FORMAT_TEXT, this.getClass().getCanonicalName(), this.alias, this.content, this.timestamp, this.timestamp);
	}
}