package de.htw.ds.grid;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import de.sb.javase.TypeMetadata;


/**
 * <p>Adapter class for the XML marshaling of string maps into string
 * entry arrays and vice versa. Note that this requires an intermediate
 * inner StringEntry class because sadly the entries of map entry sets
 * cannot be marshaled; otherwise it would be sufficient to use
 * <tt>"map.entrySet().toArray(new Map.Entry[0])".</p>
 */
@TypeMetadata(copyright="2009-2013 Sascha Baumeister, all rights reserved", version="0.3.0", authors="Sascha Baumeister")
public class StringMapAdapter extends XmlAdapter<StringMapEntry[],Map<String,String>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StringMapEntry[] marshal(final Map<String,String> stringMap) {
		if (stringMap == null) return null;
		final StringMapEntry[] result = new StringMapEntry[stringMap.size()];
		final Iterator<Map.Entry<String,String>> iterator = stringMap.entrySet().iterator();
		for (int index = 0; index < result.length; ++index) {
			final Map.Entry<String,String> entry = iterator.next();
			result[index] = new StringMapEntry(entry.getKey(), entry.getValue());
		}
		return result;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String,String> unmarshal(final StringMapEntry[] stringMapEntries) {
		if (stringMapEntries == null) return null;
		final Map<String,String> result = new HashMap<>();
		for (final StringMapEntry entry : stringMapEntries) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}