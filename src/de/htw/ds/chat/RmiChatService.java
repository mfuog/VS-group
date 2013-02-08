package de.htw.ds.chat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import de.sb.javase.TypeMetadata;


/**
 * <p>Chat service interface for Java-RMI.</p>
 */
@TypeMetadata(copyright="2008-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public interface RmiChatService extends Remote {

	/**
	 * Adds a chat entry.
	 * @param alias the user alias
	 * @param content the content
	 * @return the creation timestamp
	 */
	long addEntry(String alias, String content) throws RemoteException;


	/**
	 * Removes a chat entry.
	 * @param entry the chat entry
	 */
	boolean removeEntry(ChatEntry entry) throws RemoteException;


	/**
	 * Returns the chat entries.
	 * @return the chat entries
	 */
	ChatEntry[] getEntries() throws RemoteException;
}