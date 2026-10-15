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
import static org.junit.Assert.fail;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.junit.Test;
import org.osgi.framework.BundleException;

/**
 * OSGiManifestBuilder.isValidBundleManifest and validateBundleManifest throw a NumberFormatException for a
 * Bundle-ManifestVersion that is not an integer, instead of returning false / throwing a BundleException.
 *
 * <h3>Severity</h3>
 * cosmetic. The WildFly subsystem calls isValidBundleManifest for every deployment and for every jar of an EAR
 * (OSGiManifestStructureProcessor, BundleSubDeploymentMarkingProcessor, also the Arquillian
 * JBOSGiApplicationArchiveProcessor and jbosgi-repository). A deployment whose manifest carries for example an
 * unfiltered {@code Bundle-ManifestVersion: ${bundle.manifest.version}} - or an EAR that contains such a jar - fails
 * in the STRUCTURE phase with an unexplained NumberFormatException (code reading: the processors have no catch;
 * not run on a server), instead of being treated as a non-bundle (or
 * rejected with a manifest error message).
 *
 * <h3>Component</h3>
 * jbosgi-metadata, {@code org.jboss.osgi.metadata.OSGiManifestBuilder.getBundleManifestVersion(Manifest)} used by
 * {@code isValidBundleManifest} / {@code validateBundleManifest}.
 *
 * <h3>Found by</h3>
 * manifest workstream, round 3 (WildFly integration metadata paths), review of the subsystem's bundle recognition
 * plus harness cases {@code v-bmv-abc}, {@code v-bmv-2.0} (the framework itself reports MANIFEST_ERROR for them).
 *
 * <h3>Spec reference</h3>
 * OSGi Core R8, 3.6.1 Bundle-ManifestVersion (https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html):
 * "Bundle-ManifestVersion ::= number"; 3.12: a syntactic error is a manifest error. The contract violated here is
 * the method's own: isValidBundleManifest "@return True if the manifest is valid" catches BundleException only.
 *
 * <h3>Code location</h3>
 * {@code src/main/java/org/jboss/osgi/metadata/OSGiManifestBuilder.java:446-452} ({@code Integer.parseInt}
 * without a catch), {@code :395-405} (isValidBundleManifest only catches BundleException); callers
 * jbosgi/wildfly/subsystem {@code OSGiManifestStructureProcessor.java:70}, {@code BundleSubDeploymentMarkingProcessor.java:68}.
 * Since 95a32b1 (2012).
 *
 * <h3>Root cause</h3>
 * The NumberFormatException of Integer.parseInt is not converted.
 *
 * <h3>Reproduction</h3>
 * {@code cd jbosgi-metadata && JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home mvn test
 * -Dtest=BundleManifestRecognitionIssueTestCase}
 *
 * <h3>Observed</h3>
 * <pre>
 * testIsValidBundleManifest: isValidBundleManifest must return false for Bundle-ManifestVersion: ${bundle.manifest.version},
 *   but it threw java.lang.NumberFormatException: For input string: "${bundle.manifest.version}"
 * testValidateBundleManifest: validateBundleManifest must throw a BundleException for Bundle-ManifestVersion: 2.0,
 *   but it threw java.lang.NumberFormatException: For input string: "2.0"
 * </pre>
 *
 * <h3>Expected</h3>
 * false, and a BundleException (MANIFEST_ERROR).
 *
 * <h3>Reference behaviour</h3>
 * Not applicable (jbosgi API). The jbosgi framework's own path (OSGiMetaDataBuilder.validateMetadata) already wraps
 * the NumberFormatException into a BundleException(MANIFEST_ERROR).
 *
 * <h3>Suggested fix</h3>
 * Catch NumberFormatException in getBundleManifestVersion(Manifest)/validateBundleManifest and throw
 * {@code new BundleException("Invalid Bundle-ManifestVersion: " + value, BundleException.MANIFEST_ERROR, ex)}.
 * Verified: with that change both test methods pass and the jbosgi-metadata tests still pass (JDK 25).
 *
 * <h3>Regression vs WF41</h3>
 * no (WF41 behaves the same).
 */
public class BundleManifestRecognitionIssueTestCase {

    @Test
    public void testIsValidBundleManifest() throws Exception {
        Manifest manifest = getManifest("${bundle.manifest.version}");
        try {
            assertEquals("A Bundle-ManifestVersion that is not a number is not a valid bundle manifest", false, OSGiManifestBuilder.isValidBundleManifest(manifest));
        } catch (RuntimeException ex) {
            fail("isValidBundleManifest must return false for Bundle-ManifestVersion: ${bundle.manifest.version}, but it threw " + ex);
        }
    }

    @Test
    public void testValidateBundleManifest() throws Exception {
        Manifest manifest = getManifest("2.0");
        try {
            OSGiManifestBuilder.validateBundleManifest(manifest);
            fail("validateBundleManifest must throw a BundleException for Bundle-ManifestVersion: 2.0");
        } catch (BundleException ex) {
            // expected
        } catch (RuntimeException ex) {
            fail("validateBundleManifest must throw a BundleException for Bundle-ManifestVersion: 2.0, but it threw " + ex);
        }
    }

    private Manifest getManifest(String manifestVersion) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue("Manifest-Version", "1.0");
        attributes.putValue("Bundle-ManifestVersion", manifestVersion);
        attributes.putValue("Bundle-SymbolicName", "recognition");
        return manifest;
    }
}
