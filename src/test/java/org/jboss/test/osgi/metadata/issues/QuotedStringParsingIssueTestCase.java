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

import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.metadata.ParameterizedAttribute;
import org.junit.Test;

/**
 * The manifest header parser does not implement the quoted-string of the common header syntax: a {@code :=} inside
 * a quoted attribute value turns the attribute into a directive, an escaped reverse solidus {@code \\} is kept as
 * two characters, and an escaped comma in a List attribute keeps its escape character.
 *
 * <h3>Severity</h3>
 * minor. {@code Provide-Capability: q.ns;a="x:=y"} yields a capability with no attribute {@code a} but a
 * directive named {@code a="x} with the value {@code y"}; values with a backslash (for example Windows paths or
 * regular expressions in capability attributes) are not the value the manifest specifies, so filters written
 * against the intended value do not match; a List element containing a comma cannot be expressed.
 *
 * <h3>Component</h3>
 * jbosgi-metadata, {@code org.jboss.osgi.metadata.internal.ManifestParser.parse} and {@code unquote},
 * {@code org.jboss.osgi.metadata.spi.ElementParser.parseDelimitedString},
 * {@code internal.StringListValueCreator}.
 *
 * <h3>Found by</h3>
 * manifest workstream, Equinox/Felix differential harness ({@code explore/manifest}, cases {@code q-pc-03},
 * {@code q-pc-05}..{@code q-pc-08}, {@code q-exp-01}, {@code q-imp-01}, {@code ty-pc-22}..{@code ty-pc-24}).
 *
 * <h3>Spec reference</h3>
 * OSGi Core R8, 1.3.2 General Syntax Definitions
 * (https://docs.osgi.org/specification/osgi.core/8.0.0/framework.introduction.html):
 * "quoted-string ::= '"' ( ~["\#x0D#x0A#x00] | '\"' | '\\' )* '"'"; "argument ::= extended | quoted-string";
 * "directive ::= extended ':=' argument"; "attribute ::= extended '=' argument".<br>
 * 3.3.4 Bundle Capability Attributes (framework.module.html): "Commas or reverse solidi ('\' \) that are part
 * of an element value must be escaped by prefixing them with a reverse solidus. In practice, this requires
 * escaping with two reverse solidi because a reverse solidus must already be escaped in strings."
 *
 * <h3>Code location</h3>
 * {@code src/main/java/org/jboss/osgi/metadata/internal/ManifestParser.java:109}
 * ({@code piece.indexOf(":=")} searches the whole parameter, including the quoted value, before the {@code =});
 * {@code ManifestParser.java:175-183} (unquote only strips the quotes, it does not resolve {@code \\} and
 * {@code \"}); {@code src/main/java/org/jboss/osgi/metadata/internal/StringListValueCreator.java:43} (the list is
 * split at unescaped commas but the escape character is kept). Pre-existing (2012, c2e1893 / 95a32b1).
 *
 * <h3>Root cause</h3>
 * The parameter name/separator is located by a plain indexOf over the parameter text instead of scanning the
 * name up to the first {@code =} or {@code :=}; quoted strings are never unescaped.
 *
 * <h3>Reproduction</h3>
 * {@code cd jbosgi-metadata && JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home mvn test
 * -Dtest=QuotedStringParsingIssueTestCase}
 *
 * <h3>Observed</h3>
 * <pre>
 * testDirectiveSeparatorInsideQuotedValue: Provide-Capability q.ns;a="x:=y" has the attribute a with the value
 *   x:=y expected:&lt;x:=y&gt; but was:&lt;null&gt;
 * testEscapedReverseSolidus: Provide-Capability q.ns;a="x\\y" has the attribute a with the value x\y
 *   expected:&lt;x[]\y&gt; but was:&lt;x[\]\y&gt;
 * testEscapedCommaInListElement: Provide-Capability q.ns;a:List&lt;String&gt;="x\\,y,z" has the elements [x,y, z]
 *   expected:&lt;[x[],y, z]&gt; but was:&lt;[x[\],y, z]&gt;
 * </pre>
 *
 * <h3>Expected</h3>
 * {@code a = "x:=y"} (no directive), {@code a = "x\y"}, {@code a = ["x,y", "z"]}.
 *
 * <h3>Reference behaviour</h3>
 * Equinox 3.23.0 produces {@code x:=y} and {@code x\y} (it splits the list differently, {@code [x\, y, z]});
 * Felix 7.0.5 produces {@code x:=y} and keeps {@code x\\y} like jbosgi.
 *
 * <h3>Suggested fix</h3>
 * In ManifestParser locate the separator from the first {@code '='} of the parameter (a directive when it is
 * preceded by {@code ':'}) instead of {@code indexOf(":=")}; resolve the quoted-string escapes {@code \\} and
 * {@code \"} of a quoted attribute or directive value; for List attributes split the still escaped value at
 * unescaped commas and then resolve, per element, the quoted-string escapes and the list escapes {@code \,} and
 * {@code \\}. Unescaping in {@code unquote} before the list split is not enough: the split would then see bare
 * quote characters. Verified: with exactly this change the three test methods pass and all existing
 * jbosgi-metadata tests (including ManifestProvideCapabilityTestCase with its {@code a\"quote,a\,comma} list)
 * still pass (JDK 25).
 *
 * <h3>Regression vs WF41</h3>
 * no (WF41 behaves the same).
 */
public class QuotedStringParsingIssueTestCase {

    @Test
    public void testDirectiveSeparatorInsideQuotedValue() throws Exception {
        ParameterizedAttribute cap = getCapability("q.ns;a=\"x:=y\"");
        assertEquals("Provide-Capability q.ns;a=\"x:=y\" has the attribute a with the value x:=y", "x:=y", cap.getAttributeValue("a", String.class));
        assertEquals("Provide-Capability q.ns;a=\"x:=y\" has no directives", 0, cap.getDirectives().size());
    }

    @Test
    public void testEscapedReverseSolidus() throws Exception {
        ParameterizedAttribute cap = getCapability("q.ns;a=\"x\\\\y\"");
        assertEquals("Provide-Capability q.ns;a=\"x\\\\y\" has the attribute a with the value x\\y", "x\\y", cap.getAttributeValue("a", String.class));
    }

    @Test
    public void testEscapedCommaInListElement() throws Exception {
        ParameterizedAttribute cap = getCapability("q.ns;a:List<String>=\"x\\\\,y,z\"");
        Object value = cap.getAttribute("a").getValue();
        assertEquals("Provide-Capability q.ns;a:List<String>=\"x\\\\,y,z\" has the elements [x,y, z]", Arrays.asList("x,y", "z"), value);
    }

    private ParameterizedAttribute getCapability(String header) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Bundle-ManifestVersion", "2");
        attributes.putValue("Bundle-SymbolicName", "quoted.string");
        attributes.putValue("Provide-Capability", header);
        OSGiMetaData metadata = OSGiMetaDataBuilder.load(manifest);
        List<ParameterizedAttribute> caps = metadata.getProvidedCapabilities();
        assertEquals("One capability", 1, caps.size());
        return caps.get(0);
    }
}
