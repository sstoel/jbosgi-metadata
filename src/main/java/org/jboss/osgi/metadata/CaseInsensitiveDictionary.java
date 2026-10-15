/*
 * #%L
 * JBossOSGi Resolver Metadata
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.osgi.metadata;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.osgi.metadata.spi.NotNullException;

/**
 * CaseInsensitiveDictionary.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 */
@SuppressWarnings("rawtypes")
public class CaseInsensitiveDictionary extends Hashtable {
    private static final long serialVersionUID = 5802491129524016545L;

    /** The entries, keys compare like String.equalsIgnoreCase as in the filter implementation */
    private Map<String, Object> delegate;

    /** The original keys */
    private Set<String> originalKeys;

    /**
     * Create a new CaseInsensitiveDictionary.
     *
     * @param delegate the delegate
     */
    @SuppressWarnings("unchecked")
    public CaseInsensitiveDictionary(Dictionary delegate) {
        NotNullException.assertValue(delegate, "delegate");

        this.delegate = Collections.synchronizedMap(new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER));
        this.originalKeys = Collections.synchronizedSet(new HashSet<String>());
        Enumeration<String> e = delegate.keys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            if (get(key) != null)
                throw new IllegalArgumentException("Duplicates with varying case for key [" + key + "] : " + delegate);

            this.delegate.put(key, delegate.get(key));
            originalKeys.add(key);
        }
    }

    @SuppressWarnings("unchecked")
    public Enumeration<Object> elements() {
        return Collections.enumeration(values());
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean equals(Object obj) {
        if (obj == this)
            return true;

        // A map has the same entries, the keys in their original case as in hashCode (Map.equals)
        if (obj instanceof Map)
            return entrySet().equals(((Map) obj).entrySet());

        // Another dictionary has the same entries when its keys, looked up as they are in here, give the same values
        if (obj instanceof Dictionary == false)
            return false;

        Dictionary other = (Dictionary) obj;
        if (size() != other.size())
            return false;

        for (String key : keySet()) {
            if (get(key).equals(other.get(key)) == false)
                return false;
        }
        return true;
    }

    public Object get(Object key) {
        if (key instanceof String == false)
            return null;
        return delegate.get(key);
    }

    public int hashCode() {
        // Like a map that has the keys in their original case
        return entrySet().hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public Enumeration<String> keys() {
        return new Vector(originalKeys).elements();
    }

    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    public Object remove(Object key) {
        throw new UnsupportedOperationException("immutable");
    }

    // Hashtable is a Map, the Map methods must not use the unused table of the super class

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return contains(value);
    }

    @Override
    public boolean contains(Object value) {
        synchronized (delegate) {
            for (Object aux : delegate.values()) {
                if (aux.equals(value))
                    return true;
            }
        }
        return false;
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        Object value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer action) {
        for (String key : keySet())
            action.accept(key, get(key));
    }

    @Override
    public void putAll(Map map) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object replace(Object key, Object value) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public void replaceAll(BiFunction function) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object computeIfAbsent(Object key, Function mappingFunction) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction remappingFunction) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object compute(Object key, BiFunction remappingFunction) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Object merge(Object key, Object value, BiFunction remappingFunction) {
        throw new UnsupportedOperationException("immutable");
    }

    @Override
    public Set<String> keySet() {
        synchronized (originalKeys) {
            return Collections.unmodifiableSet(new LinkedHashSet<String>(originalKeys));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection values() {
        // In the order of the keys
        List<Object> values = new ArrayList<Object>();
        for (String key : keySet())
            values.add(get(key));
        return Collections.unmodifiableCollection(values);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set entrySet() {
        Set<Map.Entry> entrySet = new LinkedHashSet<Map.Entry>();
        for (String key : keySet())
            entrySet.add(new AbstractMap.SimpleImmutableEntry(key, get(key)));
        return Collections.unmodifiableSet(entrySet);
    }

    public int size() {
        return delegate.size();
    }

    public String toString() {
        // With the keys in their original case
        return new LinkedHashMap<Object, Object>(this).toString();
    }
}
