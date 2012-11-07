package de.htw.ds.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.htw.ds.TypeMetadata;


/**
 * <p>This is a wrapper class enriching underlying string maps with event callback functionality.
 * Instances notify their registered event handlers of every change event within the underlying
 * map (via <tt>put()</tt>, <tt>remove()</tt>, or <tt>clear()</tt> methods). This includes
 * modifications caused by map views (<tt>keySet()</tt>, <tt>values()</tt>, <tt>entrySet()</tt>)
 * and their associated iterators. Note that event notification doesn't happen if the underlying
 * map (the one provided with the constructor) is modified directly. Also note that this
 * implementation supports only string keys because property change events only support string
 * property names.</p>
 * @param <V> the value type
 */
@TypeMetadata(copyright="2010-2012 Sascha Baumeister, all rights reserved", version="0.2.2", authors="Sascha Baumeister")
public final class CallbackMap<V> extends AbstractMap<String,V> {

	private final Map<String,V> delegateMap;
	private final Set<VetoableChangeListener> listeners; 


	/**
	 * Creates a new instance based on an empty hash map.
	 */
	public CallbackMap() {
		this(new HashMap<String,V>());
	}


	/**
	 * Creates a new instance based on the given map. Note that no put events are spawned
	 * for the elements existing within the underlying map, use <tt>putAll()</tt> in case
	 * this is required.
	 * @param delegateMap the underlying map
	 */
	public CallbackMap(final Map<String,V> delegateMap) {
		this.delegateMap = delegateMap;
		this.listeners = new HashSet<>();
	}


	/**
	 * Returns the registered event listeners.
	 * @return the event listeners
	 */
	public Set<VetoableChangeListener> getListeners() {
		return this.listeners;
	}


	/**
	 * Returns the underlying delegate map. Note that event notifications don't take place
	 * when modifying the delegate map directly, which is the intended use-case for this method.
	 * @return the delegate map
	 */
	public Map<String,V> getDelegateMap() {
		return this.delegateMap;
	}


	/**
	 * {@inheritDoc Map}
	 * <p>In contradiction to the previous (inherited) sentence, this implementation only
	 * throws an <tt>UnsupportedOperationException</tt> if the underlying map does.
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
	 */
	public V put(final String key, final V value) {
		final V oldValue = this.delegateMap.get(key);

		final PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, value);
		final VetoableChangeListener[] listeners = this.listeners.toArray(new VetoableChangeListener[0]);
		try {
			for (final VetoableChangeListener listener : listeners) {
				listener.vetoableChange(event);	
			}
			return this.delegateMap.put(key, value);
		} catch (final PropertyVetoException exception) {
			throw new IllegalArgumentException(exception);
		}
	}


	/**
	 * {@inheritDoc}}
	 */
	public Set<Map.Entry<String,V>> entrySet() {
		return new AbstractSet<Map.Entry<String,V>>() {
			final Set<Map.Entry<String,V>> entrySet = CallbackMap.this.delegateMap.entrySet();

			public int size() {
				return this.entrySet.size();
			}

			public Iterator<Map.Entry<String,V>> iterator() {
				return new Iterator<Map.Entry<String,V>>() {
					@SuppressWarnings("unqualified-field-access")
					private final Iterator<Map.Entry<String,V>> iterator = entrySet.iterator();
					private Map.Entry<String,V> currentEntry = null;

					public boolean hasNext() {
						return this.iterator.hasNext();
					}

					public java.util.Map.Entry<String,V> next() {
						return this.currentEntry = this.iterator.next();
					}

					public void remove() {
						final String key = this.currentEntry.getKey();
						final V oldValue = this.currentEntry.getValue();
						final PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, null);
						final VetoableChangeListener[] listeners = CallbackMap.this.listeners.toArray(new VetoableChangeListener[0]);
						try {
							for (final VetoableChangeListener listener : listeners) {
								listener.vetoableChange(event);	
							}
							this.iterator.remove();
						} catch (final PropertyVetoException exception) {
							throw new IllegalArgumentException(exception);
						}
					}
				};
			}
		};
	}
}