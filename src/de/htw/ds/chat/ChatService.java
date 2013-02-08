package de.htw.ds.chat;

import java.util.Deque;
import java.util.LinkedList;
import de.sb.javase.TypeMetadata;


/**
 * <p>ServiceProtocol independent POJO chat service class. Note that this
 * implementation is thread-safe.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public final class ChatService {
	private final Deque<ChatEntry> chatEntries;
	private final int maxSize;


	/**
	 * Public constructor.
	 * @param maxSize the maximum number of chat entries
	 * @throws IllegalArgumentException if the maximum number of chat entries is negative
	 */
	public ChatService(final int maxSize) {
		super();
		if (maxSize <= 0) throw new IllegalArgumentException(Integer.toString(maxSize));

		this.chatEntries = new LinkedList<ChatEntry>();
		this.maxSize = maxSize;
	}


	/**
	 * Adds a chat entry.
	 * @param alias the user alias
	 * @param content the content
	 * @return the creation timestamp
	 * @throws NullPointerException if any of the given arguments is null
	 */
	public long addEntry(final String alias, final String content) {
		final ChatEntry entry = new ChatEntry(alias, content, System.currentTimeMillis());

		synchronized (this.chatEntries) {
			if (this.chatEntries.size() == this.maxSize) {
				this.chatEntries.removeLast();
			}
			this.chatEntries.addFirst(entry);
		}

		return entry.getTimestamp();
	}


	/**
	 * Removes a chat entry.
	 * @param entry the chat entry
	 */
	public boolean removeEntry(final ChatEntry entry) {
		synchronized (this.chatEntries) {
			return this.chatEntries.remove(entry);
		}
	}


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 */
	public ChatEntry[] getEntries() {
		synchronized (this.chatEntries) {
			return this.chatEntries.toArray(new ChatEntry[this.chatEntries.size()]);
		}
	}
}