/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.test.osgi.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jboss.osgi.metadata.CaseInsensitiveDictionary;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the {@link CaseInsensitiveDictionary}
 */
public class CaseInsensitiveDictionaryTestCase {

    private CaseInsensitiveDictionary createDictionary() {
        Hashtable<String, Object> source = new Hashtable<String, Object>();
        source.put("Bundle-SymbolicName", "foo");
        source.put("Bundle-Version", "1.0.0");
        return new CaseInsensitiveDictionary(source);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLookup() throws Exception {
        CaseInsensitiveDictionary dict = createDictionary();
        Assert.assertEquals("foo", dict.get("bundle-symbolicname"));
        Assert.assertEquals("foo", dict.get("BUNDLE-SYMBOLICNAME"));
        Assert.assertTrue(dict.containsKey("bundle-version"));
        Assert.assertEquals(2, dict.size());

        // The keys keep their case, the values are in the order of the keys
        Assert.assertTrue(dict.keySet().contains("Bundle-SymbolicName"));
        Assert.assertEquals(Collections.list(dict.keys()), new java.util.ArrayList<Object>(dict.keySet()));
        int index = 0;
        Object[] values = dict.values().toArray();
        for (Object key : dict.keySet())
            Assert.assertEquals(dict.get(key), values[index++]);
        Assert.assertEquals(Collections.list(dict.elements()), new java.util.ArrayList<Object>(dict.values()));
        Assert.assertTrue(dict.toString(), dict.toString().contains("Bundle-SymbolicName=foo"));
    }

    @Test
    public void testEquals() throws Exception {
        CaseInsensitiveDictionary dict = createDictionary();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Bundle-SymbolicName", "foo");
        map.put("Bundle-Version", "1.0.0");

        Assert.assertEquals(dict, createDictionary());
        Assert.assertEquals(dict, new Hashtable<String, Object>(map));
        Assert.assertEquals(dict, map);
        Assert.assertEquals(map, dict);
        Assert.assertEquals(map.hashCode(), dict.hashCode());

        map.put("Bundle-Version", "2.0.0");
        Assert.assertNotEquals(dict, map);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImmutable() throws Exception {
        final CaseInsensitiveDictionary dict = createDictionary();
        Runnable[] changes = new Runnable[] {
            () -> dict.put("x", "y"),
            () -> dict.remove("Bundle-Version"),
            () -> dict.remove("Bundle-Version", "1.0.0"),
            () -> dict.putIfAbsent("x", "y"),
            () -> dict.replace("Bundle-Version", "2.0.0"),
            () -> dict.replace("Bundle-Version", "1.0.0", "2.0.0"),
            () -> dict.replaceAll((k, v) -> v),
            () -> dict.compute("x", (k, v) -> "y"),
            () -> dict.computeIfAbsent("x", k -> "y"),
            () -> dict.computeIfPresent("Bundle-Version", (k, v) -> "y"),
            () -> dict.merge("x", "y", (a, b) -> b),
            () -> dict.putAll(Collections.singletonMap("x", "y")),
            () -> dict.clear()
        };
        for (int i = 0; i < changes.length; i++) {
            try {
                changes[i].run();
                Assert.fail("UnsupportedOperationException expected for change " + i);
            } catch (UnsupportedOperationException ex) {
                // expected
            }
        }
        Assert.assertEquals(2, dict.size());
        Assert.assertNull(dict.get("x"));
    }
}
