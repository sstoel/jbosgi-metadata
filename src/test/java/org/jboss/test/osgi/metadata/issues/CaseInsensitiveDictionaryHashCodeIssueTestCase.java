/*
 * #%L
 * JBossOSGi Metadata
 * %%
 * Copyright (C) 2026 JBoss by Red Hat
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
package org.jboss.test.osgi.metadata.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.jboss.osgi.metadata.CaseInsensitiveDictionary;
import org.junit.Test;

/**
 * Two {@code CaseInsensitiveDictionary} objects (the type of {@code Bundle.getHeaders()}) whose header names differ
 * only in case are equal but have different hash codes: {@code equals} looks the keys up case-insensitively, while
 * {@code hashCode} hashes the entries with the keys in their original case.
 *
 * <h3>Severity</h3>
 * minor. Violates the {@code Object.hashCode} contract (and the {@code Map.equals}/{@code Map.hashCode} contract the
 * branch commit 20fb3dd claims to establish): header dictionaries used as keys or set elements (caches keyed by
 * bundle headers, de-duplication of manifests) are found or lost depending on the case of the header names.
 *
 * <h3>Component</h3>
 * jbosgi-metadata, {@code org.jboss.osgi.metadata.CaseInsensitiveDictionary} (returned by
 * {@code AbstractOSGiMetaData.getHeaders()} and the framework's {@code Bundle.getHeaders()}).
 *
 * <h3>Found by</h3>
 * static workstream: SpotBugs 4.9.8 WMI_WRONG_MAP_ITERATOR / Error Prone UnsynchronizedOverridesSynchronized on
 * CaseInsensitiveDictionary.equals/hashCode (CaseInsensitiveDictionary.java:105/117) led to a manual check of the
 * equals/hashCode pair (category: equals/hashCode contract violations).
 *
 * <h3>Spec reference</h3>
 * Java SE 25 {@code Object.hashCode()} javadoc: "If two objects are equal according to the equals method, then
 * calling the hashCode method on each of the two objects must produce the same integer result."<br>
 * {@code java.util.Map.hashCode()}: "The hash code of a map is defined to be the sum of the hash codes of each entry
 * in the map's entrySet() view. This ensures that m1.equals(m2) implies that m1.hashCode()==m2.hashCode() for any two
 * maps m1 and m2"; {@code Map.equals}: "two maps m1 and m2 represent the same mappings if
 * m1.entrySet().equals(m2.entrySet())".<br>
 * OSGi Core R8 {@code Bundle.getHeaders()} javadoc: "Header names are case-insensitive." (the lookup, not equality,
 * is required to be case-insensitive).
 *
 * <h3>Code location</h3>
 * {@code src/main/java/org/jboss/osgi/metadata/CaseInsensitiveDictionary.java:87-109} ({@code equals}: for every key
 * of this dictionary, {@code get(key).equals(other.get(key))} - the other dictionary is a CaseInsensitiveDictionary,
 * so {@code "Bundle-Name"} finds its {@code "bundle-name"} entry) and lines 116-120 ({@code hashCode}:
 * {@code entrySet().hashCode()} over the original-case keys). Introduced by 20fb3dd "Build against OSGi Core R8, make
 * CaseInsensitiveDictionary a consistent Map" (WF41-osgi-r8). On WF41 hashCode was {@code delegate.hashCode()} over
 * the lower-cased keys, consistent with a case-insensitive equals (whose value comparison was inverted there; this
 * test passes on WF41 because the two dictionaries are not equal there).
 *
 * <h3>Root cause</h3>
 * equals and hashCode use different notions of key equality (case-insensitive vs. case-sensitive).
 *
 * <h3>Reproduction</h3>
 * {@code cd jbosgi-metadata && JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home mvn
 * -Dmaven.compiler.proc=full test -Dtest=CaseInsensitiveDictionaryHashCodeIssueTestCase}
 *
 * <h3>Observed</h3>
 * <pre>
 * java.lang.AssertionError: CaseInsensitiveDictionary objects that are equal ({Bundle-SymbolicName=foo,
 * Bundle-Version=1.0.0, Bundle-Name=Foo Bundle} / {Bundle-SymbolicName=foo, bundle-name=Foo Bundle,
 * BUNDLE-VERSION=1.0.0}) must have equal hash codes expected:&lt;-1298068752&gt; but was:&lt;2065124240&gt;
 * </pre>
 *
 * <h3>Expected</h3>
 * Equal dictionaries have equal hash codes: either equals compares the entry sets (original-case keys, as Map.equals
 * defines it, so the two dictionaries are not equal) or hashCode uses the lower-cased keys.
 *
 * <h3>Reference behaviour</h3>
 * Not compared (Equinox's Headers does not override equals/hashCode, i.e. identity, which is consistent).
 *
 * <h3>Suggested fix</h3>
 * Make equals {@code obj instanceof Map ? entrySet().equals(((Map) obj).entrySet()) : ...} (consistent with the
 * existing hashCode and with Map.equals; the existing CaseInsensitiveDictionaryTestCase.testEquals still passes), or
 * compute hashCode from the lower-cased keys.
 *
 * <h3>Regression vs WF41</h3>
 * yes (the inconsistent hashCode comes with 20fb3dd on WF41-osgi-r8).
 */
public class CaseInsensitiveDictionaryHashCodeIssueTestCase {

    private static CaseInsensitiveDictionary headers(String nameHeader, String versionHeader) {
        Hashtable<String, Object> source = new Hashtable<String, Object>();
        source.put("Bundle-SymbolicName", "foo");
        source.put(nameHeader, "Foo Bundle");
        source.put(versionHeader, "1.0.0");
        return new CaseInsensitiveDictionary(source);
    }

    @Test
    public void testEqualDictionariesHaveEqualHashCodes() throws Exception {
        CaseInsensitiveDictionary dict1 = headers("Bundle-Name", "Bundle-Version");
        CaseInsensitiveDictionary dict2 = headers("bundle-name", "BUNDLE-VERSION");

        boolean equal = dict1.equals(dict2) && dict2.equals(dict1);
        if (equal) {
            assertEquals("CaseInsensitiveDictionary objects that are equal (" + dict1 + " / " + dict2 + ") must have equal hash codes",
                    dict1.hashCode(), dict2.hashCode());
        } else {
            // a fix that makes the dictionaries unequal must keep equals symmetric
            assertTrue("equals must be symmetric", dict1.equals(dict2) == dict2.equals(dict1));
        }
    }
}
